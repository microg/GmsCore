/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.gcm.registeration

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.LocaleList
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import google.internal.notifications.v1.AppBlockState
import google.internal.notifications.v1.AppRegistration
import google.internal.notifications.v1.AppRegistrationContainer
import google.internal.notifications.v1.AuthWrapper
import google.internal.notifications.v1.Channel
import google.internal.notifications.v1.DeviceRequest
import google.internal.notifications.v1.DeviceType
import google.internal.notifications.v1.FeatureBitmapList
import google.internal.notifications.v1.GmsDeviceContext
import google.internal.notifications.v1.GmsDeviceProfile
import google.internal.notifications.v1.NotificationsApiServiceClient
import google.internal.notifications.v1.RegistrationPayload
import google.internal.notifications.v1.RegistrationReason
import google.internal.notifications.v1.RegistrationRequest
import google.internal.notifications.v1.RegistrationStatus
import google.internal.notifications.v1.SdkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthConstants
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import org.microg.gms.common.Utils
import org.microg.gms.gcm.GMS_NOTS_BASE_URL
import org.microg.gms.gcm.GMS_NOTS_OAUTH_SERVICE
import org.microg.gms.gcm.createGrpcClient
import org.microg.gms.profile.Build
import org.microg.gms.profile.Build.VERSION.SDK_INT
import org.microg.gms.utils.toBase64
import java.util.Locale
import java.util.TimeZone

private const val TAG = "ChimeGmsRegistration"
private const val DEFAULT_TTL = 15_552_000
private const val DEFAULT_CHANNEL_NUMBER = "-1"

class ChimeGmsRegistrationHelper(val context: Context) {
    private val chimeAccountsStore = context.getSharedPreferences("chime_gms_accounts", Context.MODE_PRIVATE)

    suspend fun handleRegistration(regId: String, reason: RegistrationReason = RegistrationReason.ACCOUNT_CHANGED): List<Account> {
        Log.d(TAG, "handle Account Registration regId:$regId")
        clearDeletedAccountRegistration()
        val accounts = AccountManager.get(context).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).filter {
            val accountRegistration = getAccountRegistration(it.name)
            accountRegistration == null || accountRegistration.registrationStatus != RegistrationStatus.STATUS_REGISTERED
        }
        Log.d(TAG, "handleRegistration: needRegistration: ${accounts.joinToString("|") { it.name }}")
        if (accounts.isEmpty()) return emptyList()
        val authTokens = withContext(Dispatchers.IO) {
            accounts.mapNotNull {
                val authToken = runCatching { AccountManager.get(context).blockingGetAuthToken(it, GMS_NOTS_OAUTH_SERVICE, true) }.getOrNull()
                if (authToken != null) Pair(it.name, authToken) else null
            }
        }
        Log.d(TAG, "authTokens: $authTokens")
        if (authTokens.isEmpty()) return emptyList()
        val response = withContext(Dispatchers.IO) {
            val request = buildRegistrationRequest(authTokens, reason, regId)
            Log.d(TAG, "Registration request: ${request.encode().toBase64()}")
            val api = createGrpcClient<NotificationsApiServiceClient>(
                baseUrl = GMS_NOTS_BASE_URL,
                oauthToken = authTokens.first().second
            )
            runCatching { api.MultiLoginUpdate().executeBlocking(request) }.onFailure {
                Log.d(TAG, "handleRegistration: failed!", it)
            }.getOrNull()
        }
        Log.d(TAG, "Registration response: $response")
        if (response == null) return emptyList()
        accounts.map {
            val accountId = it.name.hashCode().toString()
            val result = response.registrationResults.find { data -> accountId == data.id }
            ChimeGmsAccount(
                id = accountId,
                accountName = it.name,
                representativeTargetId = result?.payload?.representativeTargetId,
                registrationStatus = if (result?.success == true) RegistrationStatus.STATUS_REGISTERED else RegistrationStatus.STATUS_UNREGISTERED,
                obfuscatedGaiaId = result?.obfuscatedGaiaId
            )
        }.forEach { saveAccountRegistration(it) }
        Log.d(TAG, "Registration success : ${accounts.joinToString("|") { it.name }}")
        return accounts
    }

    private fun buildRegistrationRequest(authTokens: List<Pair<String, String?>>, reason: RegistrationReason, gmsRegId: String): RegistrationRequest {
        val devices = authTokens.map {
            DeviceRequest.build {
                id = it.first.hashCode().toString()
                auth = AuthWrapper.build {
                    authTokenWrapper = AuthWrapper.AuthToken.build { authToken = it.second }
                }
                gmsDeviceContext = buildDeviceContext()
                ttl = DEFAULT_TTL
            }
        }
        return RegistrationRequest.Builder()
            .chimeGmsClientId("fcm")
            .reason(reason)
            .registrationPayload(buildRegistrationPayload(gmsRegId))
            .devices(devices)
            .build()
    }

    private fun buildRegistrationPayload(gmsRegId: String) =
        RegistrationPayload.build {
            channel = Channel.GCM_DEVICE_PUSH
            appRegistrationContainer = AppRegistrationContainer.build {
                appRegistration = AppRegistration.build {
                    packageName = Constants.GMS_PACKAGE_NAME
                    androidId = LastCheckinInfo.read(context).androidId
                    regId = gmsRegId
                }
            }
        }

    private fun buildDeviceContext() = GmsDeviceContext.build {
        languageTag = if (SDK_INT >= 24) LocaleList.getDefault().get(0).toLanguageTag() else Locale.getDefault().language
        gmsDeviceProfile = GmsDeviceProfile.build {
            val packageInfo = context.packageManager.getPackageInfo(Constants.GMS_PACKAGE_NAME, 0)
            density = context.resources.displayMetrics.density
            versionName = packageInfo.versionName
            release = Build.VERSION.RELEASE
            id = Build.ID
            model = Build.MODEL
            sdkVersion = SDK_INT
            manufacturer = Build.MANUFACTURER
            sdkType = SdkType.RAW_FCM_GMSCORE
            channelNumber = DEFAULT_CHANNEL_NUMBER
            deviceModel = "${Build.BRAND} ${Build.MODEL}"
            appBlockState = if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                AppBlockState.ALLOWED
            } else {
                AppBlockState.BANNED
            }
            deviceCountry = Utils.getLocale(context).country.lowercase()
            featureBitmapList = FeatureBitmapList(listOf(0x40))
            deviceType = DeviceType.DEVICE_TYPE_DEFAULT
        }
        timeZoneId = TimeZone.getDefault().id
    }

    private fun saveAccountRegistration(chimeGmsAccount: ChimeGmsAccount) {
        chimeAccountsStore.edit {
            putString(chimeGmsAccount.accountName, chimeGmsAccount.toJson())
        }
    }

    private fun getAccountRegistration(accountName: String): ChimeGmsAccount? {
        val json = chimeAccountsStore.getString(accountName, null) ?: return null
        return ChimeGmsAccount.parseJson(json)
    }

    private fun clearDeletedAccountRegistration() {
        val accounts = AccountManager.get(context).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
        if (accounts.isEmpty() || chimeAccountsStore.all.isNullOrEmpty()) return
        chimeAccountsStore.all?.forEach {
            if (accounts.all { account -> account.name != it.key }) {
                chimeAccountsStore.edit { remove(it.key) }
            }
        }
    }

    fun resetAllData() {
        chimeAccountsStore.edit { clear() }
    }
}