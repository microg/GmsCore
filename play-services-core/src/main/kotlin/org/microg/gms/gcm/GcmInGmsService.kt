/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.gcm

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.legacy.content.WakefulBroadcastReceiver
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.BuildConfig
import com.google.android.gms.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okio.ByteString
import org.microg.gms.accountsettings.ui.KEY_NOTIFICATION_ID
import org.microg.gms.accountsettings.ui.MainActivity
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthManager
import org.microg.gms.auth.AuthPrefs
import org.microg.gms.auth.AuthResponse
import org.microg.gms.auth.ItAuthData
import org.microg.gms.auth.ItMetadataData
import org.microg.gms.auth.OAuthAuthorization
import org.microg.gms.auth.OAuthTokenData
import org.microg.gms.auth.TokenField
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import org.microg.gms.common.ForegroundServiceContext
import org.microg.gms.gcm.registeration.ChimeGmsRegistrationHelper
import org.microg.gms.profile.Build.VERSION.SDK_INT
import org.microg.gms.profile.ProfileManager
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min

private const val TAG = "GcmInGmsService"

private const val KEY_GCM_ANDROID_ID = "androidId"
private const val KEY_GCM_REG_ID = "regId"
private const val KEY_GCM_REG_SENDER = "sender"
private const val KEY_GCM_REG_TIME = "reg_time"
private const val KEY_GCM_REG_ACCOUNT_LIST = "accountList"

private const val GMS_GCM_REGISTER_SCOPE = "GCM"
private const val GMS_GCM_REGISTER_SENDER = "745476177629"
private const val GMS_GCM_REGISTER_SUBTYPE = "745476177629"
private const val GMS_GCM_REGISTER_SUBSCRIPTION = "745476177629"
private const val GCM_GROUP_SENDER = "google.com"
private const val GCM_GMS_REG_REFRESH_S = 604800L

private const val DEFAULT_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
private const val AUTHS_TOKEN_PREFIX = "ya29.m."
private const val GMS_GCM_OAUTH_SERVICE = "oauth2:https://www.googleapis.com/auth/gcm"

private const val CHANNEL_ID = "gcm_notification"
private const val CHANNEL_NAME = "gnots"
private const val GMS_GCM_NOTIFICATIONS = "notifications"
private const val NOTIFICATION_STATUS_READY = 2
private const val NOTIFICATION_STATUS_COMPLETE = 5
private const val NOTIFICATION_REPEAT_NUM = 3
private const val NOTIFICATION_DELAY_TIME = 500L

class GcmInGmsService : LifecycleService() {
    companion object {
        private val accountNotificationMap = HashMap<String, MutableList<Pair<Int, NotificationData>>>()
        private val notificationIdGenerator = AtomicInteger(0)
    }
    private var sp: SharedPreferences? = null
    private var accountManager: AccountManager? = null
    private val chimeGmsRegistrationHelper by lazy { ChimeGmsRegistrationHelper(this) }

    override fun onCreate() {
        super.onCreate()
        ProfileManager.ensureInitialized(this)
        sp = getSharedPreferences("com.google.android.gcm", MODE_PRIVATE) ?: throw RuntimeException("sp get error")
        accountManager = getSystemService(ACCOUNT_SERVICE) as AccountManager? ?: throw RuntimeException("accountManager is null")
        if (SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            ForegroundServiceContext.completeForegroundService(this, intent, TAG)
            Log.d(TAG, "onStartCommand: $intent")
            lifecycleScope.launchWhenStarted {
                if (checkGcmStatus()) {
                    handleIntent(intent)
                } else {
                    val intent = Intent(ACTION_GCM_RECONNECT).apply {
                        setPackage(Constants.GMS_PACKAGE_NAME)
                    }
                    sendBroadcast(intent)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun checkGcmStatus(): Boolean {
        if (McsService.isConnected(this)) {
            Log.d(TAG, "checkGcmStatus: gcm isConnected")
            return true
        }
        Log.d(TAG, "checkGcmStatus: gcm need reconnect")
        return false
    }

    private suspend fun handleIntent(intent: Intent) {
        val action = intent.action
        if (checkGmsGcmStatus()) {
            Log.d(TAG, "handleIntent: checkGmsGcmStatus -> reset")
            runCatching { registerGcmInGms(this, intent) }.onFailure {
                Log.w(TAG, "handleIntent: registerGcmInGms error", it)
            }
            return
        }
        Log.d(TAG, "handleIntent: action: $action")
        when (action) {
            GcmConstants.ACTION_C2DM_RECEIVE -> {
                Log.d(TAG, "start handle gcm message")
                intent.extras?.let { notifyVerificationInfo(it) }
            }
            ACTION_GCM_REGISTER_ALL_ACCOUNTS,
            ACTION_GCM_CONNECTED -> {
                updateLocalAccountGroups()
            }
            ACTION_GCM_REGISTER_ACCOUNT -> {
                val accountName = intent.getStringExtra(KEY_GCM_REGISTER_ACCOUNT_NAME) ?: return
                Log.d(TAG, "GCM groups update account name: $accountName")
                val account = accountManager?.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)?.find { it.name == accountName } ?: return
                updateLocalAccountGroups(account)
            }
            ACTION_GCM_NOTIFY_COMPLETE -> {
                val accountName = intent.getStringExtra(EXTRA_NOTIFICATION_ACCOUNT) ?: return
                val notificationList = accountNotificationMap[accountName] ?: return
                notificationList.forEach {
                    val notificationId = it.first
                    val notificationData = it.second
                    updateNotificationReadState(accountName, notificationData, NOTIFICATION_STATUS_COMPLETE)
                    NotificationManagerCompat.from(this).cancel(notificationId)
                    Log.d(TAG, "Notification with $accountName updateNotificationReadState <$notificationId> to Completed.")
                }
                accountNotificationMap.remove(accountName)
            }
        }
    }

    private fun getCurrentLanguageTag(): String {
        return runCatching {
            if (SDK_INT >= 24) {
                resources.configuration.locales[0].toLanguageTag()
            } else {
                val locale = resources.configuration.locale
                locale.language + (if (locale.country.isEmpty()) "" else "-" + locale.country)
            }
        }.getOrDefault(Locale.getDefault().language)
    }

    private fun getDensityQualifier(): DeviceInfo.DensityQualifier {
        val dpi = resources.displayMetrics.densityDpi
        return when {
            dpi >= DisplayMetrics.DENSITY_XXHIGH -> DeviceInfo.DensityQualifier.XXHDPI
            dpi >= DisplayMetrics.DENSITY_XHIGH -> DeviceInfo.DensityQualifier.XHDPI
            dpi >= DisplayMetrics.DENSITY_HIGH -> DeviceInfo.DensityQualifier.HDPI
            dpi >= DisplayMetrics.DENSITY_TV -> DeviceInfo.DensityQualifier.TVDPI
            dpi >= DisplayMetrics.DENSITY_MEDIUM -> DeviceInfo.DensityQualifier.MDPI
            else -> DeviceInfo.DensityQualifier.LDPI
        }
    }

    private suspend fun requestNotificationInfo(account: Account, notificationData: NotificationData) = suspendCoroutine { sup ->
        try {
            val response = getGunsApiServiceClient(account, accountManager!!).GmsGnotsFetchByIdentifier().executeBlocking(FetchByIdentifierRequest.Builder().apply {
                config(GmsConfig.Builder().apply {
                    versionInfo(GmsConfig.GmsVersionInfo(Constants.GMS_VERSION_CODE))
                }.build())
                identifiers(NotificationIdentifierList.Builder().apply {
                    deviceInfo(DeviceInfo.Builder().apply {
                        densityQualifier(getDensityQualifier())
                        localeTag(getCurrentLanguageTag())
                        sdkVersion(SDK_INT)
                        density(resources.displayMetrics.density)
                        timeZoneId(TimeZone.getDefault().id)
                    }.build())
                    notifications(notificationData.identifier?.let { listOf(it) } ?: emptyList())
                }.build())
            }.build())
            sup.resume(response)
        } catch (e: Exception) {
            sup.resumeWithException(e)
        }
    }

    private suspend fun notifyVerificationInfo(data: Bundle) {
        Log.d(TAG, "notifyVerificationInfo: from: ${data.getString(GcmConstants.EXTRA_FROM)} data: $data")
        val gcmBodyType = data.getString(GcmConstants.EXTRA_GCM_BODY) ?: return
        if (GMS_GCM_NOTIFICATIONS != gcmBodyType) return
        val payloadData = data.getString(GcmConstants.EXTRA_GMS_GNOTS_PAYLOAD) ?: return
        val notificationData = NotificationData.ADAPTER.decode(Base64.decode(payloadData, DEFAULT_FLAGS))
        Log.w(TAG, "notifyVerificationInfo: $notificationData")
        if (notificationData.isActive == true) return
        val account = notificationData.userInfo?.userId?.let { id ->
            accountManager?.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)?.find {
                accountManager?.getUserData(it, AuthConstants.GOOGLE_USER_ID) == id
            }
        } ?: return
        Log.d(TAG, "notifyVerificationInfo: account: ${account.name}")
        val identifierResponse = withContext(Dispatchers.IO) {
            repeat(NOTIFICATION_REPEAT_NUM) { attempt ->
                try {
                    val notificationInfo = requestNotificationInfo(account, notificationData)
                    if (notificationInfo.notifications?.notificationDataList.isNullOrEmpty()) {
                        throw RuntimeException("Notification not found")
                    }
                    return@withContext notificationInfo
                } catch (e: Exception) {
                    Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                }
                delay(NOTIFICATION_DELAY_TIME)
            }
            return@withContext null
        }
        Log.d(TAG, "notifyVerificationInfo: identifierResponse: $identifierResponse")
        val notifications = identifierResponse?.notifications?.notificationDataList ?: return
        notifications.forEachIndexed { index, it ->
            Log.d(TAG, "notifyVerificationInfo: notifications: index:$index it: $it")
            updateNotificationReadState(account.name, it, NOTIFICATION_STATUS_READY)
            sendNotification(account, notificationIdGenerator.incrementAndGet(), it)
            updateNotificationReadState(account.name, it, NOTIFICATION_STATUS_COMPLETE)
        }
    }

    private fun sendNotification(account: Account, notificationId: Int, notificationData: NotificationData) {
        if (notificationData.isActive == true) return
        val content = notificationData.content ?: return
        val intentExtras = notificationData.intentActions?.primaryPayload?.extras ?: return
        val intent = Intent(this, MainActivity::class.java).apply {
            `package` = Constants.GMS_PACKAGE_NAME
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            intentExtras.forEach { putExtra(it.key, it.value_) }
            putExtra(KEY_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntentCompat.getActivity(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT, false)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(content.accountName)
            .setContentText(content.description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.description))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_google_logo)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(notificationId, builder.build())
        }
        runCatching { startActivity(intent) }
        accountNotificationMap.getOrPut(account.name) { mutableListOf() }.add(Pair(notificationId, notificationData))
    }

    private suspend fun updateGroupsWithAccount(account: Account, regId: String) {
        Log.d(TAG, "updateGroupsWithAccount: account: ${account.name}")
        val authManager = AuthManager(this, account.name, Constants.GMS_PACKAGE_NAME, GMS_GCM_OAUTH_SERVICE).apply {
            setItCaveatTypes("2")
        }
        val authsToken = runCatching { withContext(Dispatchers.IO) { authManager.requestAuth(true) }.parseAuthsToken() }.getOrNull() ?: return
        val extras = Bundle().apply {
            putString(GcmConstants.EXTRA_ACCOUNT_NAME, account.name)
            putString(GcmConstants.EXTRA_REG_ID, regId)
            putString(GcmConstants.EXTRA_AUTHS_TOKEN, authsToken)
            putString(GcmConstants.EXTRA_SEND_TO, GCM_GROUP_SENDER)
            putString(GcmConstants.EXTRA_SEND_FROM, GCM_GROUP_SENDER)
            putString(GcmConstants.EXTRA_MESSAGE_ID, "${System.currentTimeMillis() / 1000}-0")
        }
        Log.d(TAG, "updateGroupsWithAccount extras: $extras")
        val intent = Intent(GcmConstants.ACTION_GCM_SEND).apply {
            setPackage(Constants.GMS_PACKAGE_NAME)
            putExtras(extras)
            putExtra(GcmConstants.EXTRA_APP, Intent().apply { setPackage(Constants.GMS_PACKAGE_NAME) }.let { PendingIntentCompat.getBroadcast(this@GcmInGmsService, 0, it, 0, false) })
        }.also {
            it.putExtra(GcmConstants.EXTRA_MESSENGER, Messenger(object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (Binder.getCallingUid() == Process.myUid()) {
                        val history = sp?.getString(KEY_GCM_REG_ACCOUNT_LIST, "")
                        if (history?.contains(account.name) == true) {
                            Log.d(TAG, "updateGroupsWithAccount handleMessage history<$history> contains: ${account.name}")
                            return
                        }
                        val saveStr = if (history.isNullOrEmpty()) account.name else "${account.name}/$history"
                        sp?.edit()?.putString(KEY_GCM_REG_ACCOUNT_LIST, saveStr)?.apply()
                        Log.d(TAG, "updateGroupsWithAccount handleMessage save: $saveStr")
                    }
                }
            }))
        }
        sendOrderedBroadcast(intent, null)
    }

    private suspend fun updateLocalAccountGroups(account: Account? = null) {
        Log.d(TAG, "GMS $GMS_GCM_REGISTER_SENDER already registered, start updateLocalAccount")
        val regId = sp?.getString(KEY_GCM_REG_ID, null) ?: return
        val accounts = chimeGmsRegistrationHelper.handleRegistration(regId)
        if (accounts.isNotEmpty()) {
            Log.d(TAG, "updateLocalAccountGroups: handleRegistration done")
            accounts.forEach { updateGroupsWithAccount(it, regId) }
            return
        }
        val localGoogleAccounts = accountManager?.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE) ?: return
        val accountList = sp?.getString(KEY_GCM_REG_ACCOUNT_LIST, null)
        if (account != null && accountList?.contains(account.name) != true) {
            Log.d(TAG, "updateLocalAccountGroups: single account: ${account.name}")
            updateGroupsWithAccount(account, regId)
            return
        }
        Log.d(TAG, "updateLocalAccountGroups: registrationAccountList: $accountList")
        val needRegisterAccounts = if (accountList == null) localGoogleAccounts.toList() else localGoogleAccounts.filter { !accountList.contains(it.name) }
        Log.d(TAG, "updateLocalAccountGroups: needRegisterAccounts: ${needRegisterAccounts.joinToString("|") { it.name }}")
        if (needRegisterAccounts.isEmpty()) return
        needRegisterAccounts.forEach { updateGroupsWithAccount(it, regId) }
    }

    private suspend fun registerGcmInGms(context: Context, intent: Intent) {
        Log.i(TAG, "Registering GMS $GMS_GCM_REGISTER_SENDER")
        val regId = withContext(Dispatchers.IO) {
            val request = RegisterRequest().build(context)
                .checkin(LastCheckinInfo.read(context))
                .app(Constants.GMS_PACKAGE_NAME, Constants.GMS_PACKAGE_SIGNATURE_SHA1, BuildConfig.VERSION_CODE)
                .sender(GMS_GCM_REGISTER_SENDER)
                .extraParam("subscription", GMS_GCM_REGISTER_SUBSCRIPTION)
                .extraParam("X-subscription", GMS_GCM_REGISTER_SUBSCRIPTION)
                .extraParam("subtype", GMS_GCM_REGISTER_SUBTYPE)
                .extraParam("X-subtype", GMS_GCM_REGISTER_SUBTYPE)
                .extraParam("scope", GMS_GCM_REGISTER_SCOPE)
            val gcmDatabase = GcmDatabase(context)
            ensureAppRegistrationAllowed(context, gcmDatabase, request.app)
            completeRegisterRequest(context, gcmDatabase, request).getString(GcmConstants.EXTRA_REGISTRATION_ID)
        }
        Log.d(TAG, "GCM IN GMS regId: $regId")
        if (regId == null) {
            Log.w(TAG, "registerGcmInGms reg id is null")
            return
        }
        val sharedPreferencesEditor = sp?.edit()
        sharedPreferencesEditor?.putLong(KEY_GCM_ANDROID_ID, LastCheckinInfo.read(context).androidId)
        sharedPreferencesEditor?.putString(KEY_GCM_REG_ID, regId)
        sharedPreferencesEditor?.putString(KEY_GCM_REG_SENDER, GMS_GCM_REGISTER_SENDER)
        sharedPreferencesEditor?.putLong(KEY_GCM_REG_TIME, System.currentTimeMillis())
        sharedPreferencesEditor?.remove(KEY_GCM_REG_ACCOUNT_LIST)
        chimeGmsRegistrationHelper.resetAllData()
        if (sharedPreferencesEditor?.commit() == false) {
            Log.d(TAG, "Failed to write GMS registration")
        } else {
            Log.d(TAG, "registerGcmInGms: sendBroadcast: ${intent.action}")
            Intent(intent.action).apply {
                setPackage(Constants.GMS_PACKAGE_NAME)
                putExtras(intent)
            }.let { sendBroadcast(it) }
        }
    }

    private suspend fun updateNotificationReadState(accountName: String, notificationData: NotificationData, readState: Int) {
        if (accountName.isEmpty() || notificationData.identifier?.uniqueId?.isEmpty() == true) {
            return
        }
        try {
            val identifier = notificationData.identifier
            val readStateList = when {
                readState == NOTIFICATION_STATUS_COMPLETE -> {
                    listOf(
                            ReadStateItem.Builder().apply {
                                this.notification = identifier
                                this.state = null
                                this.status = readState
                            }.build()
                    )
                }
                notificationData.content?.actionButtons.isNullOrEmpty() -> {
                    Log.w(TAG, "No action buttons found, skipping read state update.")
                    return
                }
                else -> {
                    notificationData.content!!.actionButtons.map {
                        ReadStateItem.Builder().apply {
                            this.notification = identifier
                            this.state = it.icon
                            this.status = readState
                        }.build()
                    }
                }
            }
            withContext(Dispatchers.IO) {
                sendNotificationReadState(accountName, ReadStateList.Builder().apply { items = readStateList }.build())
            }
            Log.i(TAG, "Notification read state updated successfully for account: $accountName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update the notification(s) read state.", e)
        }
    }

    private fun sendNotificationReadState(accountName: String, readStateList: ReadStateList) {
        val account = accountManager?.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)?.find { it.name == accountName } ?: return
        getGunsApiServiceClient(account, accountManager!!).GmsGnotsSetReadStates().executeBlocking(
            GmsGnotsSetReadStatesRequest.Builder().apply {
                config = GmsConfig.Builder().apply {
                    versionInfo(GmsConfig.GmsVersionInfo(Constants.GMS_VERSION_CODE))
                }.build()
                readStates = readStateList
            }.build()
        )
    }

    private fun checkGmsGcmStatus(): Boolean {
        val targetId = LastCheckinInfo.read(this).androidId
        val regSender = sp?.getString(KEY_GCM_REG_SENDER, null)
        val regId = sp?.getString(KEY_GCM_REG_ID, null)
        val androidId = sp?.getLong(KEY_GCM_ANDROID_ID, 0)
        val regTime = sp?.getLong(KEY_GCM_REG_TIME, 0) ?: 0L
        return targetId != androidId || regSender == null || regId == null || regTime + GCM_GMS_REG_REFRESH_S * 1000 < System.currentTimeMillis()
    }

    private fun AuthResponse.parseAuthsToken(): String? {
        Log.d(TAG, "parseAuthsToken start: auths: $auths itMetadata: $itMetadata")
        if (auths.isNullOrEmpty() || itMetadata.isNullOrEmpty()) return null
        if (!auths.startsWith(AUTHS_TOKEN_PREFIX)) return null
        try {
            val tokenBase64 = auths.substring(AUTHS_TOKEN_PREFIX.length)
            val authData = ItAuthData.ADAPTER.decode(Base64.decode(tokenBase64, DEFAULT_FLAGS))
            val metadata = ItMetadataData.ADAPTER.decode(Base64.decode(itMetadata, DEFAULT_FLAGS))
            val authorization = OAuthAuthorization.Builder().apply {
                effectiveDurationSeconds(min(metadata.liveTime ?: Int.MAX_VALUE, expiresInDurationSec))
                if (metadata.field_?.types?.contains(TokenField.FieldType.SCOPE) == true) {
                    val scopeIds = metadata.entries.flatMap { entry ->
                        entry.name.map { scope -> entry to scope }
                    }.filter { (_, scope) ->
                        scope in grantedScopes
                    }.mapNotNull { (entry, _) ->
                        entry.id
                    }.toSet()
                    scopeIds(scopeIds.toList())
                }
            }.build()
            val oAuthTokenData = OAuthTokenData.Builder().apply {
                fieldType(TokenField.FieldType.SCOPE.value)
                authorization(authorization.encodeByteString())
                durationMillis(0)
            }.build()
            val tokenDataBytes = oAuthTokenData.encode()
            val secretKey: ByteArray? = authData.signature?.toByteArray()
            val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secretKey, "HmacSHA256")) }
            val bytes: ByteArray = mac.doFinal(tokenDataBytes)
            val newAuthData = authData.newBuilder().apply {
                tokens(arrayListOf(oAuthTokenData.encodeByteString()))
                signature(ByteString.of(*bytes))
            }.build()
            return AUTHS_TOKEN_PREFIX + Base64.encodeToString(newAuthData.encode(), DEFAULT_FLAGS)
        } catch (e: Exception) {
            Log.w(TAG, "parseAuthsToken: ", e);
            return null;
        }
    }

    private fun getGunsApiServiceClient(account: Account, accountManager: AccountManager): GunsGmscoreApiServiceClient {
        val oauthToken = accountManager.blockingGetAuthToken(account, GMS_NOTS_OAUTH_SERVICE, true)
        return createGrpcClient<GunsGmscoreApiServiceClient>(baseUrl = GMS_NOTS_BASE_URL, oauthToken = oauthToken)
    }
}

class GcmRegistrationReceiver : WakefulBroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val shouldReceiveTwoStepVerification = AuthPrefs.shouldReceiveTwoStepVerification(context)
        if (!shouldReceiveTwoStepVerification) {
            Log.d(TAG, "GcmRegistrationReceiver onReceive: <Two-Step> Switch not allowed ")
            return
        }
        Log.d(TAG, "GcmRegistrationReceiver onReceive: action: ${intent.action}")
        val callIntent = Intent(context, GcmInGmsService::class.java)
        callIntent.action = intent.action
        if (ACTION_GCM_REGISTER_ACCOUNT == intent.action || ACTION_GCM_NOTIFY_COMPLETE == intent.action || GcmConstants.ACTION_C2DM_RECEIVE == intent.action) {
            callIntent.putExtras(intent.extras!!)
        }
        ForegroundServiceContext(context).startService(callIntent)
    }
}
