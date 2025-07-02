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
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.legacy.content.WakefulBroadcastReceiver
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.BuildConfig
import com.google.android.gms.R
import com.squareup.wire.GrpcClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.ByteString
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
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

const val ACTION_GCM_RECONNECT = "org.microg.gms.gcm.RECONNECT"
const val ACTION_GCM_REGISTERED = "org.microg.gms.gcm.REGISTERED"
const val ACTION_GCM_REGISTER_ACCOUNT = "org.microg.gms.gcm.REGISTER_ACCOUNT"
const val ACTION_GCM_NOTIFY_COMPLETE = "org.microg.gms.gcm.NOTIFY_COMPLETE"
const val KEY_GCM_REGISTER_ACCOUNT_NAME = "register_account_name"
const val EXTRA_NOTIFICATION_ACCOUNT = "notification_account"

private const val TAG = "GcmInGmsService"

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
private const val GMS_NOTS_OAUTH_SERVICE = "oauth2:https://www.googleapis.com/auth/notifications"
private const val NOTIFICATION_STATUS_READY = 2
private const val NOTIFICATION_STATUS_COMPLETE = 5

class GcmInGmsService : LifecycleService() {
    private val notificationIdGenerator = AtomicInteger(0)
    private var sp: SharedPreferences? = null
    private var accountManager: AccountManager? = null
    private val activeNotifications = HashMap<String, NotificationData>()

    override fun onCreate() {
        super.onCreate()
        sp = getSharedPreferences("com.google.android.gcm", MODE_PRIVATE) ?: throw RuntimeException("sp get error")
        accountManager = getSystemService(ACCOUNT_SERVICE) as AccountManager? ?: throw RuntimeException("accountManager is null")
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            WakefulBroadcastReceiver.completeWakefulIntent(intent)
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
            registerGcmInGms(this, intent)
            return
        }
        Log.d(TAG, "handleIntent: action: $action")
        if (action == ACTION_GCM_REGISTERED) {
            updateLocalAccountGroups()
        } else if (action == ACTION_GCM_REGISTER_ACCOUNT) {
            val accountName = intent.getStringExtra(KEY_GCM_REGISTER_ACCOUNT_NAME) ?: return
            Log.d(TAG, "GCM groups update account name: $accountName")
            val account = accountManager?.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)?.find { it.name == accountName } ?: return
            updateGroupsWithAccount(account)
        } else if (action == GcmConstants.ACTION_C2DM_RECEIVE) {
            Log.d(TAG, "start handle gcm message")
            intent.extras?.let { notifyVerificationInfo(it) }
        } else if (action == ACTION_GCM_NOTIFY_COMPLETE) {
            val accountName = intent.getStringExtra(EXTRA_NOTIFICATION_ACCOUNT)
            val notificationData = activeNotifications[accountName]
            if (notificationData != null) {
                Log.d(TAG, "Notification with $accountName updateNotificationReadState to Completed.")
                updateNotificationReadState(accountName!!, notificationData, NOTIFICATION_STATUS_COMPLETE)
                activeNotifications.remove(accountName)
                NotificationManagerCompat.from(this).cancel(accountName.hashCode())
            }
        }
    }

    private suspend fun notifyVerificationInfo(data: Bundle) {
        Log.d(TAG, "notifyVerificationInfo: from: ${data.getString(GcmConstants.EXTRA_FROM)} data: $data")
        val gcmBodyType = data.getString(GcmConstants.EXTRA_GCM_BODY) ?: return
        if (GMS_GCM_NOTIFICATIONS != gcmBodyType) return
        val payloadData = data.getString(GcmConstants.EXTRA_GMS_GNOTS_PAYLOAD) ?: return
        val notificationData = NotificationData.ADAPTER.decode(Base64.decode(payloadData, DEFAULT_FLAGS))
        if (notificationData.isActive == true) return
        val account = notificationData.userInfo?.userId?.let { id ->
            accountManager?.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)?.find {
                accountManager?.getUserData(it, AuthConstants.GOOGLE_USER_ID) == id
            }
        } ?: return
        Log.d(TAG, "notifyVerificationInfo: account: ${account.name}")
        val identifierResponse = withContext(Dispatchers.IO) {
            getGunsApiServiceClient(account, accountManager!!).GmsGnotsFetchByIdentifier().executeBlocking(FetchByIdentifierRequest.Builder().apply {
                config(GmsConfig.Builder().apply {
                    versionInfo(GmsConfig.GmsVersionInfo(Constants.GMS_VERSION_CODE))
                }.build())
                identifiers(NotificationIdentifierList.Builder().apply {
                    deviceInfo(DeviceInfo.Builder().apply {
                        localeTag(Locale.getDefault().language)
                        sdkVersion(android.os.Build.VERSION.SDK_INT)
                        density(resources.displayMetrics.density)
                        timeZoneId(TimeZone.getDefault().id)
                    }.build())
                    notifications(notificationData.identifier?.let { listOf(it) } ?: emptyList())
                }.build())
            }.build())
        }
        Log.d(TAG, "notifyVerificationInfo: identifierResponse: $identifierResponse")
        val notifications = identifierResponse.notifications?.notifications ?: return
        notifications.forEachIndexed { index, it ->
            Log.d(TAG, "notifyVerificationInfo: notifications: index:$index it: $it")
            sendNotification(account.name.hashCode(), it)
            updateNotificationReadState(account.name, it, NOTIFICATION_STATUS_READY)
            activeNotifications.put(account.name, it)
        }
    }

    private fun sendNotification(notificationId: Int, notificationData: NotificationData) {
        if (notificationData.isActive == true) return
        val content = notificationData.content ?: return
        val intentExtras = notificationData.intentActions?.primaryPayload?.extras ?: return
        val intent = Intent(this, MainActivity::class.java).apply {
            `package` = Constants.GMS_PACKAGE_NAME
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intentExtras.forEach { putExtra(it.key, it.value_) }
        }
        val requestCode = notificationIdGenerator.incrementAndGet()
        val pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
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
        startActivity(intent)
    }

    private suspend fun updateGroupsWithAccount(account: Account) {
        Log.d(TAG, "updateGroupsWithAccount: account: ${account.name}")
        var regId = sp?.getString(KEY_GCM_REG_ID, null) ?: return
        var authManager = AuthManager(this, account.name, Constants.GMS_PACKAGE_NAME, GMS_GCM_OAUTH_SERVICE).apply {
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
            putExtra(GcmConstants.EXTRA_APP, Intent().apply { setPackage(Constants.GMS_PACKAGE_NAME) }.let { PendingIntent.getBroadcast(this@GcmInGmsService, 0, it, 0) })
        }.also {
            it.putExtra(GcmConstants.EXTRA_MESSENGER, Messenger(object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (Binder.getCallingUid() == Process.myUid()) {
                        Log.d(TAG, "updateGroupsWithAccount handleMessage save: ${account.name}")
                        val history = sp?.getString(KEY_GCM_REG_ACCOUNT_LIST, null)
                        sp?.edit()?.putString(KEY_GCM_REG_ACCOUNT_LIST, if (history != null) "${account.name}/$history" else account.name)?.apply()
                    }
                }
            }))
        }
        sendOrderedBroadcast(intent, null)
    }

    private suspend fun updateLocalAccountGroups() {
        Log.d(TAG, "GMS $GMS_GCM_REGISTER_SENDER already registered, start updateLocalAccount")
        val localGoogleAccounts = accountManager?.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE) ?: return
        val accountList = sp?.getString(KEY_GCM_REG_ACCOUNT_LIST, null)
        Log.d(TAG, "updateLocalAccountGroups: accountList: $accountList")
        val needRegisterAccounts = if (accountList == null) localGoogleAccounts.toList() else localGoogleAccounts.filter { !accountList.contains(it.name) }
        Log.d(TAG, "updateLocalAccountGroups: needRegisterAccounts: ${needRegisterAccounts.map { it.name }.joinToString(" ")}")
        if (needRegisterAccounts.isEmpty()) return
        for (account in needRegisterAccounts) {
            updateGroupsWithAccount(account)
        }
    }

    private suspend fun registerGcmInGms(context: Context, intent: Intent) {
        Log.i(TAG, "Registering GMS $GMS_GCM_REGISTER_SENDER")
        val regId = withContext(Dispatchers.IO) {
            completeRegisterRequest(
                context, GcmDatabase(context), RegisterRequest().build(context)
                    .checkin(LastCheckinInfo.read(context))
                    .app(Constants.GMS_PACKAGE_NAME, Constants.GMS_PACKAGE_SIGNATURE_SHA1, BuildConfig.VERSION_CODE)
                    .sender(GMS_GCM_REGISTER_SENDER)
                    .extraParam("subscription", GMS_GCM_REGISTER_SUBSCRIPTION)
                    .extraParam("X-subscription", GMS_GCM_REGISTER_SUBSCRIPTION)
                    .extraParam("subtype", GMS_GCM_REGISTER_SUBTYPE)
                    .extraParam("X-subtype", GMS_GCM_REGISTER_SUBTYPE)
                    .extraParam("scope", GMS_GCM_REGISTER_SCOPE)
            )
                .getString(GcmConstants.EXTRA_REGISTRATION_ID)
        }
        Log.d(TAG, "GCM IN GMS regId: $regId")
        val sharedPreferencesEditor = sp?.edit()
        sharedPreferencesEditor?.putString(KEY_GCM_REG_ID, regId)
        sharedPreferencesEditor?.putString(KEY_GCM_REG_SENDER, GMS_GCM_REGISTER_SENDER)
        sharedPreferencesEditor?.putLong(KEY_GCM_REG_TIME, System.currentTimeMillis())
        sharedPreferencesEditor?.remove(KEY_GCM_REG_ACCOUNT_LIST)
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

    private fun updateNotificationReadState(accountName: String, notificationData: NotificationData, readState: Int) {
        if (accountName.isEmpty() || notificationData.identifier?.uniqueId?.isEmpty() == true) {
            return
        }
        try {
            val identifier = notificationData.identifier
            val actionButtons = notificationData.content?.actionButtons
            if (actionButtons.isNullOrEmpty()) {
                return
            }
            val readStateList = actionButtons.map { actionButton ->
                ReadStateItem.Builder().apply {
                    notification = identifier
                    state = actionButton.icon
                    status = readState
                }.build()
            }
            sendNotificationReadState(accountName, ReadStateList.Builder().apply { items = readStateList }.build())
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
        val regSender = sp?.getString(KEY_GCM_REG_SENDER, null)
        val regId = sp?.getString(KEY_GCM_REG_ID, null)
        val regTime = sp?.getLong(KEY_GCM_REG_TIME, 0) ?: 0L
        return regSender == null || regId == null || regTime + GCM_GMS_REG_REFRESH_S * 1000 < System.currentTimeMillis()
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
        val token = accountManager.blockingGetAuthToken(account, GMS_NOTS_OAUTH_SERVICE, true)
        val client = OkHttpClient().newBuilder().addInterceptor(HeaderInterceptor(token)).build()
        val grpcClient = GrpcClient.Builder().client(client).baseUrl("https://notifications-pa.googleapis.com").minMessageToCompress(Long.MAX_VALUE).build()
        return grpcClient.create(GunsGmscoreApiServiceClient::class)
    }

    private class HeaderInterceptor(
        private val oauthToken: String,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val original = chain.request().newBuilder().header("Authorization", "Bearer $oauthToken")
            return chain.proceed(original.build())
        }
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
        startWakefulService(context, callIntent)
    }
}