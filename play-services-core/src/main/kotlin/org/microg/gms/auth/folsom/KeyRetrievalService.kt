/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.folsom

import android.content.Context
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.folsom.ProductKey
import com.google.android.gms.auth.folsom.RecoveryRequest
import com.google.android.gms.auth.folsom.RecoveryResult
import com.google.android.gms.auth.folsom.SecurityDomainMember
import com.google.android.gms.auth.folsom.SharedKey
import com.google.android.gms.auth.folsom.internal.IBooleanCallback
import com.google.android.gms.auth.folsom.internal.IByteArrayCallback
import com.google.android.gms.auth.folsom.internal.IByteArrayListCallback
import com.google.android.gms.auth.folsom.internal.IKeyRetrievalCallback
import com.google.android.gms.auth.folsom.internal.IKeyRetrievalConsentCallback
import com.google.android.gms.auth.folsom.internal.IKeyRetrievalService
import com.google.android.gms.auth.folsom.internal.IKeyRetrievalSyncStatusCallback
import com.google.android.gms.auth.folsom.internal.IProductKeyCallback
import com.google.android.gms.auth.folsom.internal.IRecoveryResultCallback
import com.google.android.gms.auth.folsom.internal.ISecurityDomainMembersCallback
import com.google.android.gms.auth.folsom.internal.ISharedKeyCallback
import com.google.android.gms.auth.folsom.internal.IStringListCallback
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.auth.folsom.utils.LocalKeyManager
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues
import java.util.UUID

private const val TAG = "KeyRetrievalService"

private const val KEY_SECURITY_DOMAIN = "SECURITY_DOMAIN"
private const val KEY_SESSION_ID = "SESSION_ID"
private const val KEY_OFFER_RESET = "OFFER_RESET"

private val FEATURES = arrayOf(
    Feature("key_retrieval", 2L),
    Feature("list_recovered_security_domains", 1L),
    Feature("start_recovery", 3L),
    Feature("recoverability_fix", 2L),
    Feature("lskf_consent", 1L),
    Feature("reset_security_domain", 2L),
    Feature("generate_open_vault_request", 1L),
    Feature("silently_add_gaia_password_member", 1L),
    Feature("get_domain_state", 1),
    Feature("get_product_keys", 1),
    Feature("create_prf_member", 1),
    Feature("add_recovery_contact_to_dependent_keychain", 1),
    Feature("create_retrieval_packet", 1),
    Feature("set_claimant_key", 1),
)

class KeyRetrievalService : BaseService(TAG, GmsService.FOLSOM) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        val bundle = request.extras
        Log.d(TAG, "handleServiceRequest: packageName=${packageName}, extras=$bundle")
        val securityDomain = bundle?.getString(KEY_SECURITY_DOMAIN)
        if (securityDomain.isNullOrEmpty()) {
            Log.w(TAG, "Security domain is not set")
            callback.onPostInitComplete(ERROR_CODE_SECURITY_DOMAIN_NOT_SET, null, null)
            return
        }
        val sessionId = bundle.getString(KEY_SESSION_ID, UUID.randomUUID().toString())
        val offerReset = bundle.getBoolean(KEY_OFFER_RESET, false)
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            KeyRetrievalServiceImpl(this, lifecycle, securityDomain, sessionId, offerReset),
            ConnectionInfo().apply { features = FEATURES }
        )
    }
}

class KeyRetrievalServiceImpl(
    private val context: Context,
    override val lifecycle: Lifecycle,
    private val domainId: String,
    private val sessionId: String,
    private val offerReset: Boolean
) : IKeyRetrievalService.Stub(), LifecycleOwner {

    override fun setConsent(callback: IKeyRetrievalConsentCallback?, accountName: String?, force: Boolean, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented setConsent accountName:$accountName force:$force")
        callback?.onResult(Status.SUCCESS, true)
    }

    override fun getConsent(callback: IKeyRetrievalConsentCallback?, accountName: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented getConsent accountName:$accountName")
        callback?.onResult(Status.SUCCESS, false)
    }

    override fun getSyncStatus(callback: IKeyRetrievalSyncStatusCallback?, accountName: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented getSyncStatus accountName:$accountName")
        callback?.onResult(Status.SUCCESS, true)
    }

    override fun markLocalKeysAsStale(callback: IKeyRetrievalCallback?, accountName: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "markLocalKeysAsStale accountName:$accountName")
        if (accountName.isNullOrEmpty()) {
            callback?.onResult(Status.INTERNAL_ERROR)
            return
        }
        lifecycleScope.launchWhenStarted {
            try {
                val localKeyManager = LocalKeyManager.getInstance(context)
                val currentStatus = localKeyManager.getDomainStatus(accountName, domainId)
                Log.d(TAG, "markLocalKeysAsStale: currentStatus=$currentStatus")

                if (currentStatus != DomainStatus.UNKNOWN) {
                    localKeyManager.setDomainStatus(accountName, domainId, DomainStatus.RECOVERABLE)
                    localKeyManager.updateLastFetchTimestamp(accountName, domainId, 0L)
                    Log.d(TAG, "markLocalKeysAsStale: marked as stale (timestamp reset)")
                }
                callback?.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "markLocalKeysAsStale failed", e)
                callback?.onResult(Status(CommonStatusCodes.INTERNAL_ERROR))
            }
        }
    }

    override fun getKeyMaterial(callback: ISharedKeyCallback?, accountName: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "getKeyMaterial accountName:$accountName securityDomain:$domainId")
        if (accountName.isNullOrEmpty()) {
            Log.w(TAG, "getKeyMaterial: accountName is null or empty")
            callback?.onResult(Status(CommonStatusCodes.DEVELOPER_ERROR), emptyArray<SharedKey>())
            return
        }
        fun errorResult(domainStatus: DomainStatus) {
            val errorStatus = context.buildKeyRetrievalStatus(accountName, domainId, 1, sessionId, offerReset) {
                val statusMessage = when (domainStatus) {
                    DomainStatus.NO_KEYS -> "Empty domain"
                    DomainStatus.NOT_RECOVERABLE -> "Domain is not retrievable"
                    else -> "No shared keys available"
                }
                Status(CommonStatusCodes.SIGN_IN_REQUIRED, statusMessage, it)
            }
            callback?.onResult(errorStatus, emptyArray<SharedKey>())
        }
        lifecycleScope.launchWhenStarted {
            try {
                val localKeyManager = LocalKeyManager.getInstance(context)
                val domainStatus = localKeyManager.getDomainStatus(accountName, domainId)
                Log.d(TAG, "getKeyMaterial: domainStatus=$domainStatus")

                val localKeys = localKeyManager.getLocalKeysOrSync(context, accountName, domainId, sessionId)
                if (localKeys.isEmpty()) {
                    Log.w(TAG, "getKeyMaterial: no keys available")
                    return@launchWhenStarted errorResult(domainStatus)
                }

                val validKeys = localKeys.filter { it.keyVersion != 0 }
                if (validKeys.isEmpty() && localKeys.any { it.keyVersion == 0 }) {
                    Log.w(TAG, "getKeyMaterial: only invalid keys (version=0) available")
                    return@launchWhenStarted errorResult(domainStatus)
                }

                val sharedKeys = localKeys.mapNotNull { key ->
                    val keyMaterial = key.keyMaterial?.toByteArray()
                    if (keyMaterial != null && keyMaterial.isNotEmpty()) {
                        SharedKey(key.keyVersion ?: 0, keyMaterial)
                    } else {
                        null
                    }
                }.toTypedArray()

                if (sharedKeys.isEmpty()) {
                    Log.w(TAG, "getKeyMaterial: no valid key material")
                    return@launchWhenStarted errorResult(domainStatus)
                }

                Log.d(TAG, "getKeyMaterial: returning ${sharedKeys.size} keys")
                callback?.onResult(Status.SUCCESS, sharedKeys)
            } catch (e: Exception) {
                Log.e(TAG, "getKeyMaterial failed", e)
                callback?.onResult(Status(ERROR_CODE_NO_KEYS, "No shared keys available"), emptyArray<SharedKey>())
            }
        }
    }

    override fun setKeyMaterial(callback: IKeyRetrievalCallback?, accountName: String?, keys: Array<out SharedKey?>?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented setKeyMaterial accountName:$accountName")
        callback?.onResult(Status.INTERNAL_ERROR)
    }

    override fun getRecoveredSecurityDomains(callback: IStringListCallback?, accountName: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented getRecoveredSecurityDomains accountName:$accountName")
        callback?.onResult(Status.SUCCESS, emptyArray<String>())
    }

    override fun startRecoveryOperation(callback: IRecoveryResultCallback?, metadata: ApiMetadata?, request: RecoveryRequest?) {
        Log.d(TAG, "Not implemented startRecoveryOperation request:$request")
        callback?.onResult(Status.SUCCESS, RecoveryResult())
    }

    override fun listVaultsOperation(callback: IByteArrayListCallback?, accountName: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented listVaultsOperation accountName:$accountName")
        callback?.onResult(Status.SUCCESS, emptyList<ByteArray>())
    }

    override fun generateOpenVaultRequestOperation(callback: IByteArrayCallback?, request: RecoveryRequest?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented generateOpenVaultRequestOperation request:$request")
        callback?.onResult(Status.SUCCESS, byteArrayOf(), ApiMetadata.DEFAULT)
    }

    override fun joinSecurityDomain(callback: IStatusCallback?, accountName: String?, bytes: ByteArray?, type: Int, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented joinSecurityDomain accountName:$accountName type:$type")
        callback?.onResult(Status.SUCCESS)
    }

    override fun resetSecurityDomain(callback: IStatusCallback?, accountName: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented resetSecurityDomain accountName:$accountName")
        callback?.onResult(Status.SUCCESS)
    }

    override fun listSecurityDomainMembers(callback: ISecurityDomainMembersCallback?, accountName: String?, metadata: ApiMetadata?) {
        if (accountName.isNullOrEmpty()) {
            Log.w(TAG, "listSecurityDomainMembers: accountName is null or empty")
            callback?.onResult(Status.INTERNAL_ERROR, emptyArray<SecurityDomainMember>(), metadata ?: ApiMetadata.DEFAULT)
            return
        }
        Log.d(TAG, "listSecurityDomainMembers accountName:$accountName securityDomain:$domainId")
        lifecycleScope.launchWhenStarted {
            try {
                val response = runCatching {
                    loadSecurityDomainMembers(context, accountName, sessionId, domainId)
                }.onFailure {
                    Log.w(TAG, "listSecurityDomainMembers: load error!", it)
                }.getOrNull()

                Log.d(TAG, "listSecurityDomainMembers: response members count=${response?.members?.size ?: 0}")
                if (response == null) {
                    Log.w(TAG, "listSecurityDomainMembers: response is null, domain may not exist")
                    try {
                        LocalKeyManager.getInstance(context).clearDomainKeys(accountName, domainId)
                        Log.d(TAG, "listSecurityDomainMembers: cleared local cache for domain=$domainId")
                    } catch (e: Exception) {
                        Log.w(TAG, "listSecurityDomainMembers: failed to clear cache", e)
                    }
                    callback?.onResult(Status.SUCCESS, emptyArray<SecurityDomainMember>(), ApiMetadata.DEFAULT)
                    return@launchWhenStarted
                }

                val memberList = response.members.map { member ->
                    SecurityDomainMember(
                        member.memberType ?: 0, member.memberMetadata?.encode() ?: byteArrayOf()
                    )
                }.toTypedArray()

                Log.d(TAG, "listSecurityDomainMembers: returning ${memberList.size} members")
                callback?.onResult(Status.SUCCESS, memberList, ApiMetadata.DEFAULT)
            } catch (e: Exception) {
                Log.e(TAG, "listSecurityDomainMembers failed", e)
                callback?.onResult(Status.INTERNAL_ERROR, emptyArray<SecurityDomainMember>(), ApiMetadata.DEFAULT)
            }
        }
    }

    override fun getDomainState(callback: IByteArrayCallback?, accountName: String?, metadata: ApiMetadata?) {
        if (accountName.isNullOrEmpty()) {
            Log.w(TAG, "getDomainState: accountName is null or empty")
            callback?.onResult(Status.INTERNAL_ERROR, byteArrayOf(), ApiMetadata.DEFAULT)
            return
        }
        Log.d(TAG, "getDomainState accountName:$accountName securityDomain:$domainId")
        lifecycleScope.launchWhenStarted {
            try {
                val localKeyManager = LocalKeyManager.getInstance(context)
                try {
                    localKeyManager.updateLastFetchTimestamp(accountName, domainId, 0L)
                    Log.d(TAG, "getDomainState: reset fetch timestamp")
                } catch (e: Exception) {
                    Log.w(TAG, "getDomainState: failed to reset timestamp, continuing", e)
                }
                var serverState: GetSecurityDomainResponse? = null
                var membersResponse: ListSecurityDomainMembersResponse? = null
                try {
                    serverState = getSecurityDomain(context, accountName, sessionId, domainId)
                    membersResponse = loadSecurityDomainMembers(context, accountName, sessionId, domainId)
                    Log.d(TAG, "getDomainState: serverState=$serverState, members=${membersResponse.members.size}")
                } catch (e: Exception) {
                    Log.e(TAG, "getDomainState: failed to sync domain state", e)
                }
                val computedState = localKeyManager.computeDomainStatus(accountName, domainId, serverState, membersResponse).also {
                    localKeyManager.setDomainStatus(accountName, domainId, it)
                    localKeyManager.updateLastFetchTimestamp(accountName, domainId, System.currentTimeMillis())
                }
                Log.d(TAG, "getDomainState: computedState=$computedState")
                val responseBytes = serializeDomainStateResponse(computedState.code)
                Log.d(TAG, "getDomainState: returning state=$computedState")
                callback?.onResult(Status.SUCCESS, responseBytes, ApiMetadata.DEFAULT)
            } catch (e: Exception) {
                Log.e(TAG, "getDomainState failed", e)
                val errorResponse = serializeDomainStateResponse(DomainStatus.UNKNOWN_ERROR.code)
                callback?.onResult(Status.INTERNAL_ERROR, errorResponse, ApiMetadata.DEFAULT)
            }
        }
    }

    override fun startUxFlow(callback: IKeyRetrievalCallback?, accountName: String?, type: Int, metadata: ApiMetadata?) {
        Log.d(TAG, "startUxFlow accountName:$accountName type:$type")
        if (accountName.isNullOrEmpty()) {
            callback?.onResult(Status(CommonStatusCodes.DEVELOPER_ERROR))
            return
        }
        val status = context.buildKeyRetrievalStatus(accountName, domainId, type, sessionId, offerReset) {
            Status(CommonStatusCodes.SUCCESS, "UX flow PendingIntent retrieved.", it)
        }
        callback?.onResult(status)
    }

    override fun promptForLskfConsent(callback: IKeyRetrievalCallback?, accountName: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented promptForLskfConsent accountName:$accountName")
        callback?.onResult(Status.SUCCESS)
    }

    override fun canSilentlyAddGaiaPassword(callback: IBooleanCallback?, accountName: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented canSilentlyAddGaiaPassword accountName:$accountName")
        callback?.onResult(Status.SUCCESS, true)
    }

    override fun addGaiaPasswordMember(callback: IStatusCallback?, accountName: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented addGaiaPasswordMember accountName:$accountName")
        callback?.onResult(Status.SUCCESS)
    }

    override fun getProductDetails(callback: IByteArrayCallback?, accountName: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not implemented getProductDetails accountName:$accountName")
        callback?.onResult(Status.SUCCESS, byteArrayOf(), ApiMetadata.DEFAULT)
    }

    override fun getProductKeysOperation(callback: IProductKeyCallback?, accountName: String?, accountName2: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not yet implemented: GetProductKeysOperation accountName:$accountName")
        callback?.onResult(Status.SUCCESS, emptyArray<ProductKey>(), metadata)
    }

    override fun createPrfMemberOperation(callback: IStatusCallback?, accountName: String?, bytes: ByteArray?, bytes2: ByteArray?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not yet implemented: CreatePrfMemberOperation accountName:$accountName")
        callback?.onResult(Status.SUCCESS)
    }

    override fun addRecoveryContactToDependentKeychainOperation(callback: IStatusCallback?, accountName: String?, accountName2: String?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not yet implemented: AddRecoveryContactToDependentKeychainOperation accountName:$accountName")
        callback?.onResult(Status.SUCCESS)
    }

    override fun createRetrievalPacketOperation(callback: IStatusCallback?, accountName: String?, accountName2: String?, bytes: ByteArray?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not yet implemented: CreateRetrievalPacketOperation accountName:$accountName")
        callback?.onResult(Status.SUCCESS)
    }

    override fun setClaimantKeyOperation(callback: IStatusCallback?, accountName: String?, bytes: ByteArray?, bytes2: ByteArray?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not yet implemented: SetClaimantKeyOperation accountName:$accountName")
        callback?.onResult(Status.SUCCESS)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        return warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
    }
}
