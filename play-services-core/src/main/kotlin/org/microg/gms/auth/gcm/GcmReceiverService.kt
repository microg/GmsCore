/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.gcm

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.legacy.content.WakefulBroadcastReceiver
import com.google.android.gms.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.microg.gms.accountsettings.ui.KEY_NOTIFICATION_ID
import org.microg.gms.accountsettings.ui.MainActivity
import org.microg.gms.accountsettings.ui.runOnMainLooper
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthPrefs
import org.microg.gms.common.Constants
import org.microg.gms.common.ForegroundServiceContext
import org.microg.gms.gcm.AuthHeaderInterceptor
import org.microg.gms.gcm.DEFAULT_FLAGS
import org.microg.gms.gcm.DeviceInfo
import org.microg.gms.gcm.EXTRA_NOTIFICATION_ACCOUNT
import org.microg.gms.gcm.FetchByIdentifierRequest
import org.microg.gms.gcm.GMS_NOTS_BASE_URL
import org.microg.gms.gcm.GMS_NOTS_OAUTH_SERVICE
import org.microg.gms.gcm.GcmConstants
import org.microg.gms.gcm.GmsConfig
import org.microg.gms.gcm.GmsGnotsSetReadStatesRequest
import org.microg.gms.gcm.GunsGmscoreApiServiceClient
import org.microg.gms.gcm.NotificationData
import org.microg.gms.gcm.NotificationIdentifierList
import org.microg.gms.gcm.ReadStateItem
import org.microg.gms.gcm.ReadStateList
import org.microg.gms.gcm.ReceiverService
import org.microg.gms.gcm.createGrpcClient
import org.microg.gms.profile.Build.VERSION.SDK_INT
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "Gms2FA"
private const val CHANNEL_ID = "gcm_notification"
private const val CHANNEL_NAME = "gnots"
private const val GMS_GCM_NOTIFICATIONS = "notifications"
private const val NOTIFICATION_STATUS_READY = 2
private const val NOTIFICATION_STATUS_COMPLETE = 5
private const val NOTIFICATION_REPEAT_NUM = 3
private const val NOTIFICATION_DELAY_TIME = 500L

const val ACTION_GCM_NOTIFY_COMPLETE = "org.microg.gms.gcm.NOTIFY_COMPLETE"

class GcmReceiver : WakefulBroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val shouldReceiveTwoStepVerification = AuthPrefs.shouldReceiveTwoStepVerification(context)
        if (!shouldReceiveTwoStepVerification) {
            Log.d(TAG, "onReceive: <Two-Step> Switch not allowed ")
            return
        }
        Log.d(TAG, "onReceive: action: ${intent.action}")
        val callIntent = Intent(context, GcmReceiverService::class.java)
        callIntent.action = intent.action
        intent.extras?.let { callIntent.putExtras(it) }
        ForegroundServiceContext(context).startService(callIntent)
    }
}

class GcmReceiverService : ReceiverService(TAG) {

    companion object {
        private val accountNotificationMap = HashMap<String, MutableList<Pair<Int, NotificationData>>>()
        private val notificationIdGenerator = AtomicInteger(0)
    }

    override fun onCreate() {
        super.onCreate()
        if (SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun allowed(): Boolean = AuthPrefs.shouldReceiveTwoStepVerification(this)

    override fun receiver(intent: Intent) {
        Log.d(TAG, "receiver: run thread -> ${Thread.currentThread().name}")
        Log.d(TAG, "receiver: action: ${intent.action} data: ${intent.extras}")
        val accountManager = getSystemService(ACCOUNT_SERVICE) as AccountManager? ?: return run { Log.w(TAG, "receiver: accountManager is null") }
        val data = intent.extras ?: return run { Log.w(TAG, "receiver: intent.extras is null") }
        if (intent.action == ACTION_GCM_NOTIFY_COMPLETE) {
            Log.d(TAG, "receiver: remove Notification")
            val accountName = intent.getStringExtra(EXTRA_NOTIFICATION_ACCOUNT) ?: return
            val notificationList = accountNotificationMap[accountName] ?: return
            notificationList.forEach {
                val notificationId = it.first
                val notificationData = it.second
                updateNotificationReadState(accountManager, accountName, notificationData, NOTIFICATION_STATUS_COMPLETE)
                NotificationManagerCompat.from(this).cancel(notificationId)
                Log.d(TAG, "Notification with $accountName updateNotificationReadState <$notificationId> to Completed.")
            }
            accountNotificationMap.remove(accountName)
            return
        }
        Log.d(TAG, "notifyVerificationInfo: from: ${data.getString(GcmConstants.EXTRA_FROM)} data: $data")
        val gcmBodyType = data.getString(GcmConstants.EXTRA_GCM_BODY) ?: return run { Log.w(TAG, "receiver: EXTRA_GCM_BODY is null!") }
        if (GMS_GCM_NOTIFICATIONS != gcmBodyType) return run { Log.w(TAG, "receiver: gcmBodyType is not notifications") }
        val payloadData = data.getString(GcmConstants.EXTRA_GMS_GNOTS_PAYLOAD) ?: return run { Log.w(TAG, "receiver: payloadData is null!") }
        val notificationData = NotificationData.ADAPTER.decode(Base64.decode(payloadData, DEFAULT_FLAGS))
        Log.w(TAG, "notifyVerificationInfo: $notificationData")
        if (notificationData.isActive == true) return run { Log.w(TAG, "receiver: notification isActive!") }
        val account = notificationData.userInfo?.userId?.let { id ->
            accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).find {
                accountManager.getUserData(it, AuthConstants.GOOGLE_USER_ID) == id
            }
        } ?: return run { Log.w(TAG, "receiver: account not matched!") }
        Log.d(TAG, "notifyVerificationInfo: account: ${account.name}")
        val identifierResponse = runBlocking {
            repeat(NOTIFICATION_REPEAT_NUM) { attempt ->
                try {
                    val notificationInfo = requestNotificationInfo(accountManager, account, notificationData)
                    if (notificationInfo.notifications?.notificationDataList.isNullOrEmpty()) {
                        throw RuntimeException("Notification not found")
                    }
                    return@runBlocking notificationInfo
                } catch (e: Exception) {
                    Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                }
                delay(NOTIFICATION_DELAY_TIME)
            }
            return@runBlocking null
        }
        Log.d(TAG, "notifyVerificationInfo: identifierResponse: $identifierResponse")
        val notifications = identifierResponse?.notifications?.notificationDataList ?: return run { Log.w(TAG, "receiver: notifications is null!") }
        notifications.forEachIndexed { index, it ->
            Log.d(TAG, "notifyVerificationInfo: notifications: index:$index it: $it")
            updateNotificationReadState(accountManager, account.name, it, NOTIFICATION_STATUS_READY)
            sendNotification(account, notificationIdGenerator.incrementAndGet(), it)
            updateNotificationReadState(accountManager, account.name, it, NOTIFICATION_STATUS_COMPLETE)
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
        runCatching { runOnMainLooper { startActivity(intent) } }
        accountNotificationMap.getOrPut(account.name) { mutableListOf() }.add(Pair(notificationId, notificationData))
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

    private fun requestNotificationInfo(accountManager: AccountManager, account: Account, notificationData: NotificationData) =
        getGunsApiServiceClient(account, accountManager!!).GmsGnotsFetchByIdentifier().executeBlocking(FetchByIdentifierRequest.Builder().apply {
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

    private fun updateNotificationReadState(accountManager: AccountManager, accountName: String, notificationData: NotificationData, readState: Int) {
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
            sendNotificationReadState(accountManager, accountName, ReadStateList.Builder().apply { items = readStateList }.build())
            Log.i(TAG, "Notification read state updated successfully for account: $accountName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update the notification(s) read state.", e)
        }
    }

    private fun sendNotificationReadState(accountManager: AccountManager, accountName: String, readStateList: ReadStateList) {
        val account = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).find { it.name == accountName } ?: return
        getGunsApiServiceClient(account, accountManager).GmsGnotsSetReadStates().executeBlocking(
            GmsGnotsSetReadStatesRequest.Builder().apply {
                config = GmsConfig.Builder().apply {
                    versionInfo(GmsConfig.GmsVersionInfo(Constants.GMS_VERSION_CODE))
                }.build()
                readStates = readStateList
            }.build()
        )
    }

    private fun getGunsApiServiceClient(account: Account, accountManager: AccountManager): GunsGmscoreApiServiceClient {
        val oauthToken = accountManager.blockingGetAuthToken(account, GMS_NOTS_OAUTH_SERVICE, true)
        return createGrpcClient<GunsGmscoreApiServiceClient>(baseUrl = GMS_NOTS_BASE_URL, interceptor = AuthHeaderInterceptor(oauthToken))
    }
}