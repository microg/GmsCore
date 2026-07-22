/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.phone

import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.RECEIVE_SMS
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.database.Cursor
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.Telephony
import android.telephony.SmsMessage
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.microg.gms.auth.phone.SmsRetrieverRequestType.RETRIEVER
import org.microg.gms.auth.phone.SmsRetrieverRequestType.USER_CONSENT
import org.microg.gms.common.Constants
import org.microg.gms.utils.getSignatures
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger


private const val TAG = "SmsRetrieverCore"

private const val ACTION_SMS_RETRIEVE_TIMEOUT = "org.microg.gms.auth.phone.ACTION_SMS_RETRIEVE_TIMEOUT"
private const val EXTRA_REQUEST_ID = "requestId"
private const val TIMEOUT = 1000 * 60 * 5 // 5 minutes
private const val MESSAGE_MAX_LEN = 140

class SmsRetrieverCore(private val context: Context, override val lifecycle: Lifecycle) : LifecycleOwner, DefaultLifecycleObserver {
    private val requests: HashMap<Int, SmsRetrieverRequest> = hashMapOf()
    private val requestIdCounter = AtomicInteger(0)
    private lateinit var timeoutBroadcastReceiver: BroadcastReceiver
    private lateinit var smsBroadcastReceiver: BroadcastReceiver
    private var requestCode = 0
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        lifecycle.addObserver(this)
    }

    @TargetApi(19)
    private fun configureBroadcastListenersIfNeeded() {
        synchronized(this) {
            if (!this::timeoutBroadcastReceiver.isInitialized) {
                val intentFilter = IntentFilter(ACTION_SMS_RETRIEVE_TIMEOUT)
                timeoutBroadcastReceiver = TimeoutReceiver()
                ContextCompat.registerReceiver(context, timeoutBroadcastReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
            }
            if (!this::smsBroadcastReceiver.isInitialized) {
                val intentFilter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
                intentFilter.priority = 999
                smsBroadcastReceiver = SmsReceiver()
                context.registerReceiver(smsBroadcastReceiver, intentFilter)
            }
        }
    }

    private suspend fun ensureReady(permissions: Array<String>): Boolean {
        if (SDK_INT < 19) throw RuntimeException("Version not supported")
        if (!ensurePermission(permissions)) return false
        configureBroadcastListenersIfNeeded()
        return true
    }

    suspend fun startSmsRetriever(packageName: String) {
        val appHashString = getHashString(packageName)

        if (!ensureReady(arrayOf(RECEIVE_SMS)))
            throw RuntimeException("Initialization failed")
        if (anyOtherPackageHasHashString(packageName, appHashString))
            throw RuntimeException("Collision in hash string, can't use SMS Retriever API")
        if (requests.values.any { it.packageName == packageName && it.appHashString == appHashString && it.type == RETRIEVER })
            throw RuntimeException("App already listening")

        val request = SmsRetrieverRequest(
            id = requestIdCounter.incrementAndGet(),
            type = RETRIEVER,
            packageName = packageName,
            appHashString = appHashString,
            timeoutPendingIntent = getTimeoutPendingIntent(context, packageName)
        )
        requests[request.id] = request
        alarmManager.set(AlarmManager.RTC, request.creation + TIMEOUT, request.timeoutPendingIntent)
    }

    suspend fun startWithConsentPrompt(packageName: String, senderPhoneNumber: String?) {
        if (!ensureReady(arrayOf(RECEIVE_SMS, READ_CONTACTS)))
            throw RuntimeException("Initialization failed")
        if (requests.values.any { it.packageName == packageName && it.senderPhoneNumber == senderPhoneNumber && it.type == USER_CONSENT })
            throw RuntimeException("App already listening")

        val request = SmsRetrieverRequest(
            id = requestIdCounter.incrementAndGet(),
            type = USER_CONSENT,
            packageName = packageName,
            senderPhoneNumber = senderPhoneNumber,
            timeoutPendingIntent = getTimeoutPendingIntent(context, packageName)
        )
        requests[request.id] = request
        alarmManager.set(AlarmManager.RTC, request.creation + TIMEOUT, request.timeoutPendingIntent)
    }

    fun hasOngoingUserConsentRequest(): Boolean {
        return requests.values.any { it.type == USER_CONSENT }
    }

    private fun sendRetrieverBroadcast(request: SmsRetrieverRequest, messageBody: String) {
        sendReply(request, Status.SUCCESS, bundleOf(SmsRetriever.EXTRA_SMS_MESSAGE to messageBody))
    }

    private fun sendUserConsentBroadcast(request: SmsRetrieverRequest, messageBody: String) {
        val userConsentIntent = Intent(context, UserConsentPromptActivity::class.java)
        userConsentIntent.setPackage(Constants.GMS_PACKAGE_NAME)
        userConsentIntent.putExtra(EXTRA_MESSENGER, Messenger(object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (Binder.getCallingUid() == Process.myUid()) {
                    if (msg.what == MSG_REQUEST_MESSAGE_BODY) {
                        msg.replyTo?.send(Message.obtain().apply {
                            what = 1
                            data = bundleOf("message" to messageBody)
                        })
                    } else if (msg.what == MSG_CONSUME_MESSAGE) {
                        finishRequest(request)
                    }
                }
            }
        }))

        sendReply(request, Status.SUCCESS, bundleOf(SmsRetriever.EXTRA_CONSENT_INTENT to userConsentIntent), false)
    }

    private fun getTimeoutPendingIntent(context: Context, packageName: String): PendingIntent {
        val intent = Intent(ACTION_SMS_RETRIEVE_TIMEOUT)
        intent.setPackage(packageName)
        return PendingIntentCompat.getBroadcast(context, ++requestCode, intent, 0, false)!!
    }

    private fun tryHandleIncomingMessageAsRetrieverMessage(messageBody: String): Boolean {
        for (request in requests.values) {
            if (request.type == RETRIEVER) {
                // 11-digit hash code that uniquely identifies your app
                if (request.appHashString.isNullOrBlank() || !messageBody.contains(request.appHashString)) continue

                sendRetrieverBroadcast(request, messageBody)
                return true
            }
        }
        return false
    }

    private fun tryHandleIncomingMessageAsUserConsentMessage(senderPhoneNumber: String?, messageBody: String): Boolean {
        val senderPhoneNumber = senderPhoneNumber ?: return false

        // 4-10 digit alphanumeric code containing at least one number
        if (messageBody.split("[^A-Za-z0-9]".toRegex()).none { it.length in 4..10 && it.any(Char::isDigit) }) return false

        // Sender cannot be in the user's Contacts list
        if (isPhoneNumberInContacts(context, senderPhoneNumber)) return false

        for (request in requests.values) {
            if (request.type == USER_CONSENT) {
                if (!request.senderPhoneNumber.isNullOrBlank() && request.senderPhoneNumber != senderPhoneNumber) continue

                sendUserConsentBroadcast(request, messageBody)
                return true
            }
        }
        return false
    }

    private fun handleIncomingSmsMessage(senderPhoneNumber: String?, messageBody: String) {
        Log.d(TAG, "handleIncomingSmsMessage: senderPhoneNumber:$senderPhoneNumber messageBody: $messageBody")
        if (messageBody.isBlank()) return

        if (tryHandleIncomingMessageAsRetrieverMessage(messageBody)) return
        if (tryHandleIncomingMessageAsUserConsentMessage(senderPhoneNumber, messageBody)) return
    }

    fun handleTimeout(requestId: Int) {
        val request = requests[requestId] ?: return
        sendReply(request, Status(CommonStatusCodes.TIMEOUT))
    }

    private fun sendReply(request: SmsRetrieverRequest, status: Status, extras: Bundle = Bundle.EMPTY, finish: Boolean = true) {
        Log.d(TAG, "Send reply to ${request.packageName} ${CommonStatusCodes.getStatusCodeString(status.statusCode)}")

        val intent = Intent(SmsRetriever.SMS_RETRIEVED_ACTION)
        intent.setPackage(request.packageName)
        intent.putExtras(extras)
        intent.putExtra(SmsRetriever.EXTRA_STATUS, status)
        context.sendBroadcast(intent)

        if (finish) finishRequest(request)
    }

    fun finishRequest(request: SmsRetrieverRequest) {
        alarmManager.cancel(request.timeoutPendingIntent)
        requests.remove(request.id)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        if (this::smsBroadcastReceiver.isInitialized) context.unregisterReceiver(smsBroadcastReceiver)
        if (this::timeoutBroadcastReceiver.isInitialized) context.unregisterReceiver(timeoutBroadcastReceiver)

        for (request in requests.values) {
            sendReply(request, Status(CommonStatusCodes.TIMEOUT))
        }

        requests.clear()
    }

    @TargetApi(19)
    private inner class SmsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val messageBodyBuilder = StringBuilder()
                var senderPhoneNumber: String? = null
                for (message in messages) {
                    messageBodyBuilder.append(message.messageBody)
                    senderPhoneNumber = message.originatingAddress
                }
                try {
                    handleIncomingSmsMessage(senderPhoneNumber, messageBodyBuilder.toString())
                } catch (e: Exception) {
                    Log.w(TAG, "Error handling incoming SMS", e)
                }
            }
        }
    }

    private inner class TimeoutReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val requestId = intent.getIntExtra(EXTRA_REQUEST_ID, -1)
            if (requestId != -1) {
                handleTimeout(requestId)
            }
        }
    }

    @TargetApi(19)
    fun getHashString(packageName: String): String {
        val signature =
            context.packageManager.getSignatures(packageName).firstOrNull()?.toCharsString() ?: throw RuntimeException("No signature found for $packageName")
        val appInfo = "$packageName $signature"
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(appInfo.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(messageDigest.digest(), Base64.NO_PADDING or Base64.NO_WRAP).substring(0, 11)
    }

    private fun anyOtherPackageHasHashString(packageName: String, hashString: String): Boolean {
        val collision = context.packageManager.getInstalledPackages(0)
            .firstOrNull { it.packageName != packageName && getHashString(it.packageName) == hashString } ?: return false

        Log.w(TAG, "Hash string collision between $packageName and ${collision.packageName} (both are $hashString)")
        return true
    }

    private fun isPhoneNumberInContacts(context: Context, phoneNumber: String): Boolean {
        fun normalizePhoneNumber(input: String): String {
            var output = ""
            if (!TextUtils.isEmpty(input)) {
                // only keep digits
                val temp = input.replace("[^0-9]".toRegex(), "")
                // trim leading zeroes
                output = temp.replaceFirst("^0*".toRegex(), "")
            }
            return output
        }

        val normalizePhoneNumber = normalizePhoneNumber(phoneNumber)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(Phone.CONTENT_URI, arrayOf(Phone.NUMBER), null, null, null) ?: return false
            while (cursor.moveToNext()) {
                val addressIndex = cursor.getColumnIndex(Phone.NUMBER)
                val contactPhoneNumber = normalizePhoneNumber(cursor.getString(addressIndex))
                if (!TextUtils.isEmpty(normalizePhoneNumber) && !TextUtils.isEmpty(contactPhoneNumber) && normalizePhoneNumber == contactPhoneNumber) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
        } finally {
            cursor?.close()
        }
        return false
    }

    private val activePermissionRequestLock = Mutex()
    private var activePermissionRequest: Deferred<Boolean>? = null

    private suspend fun ensurePermission(permissions: Array<String>): Boolean {
        if (SDK_INT < 23)
            return true

        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PERMISSION_GRANTED })
            return true

        val (completable, deferred) = activePermissionRequestLock.withLock {
            if (activePermissionRequest == null) {
                val completable = CompletableDeferred<Boolean>()
                activePermissionRequest = completable
                completable to activePermissionRequest!!
            } else {
                null to activePermissionRequest!!
            }
        }
        if (completable != null) {
            val intent = Intent(context, AskPermissionActivity::class.java)
            intent.putExtra(EXTRA_MESSENGER, Messenger(object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (msg.what == Activity.RESULT_OK) {
                        val grantResults = msg.data?.getIntArray(EXTRA_GRANT_RESULTS) ?: IntArray(0)
                        completable.complete(grantResults.size == permissions.size && grantResults.all { it == PERMISSION_GRANTED })
                    } else {
                        completable.complete(false)
                    }
                }
            }))
            intent.putExtra(EXTRA_PERMISSIONS, permissions)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
        return deferred.await()
    }
}