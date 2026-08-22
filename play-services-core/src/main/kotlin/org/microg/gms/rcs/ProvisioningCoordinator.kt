/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.microg.gms.carrierauth.IccAuthenticator

enum class ProvisioningState {
    IDLE,
    READING_SUBSCRIPTION,
    READING_PHONE_NUMBER,
    CHECKING_CARRIER,
    ICC_AUTHENTICATION_UNAVAILABLE,
    SMS_FALLBACK,
    NETWORK_PROVISIONING,
    RETRYABLE_FAILURE,
    COMPLETE,
    PERMANENT_FAILURE
}

sealed interface ProvisioningResult {
    val state: ProvisioningState

    data class Success(
        val subscription: SubscriptionSnapshot,
        val phoneNumber: String,
        val carrierConfig: PersistableBundle,
        val iccAuthentication: IccAuthentication
    ) : ProvisioningResult {
        override val state = ProvisioningState.COMPLETE
    }

    data class Retry(
        override val state: ProvisioningState,
        val reason: Reason
    ) : ProvisioningResult

    data class Failure(
        val reason: Reason
    ) : ProvisioningResult {
        override val state = ProvisioningState.PERMANENT_FAILURE
    }
}

enum class Reason {
    MISSING_READ_PHONE_STATE,
    MISSING_READ_PHONE_NUMBERS,
    NO_ACTIVE_SUBSCRIPTION,
    SUBSCRIPTION_UNAVAILABLE,
    PHONE_NUMBER_UNAVAILABLE,
    CARRIER_CONFIG_UNAVAILABLE,
    CARRIER_NOT_SUPPORTED,
    ICC_AUTHENTICATION_UNAVAILABLE,
    INVALID_REQUEST,
    CANCELLED,
    INTERNAL_ERROR
}

data class ProvisioningRequest(
    val subscriptionId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
    val appType: Int? = null,
    val authType: Int? = null,
    val challenge: String? = null,
    val allowSmsFallback: Boolean = true
)

data class SubscriptionSnapshot(
    val subscriptionId: Int,
    val countryIso: String?,
    val carrierName: String?,
    val mccMnc: String?,
    val hasCarrierPrivileges: Boolean
)

sealed interface IccAuthentication {
    data class Success(val response: String) : IccAuthentication
    data object NotRequested : IccAuthentication
    data object Restricted : IccAuthentication
    data object Unsupported : IccAuthentication
    data object InvalidRequest : IccAuthentication
}

class ProvisioningCoordinator(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    constructor(context: android.content.Context) : this(context, kotlinx.coroutines.Dispatchers.IO)

    private val context = context.applicationContext
    private val operationMutex = Mutex()
    private val telephonyManager =
        context.getSystemService(TelephonyManager::class.java)
    private val subscriptionManager =
        context.getSystemService(SubscriptionManager::class.java)
    private val carrierConfigManager =
        context.getSystemService(CarrierConfigManager::class.java)
    private val iccAuthenticator = IccAuthenticator(context)

    @Volatile
    var state: ProvisioningState = ProvisioningState.IDLE
        private set

    fun getStatusBundle(): Bundle {
        val result = Bundle()
        result.putBoolean("ready", false)
        result.putBoolean("ims_registered", false)
        result.putInt("active_subscription_id", SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        result.putString("state", "PENDING_CARRIER_CONFIG")

        try {
            if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
                return result
            }

            val activeSubscriptions = subscriptionManager?.activeSubscriptionInfoList.orEmpty()
            if (activeSubscriptions.isEmpty()) {
                return result
            }

            val activeSubscriptionId = getBestRcsSubscriptionId(activeSubscriptions)
            val telephony = telephonyManager?.createForSubscriptionId(activeSubscriptionId)
                ?: return result
            val imsRegistered = readImsRegistered(telephony)
            val carrierConfig = carrierConfigManager?.getConfigForSubId(activeSubscriptionId)
            result.putInt("active_subscription_id", activeSubscriptionId)
            result.putIntArray(
                "active_subscription_ids",
                activeSubscriptions.map(SubscriptionInfo::getSubscriptionId).toIntArray()
            )
            result.putBoolean("ims_registered", imsRegistered)
            result.putString(
                "state",
                if (carrierConfig != null && imsRegistered) {
                    "REGISTERED"
                } else {
                    "PENDING_CARRIER_CONFIG"
                }
            )
            result.putBoolean("ready", carrierConfig != null && imsRegistered)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
        return result
    }

    suspend fun provision(request: ProvisioningRequest): ProvisioningResult =
        withContext(dispatcher) {
            operationMutex.withLock {
                runProvisioning(request)
            }
        }

    private suspend fun runProvisioning(
        request: ProvisioningRequest
    ): ProvisioningResult {
        if (!request.isValid()) {
            return failure(Reason.INVALID_REQUEST)
        }

        return try {
            state = ProvisioningState.READING_SUBSCRIPTION
            val subscription = findSubscription(request.subscriptionId)
                ?: return retry(
                    ProvisioningState.RETRYABLE_FAILURE,
                    if (hasPermission(Manifest.permission.READ_PHONE_STATE)) {
                        Reason.NO_ACTIVE_SUBSCRIPTION
                    } else {
                        Reason.MISSING_READ_PHONE_STATE
                    }
                )

            state = ProvisioningState.READING_PHONE_NUMBER
            val phoneNumber = readPhoneNumber(subscription.subscriptionId)
                ?: return retry(
                    ProvisioningState.RETRYABLE_FAILURE,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        !hasPermission(Manifest.permission.READ_PHONE_NUMBERS)
                    ) {
                        Reason.MISSING_READ_PHONE_NUMBERS
                    } else {
                        Reason.PHONE_NUMBER_UNAVAILABLE
                    }
                )

            state = ProvisioningState.CHECKING_CARRIER
            val carrierConfig = readCarrierConfig(subscription.subscriptionId)
                ?: return retry(
                    ProvisioningState.RETRYABLE_FAILURE,
                    Reason.CARRIER_CONFIG_UNAVAILABLE
                )

            val iccAuthentication = authenticateIcc(request, subscription)
            if (iccAuthentication is IccAuthentication.Restricted ||
                iccAuthentication is IccAuthentication.Unsupported
            ) {
                state = ProvisioningState.ICC_AUTHENTICATION_UNAVAILABLE
                if (!request.allowSmsFallback) {
                    return retry(
                        ProvisioningState.RETRYABLE_FAILURE,
                        Reason.ICC_AUTHENTICATION_UNAVAILABLE
                    )
                }
                return retry(
                    ProvisioningState.SMS_FALLBACK,
                    Reason.ICC_AUTHENTICATION_UNAVAILABLE
                )
            }

            if (!carrierSupportsProvisioning(carrierConfig, iccAuthentication)) {
                return failure(Reason.CARRIER_NOT_SUPPORTED)
            }

            state = ProvisioningState.NETWORK_PROVISIONING
            state = ProvisioningState.COMPLETE
            ProvisioningResult.Success(
                subscription = subscription,
                phoneNumber = phoneNumber,
                carrierConfig = carrierConfig,
                iccAuthentication = iccAuthentication
            )
        } catch (_: CancellationException) {
            state = ProvisioningState.IDLE
            throw CancellationException("RCS provisioning cancelled")
        } catch (_: SecurityException) {
            retry(ProvisioningState.RETRYABLE_FAILURE, Reason.INTERNAL_ERROR)
        } catch (_: RuntimeException) {
            retry(ProvisioningState.RETRYABLE_FAILURE, Reason.INTERNAL_ERROR)
        }
    }

    private fun findSubscription(requestedId: Int): SubscriptionSnapshot? {
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            return null
        }

        val activeSubscriptions = try {
            subscriptionManager?.activeSubscriptionInfoList.orEmpty()
        } catch (_: SecurityException) {
            return null
        } catch (_: RuntimeException) {
            return null
        }

        if (activeSubscriptions.isEmpty()) {
            return null
        }

        val resolvedId = resolveSubscriptionId(requestedId, activeSubscriptions)
        val info = activeSubscriptions.firstOrNull { info ->
            resolvedId == SubscriptionManager.INVALID_SUBSCRIPTION_ID ||
                info.subscriptionId == resolvedId
        } ?: activeSubscriptions.firstOrNull()
        return info?.toSnapshot()
    }

    private fun resolveSubscriptionId(
        requestedId: Int,
        activeSubscriptions: List<SubscriptionInfo>
    ): Int {
        if (requestedId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return requestedId
        }

        return getBestRcsSubscriptionId(activeSubscriptions)
    }

    private fun getBestRcsSubscriptionId(activeSubscriptions: List<SubscriptionInfo>): Int {
        if (activeSubscriptions.isEmpty()) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }

        val defaultDataId = try {
            if (subscriptionManager != null) {
                SubscriptionManager.getDefaultDataSubscriptionId()
            } else {
                SubscriptionManager.INVALID_SUBSCRIPTION_ID
            }
        } catch (_: SecurityException) {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        } catch (_: RuntimeException) {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }

        val rcsCapableSubs = activeSubscriptions.filter { info ->
            val config = try {
                carrierConfigManager?.getConfigForSubId(info.subscriptionId)
            } catch (_: SecurityException) {
                null
            } catch (_: RuntimeException) {
                null
            }
            config?.getBoolean(KEY_RCS_PROVISIONING_SUPPORTED, true) ?: true
        }

        return when {
            rcsCapableSubs.isEmpty() -> {
                activeSubscriptions.firstOrNull { it.subscriptionId == defaultDataId }?.subscriptionId
                    ?: activeSubscriptions.first().subscriptionId
            }
            rcsCapableSubs.any { it.subscriptionId == defaultDataId } -> {
                defaultDataId
            }
            else -> {
                rcsCapableSubs.first().subscriptionId
            }
        }
    }

    private fun readPhoneNumber(subscriptionId: Int): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !hasPermission(Manifest.permission.READ_PHONE_NUMBERS)
        ) {
            return null
        }

        val telephony = telephonyManager ?: return null
        return try {
            val number = telephony
                .createForSubscriptionId(subscriptionId)
                .line1Number
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: return null

            PhoneNumberUtils.normalizeNumber(number)
                .takeIf(String::isNotEmpty)
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun SubscriptionInfo.toSnapshot(): SubscriptionSnapshot {
        val hasPrivileges = try {
            telephonyManager
                ?.createForSubscriptionId(subscriptionId)
                ?.hasCarrierPrivileges() == true
        } catch (_: SecurityException) {
            false
        } catch (_: RuntimeException) {
            false
        }
        val mccMnc = listOfNotNull(mccString, mncString)
            .takeIf { it.size == 2 }
            ?.joinToString("")
        return SubscriptionSnapshot(
            subscriptionId = subscriptionId,
            countryIso = countryIso,
            carrierName = carrierName?.toString(),
            mccMnc = mccMnc,
            hasCarrierPrivileges = hasPrivileges
        )
    }

    private fun readImsRegistered(telephony: TelephonyManager): Boolean {
        return try {
            val method = telephony.javaClass.getMethod("isImsRegistered")
            method.invoke(telephony) as? Boolean ?: false
        } catch (_: ReflectiveOperationException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun readCarrierConfig(subscriptionId: Int): PersistableBundle? {
        return try {
            carrierConfigManager?.getConfigForSubId(subscriptionId)
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun authenticateIcc(
        request: ProvisioningRequest,
        subscription: SubscriptionSnapshot
    ): IccAuthentication {
        val appType = request.appType ?: return IccAuthentication.NotRequested
        val authType = request.authType ?: return IccAuthentication.NotRequested
        val challenge = request.challenge ?: return IccAuthentication.NotRequested

        if (!subscription.hasCarrierPrivileges) {
            return IccAuthentication.Restricted
        }

        return when (
            val response = iccAuthenticator.authenticate(
                subscription.subscriptionId,
                appType,
                authType,
                challenge
            )
        ) {
            null -> IccAuthentication.Restricted
            else -> IccAuthentication.Success(response)
        }
    }

    private fun carrierSupportsProvisioning(
        config: PersistableBundle,
        iccAuthentication: IccAuthentication
    ): Boolean {
        val explicitlyUnsupported = config.getBoolean(
            KEY_RCS_PROVISIONING_SUPPORTED,
            true
        ).not()
        if (explicitlyUnsupported) {
            return false
        }

        val requiresIcc = config.getBoolean(
            KEY_RCS_PROVISIONING_REQUIRES_ICC_AUTH,
            false
        )
        return !requiresIcc || iccAuthentication is IccAuthentication.Success
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ProvisioningRequest.isValid(): Boolean {
        return (appType == null && authType == null && challenge == null) ||
            (appType != null && authType != null && !challenge.isNullOrEmpty())
    }

    private fun retry(
        nextState: ProvisioningState,
        reason: Reason
    ): ProvisioningResult.Retry {
        state = nextState
        return ProvisioningResult.Retry(nextState, reason)
    }

    private fun failure(reason: Reason): ProvisioningResult.Failure {
        state = ProvisioningState.PERMANENT_FAILURE
        return ProvisioningResult.Failure(reason)
    }

    companion object {
        private const val KEY_RCS_PROVISIONING_SUPPORTED =
            "carrier_supports_rcs_provisioning"
        private const val KEY_RCS_PROVISIONING_REQUIRES_ICC_AUTH =
            "carrier_rcs_provisioning_requires_icc_auth"
    }
}
