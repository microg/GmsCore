/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.gcm

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.legacy.content.WakefulBroadcastReceiver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import org.microg.gms.checkin.CheckinPreferences
import org.microg.gms.checkin.CheckinService
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.ForegroundServiceContext
import org.microg.gms.common.PackageUtils
import org.microg.gms.gcm.GcmConstants.*
import org.microg.gms.ui.AskPushPermission
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "GmsGcmRegister"

private suspend fun ensureCheckinIsUpToDate(context: Context) {
    if (!CheckinPreferences.isEnabled(context)) throw RuntimeException("Checkin disabled")
    val lastCheckin = LastCheckinInfo.read(context).lastCheckin
    if (lastCheckin < System.currentTimeMillis() - CheckinService.MAX_VALID_CHECKIN_AGE) {
        val resultData: Bundle = suspendCoroutine { continuation ->
            val intent = Intent(context, CheckinService::class.java)
            val continued = AtomicBoolean(false)
            intent.putExtra(CheckinService.EXTRA_RESULT_RECEIVER, object : ResultReceiver(null) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    if (continued.compareAndSet(false, true)) continuation.resume(resultData ?: Bundle.EMPTY)
                }
            })
            ForegroundServiceContext(context).startService(intent)
            Handler().postDelayed({
                if (continued.compareAndSet(false, true)) continuation.resume(Bundle.EMPTY)
            }, 10000L)
        }
        if (resultData.getLong(CheckinService.EXTRA_NEW_CHECKIN_TIME, 0L) + lastCheckin == 0L) {
            throw RuntimeException("No checkin available")
        }
    }
}

private suspend fun ensureAppRegistrationAllowed(context: Context, database: GcmDatabase, packageName: String) {
    if (!GcmPrefs.get(context).isEnabled) throw RuntimeException("GCM disabled")
    val app = database.getApp(packageName)
    if (app == null && GcmPrefs.get(context).confirmNewApps) {
        val accepted: Boolean = suspendCoroutine { continuation ->
            val i = Intent(context, AskPushPermission::class.java)
            i.putExtra(AskPushPermission.EXTRA_REQUESTED_PACKAGE, packageName)
            i.putExtra(AskPushPermission.EXTRA_RESULT_RECEIVER, object : ResultReceiver(null) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    continuation.resume(resultCode == Activity.RESULT_OK)
                }
            })
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            i.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
            context.startActivity(i)
        }
        if (!accepted) {
            throw RuntimeException("Push permission not granted to $packageName")
        }
    } else if (app?.allowRegister == false) {
        throw RuntimeException("Push permission not granted to $packageName")
    }
}

suspend fun completeRegisterRequest(context: Context, database: GcmDatabase, request: RegisterRequest, requestId: String? = null): Bundle = suspendCoroutine { continuation ->
    PushRegisterManager.completeRegisterRequest(context, database, requestId, request) { continuation.resume(it) }
}

private val Intent.requestId: String?
    get() {
        val kidString = getStringExtra(GcmConstants.EXTRA_KID) ?: return null
        if (kidString.startsWith("|")) {
            val kid = kidString.split("\\|".toRegex()).toTypedArray()
            if (kid.size >= 3 && "ID" == kid[1]) {
                return kid[2]
            }
        }
        return null
    }

private val Intent.app: PendingIntent?
    get() = getParcelableExtra(EXTRA_APP)

private val Intent.appPackageName: String?
    get() = PackageUtils.packageFromPendingIntent(app)

class PushRegisterService : LifecycleService() {
    private lateinit var database: GcmDatabase
    override fun onCreate() {
        super.onCreate()
        database = GcmDatabase(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            WakefulBroadcastReceiver.completeWakefulIntent(intent)
            Log.d(TAG, "onStartCommand: $intent")
            lifecycleScope.launchWhenStarted {
                handleIntent(intent)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun handleIntent(intent: Intent) {
        try {
            ensureCheckinIsUpToDate(this)
            if (ACTION_C2DM_UNREGISTER == intent.action || ACTION_C2DM_REGISTER == intent.action && "1" == intent.getStringExtra(EXTRA_DELETE)) {
                unregister(intent)
            } else if (ACTION_C2DM_REGISTER == intent.action) {
                register(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
            replyNotAvailable(intent)
        }
    }

    private fun replyNotAvailable(intent: Intent) {
        val outIntent = Intent(ACTION_C2DM_REGISTRATION)
        outIntent.putExtra(EXTRA_ERROR, PushRegisterManager.attachRequestId(ERROR_SERVICE_NOT_AVAILABLE, intent.requestId))
        sendReply(intent, intent.appPackageName, outIntent)
    }

    private suspend fun register(intent: Intent) {
        val packageName = intent.appPackageName ?: throw RuntimeException("No package provided")
        ensureAppRegistrationAllowed(this, database, packageName)
        Log.d(TAG, "register[req]: " + intent.toString() + " extras=" + intent!!.extras)
        val bundle = completeRegisterRequest(this, database,
                RegisterRequest()
                        .build(this)
                        .sender(intent.getStringExtra(EXTRA_SENDER))
                        .checkin(LastCheckinInfo.read(this))
                        .app(packageName)
                        .extraParams(intent.extras))

        val outIntent = Intent(ACTION_C2DM_REGISTRATION)
        outIntent.putExtras(bundle)
        Log.d(TAG, "register[res]: " + outIntent.toString() + " extras=" + outIntent.extras)
        sendReply(intent, packageName, outIntent)
    }

    private suspend fun unregister(intent: Intent) {
        val packageName = intent.appPackageName ?: throw RuntimeException("No package provided")
        Log.d(TAG, "unregister[req]: " + intent.toString() + " extras=" + intent.extras)
        val bundle = completeRegisterRequest(this, database, RegisterRequest()
                .build(this)
                .sender(intent.getStringExtra(EXTRA_SENDER))
                .checkin(LastCheckinInfo.read(this))
                .app(packageName)
                .extraParams(intent.extras)
        )
        val outIntent = Intent(ACTION_C2DM_REGISTRATION)
        outIntent.putExtras(bundle)
        Log.d(TAG, "unregister[res]: " + outIntent.toString() + " extras=" + outIntent.extras)
        sendReply(intent, packageName, outIntent)
    }

    private fun sendReply(intent: Intent, packageName: String?, outIntent: Intent) {
        if (sendReplyToMessenger(intent, outIntent)) return
        outIntent.setPackage(packageName)
        sendOrderedBroadcast(outIntent, null)
    }

    private fun sendReplyToMessenger(intent: Intent, outIntent: Intent): Boolean {
        try {
            val messenger = intent.getParcelableExtra<Messenger>(EXTRA_MESSENGER) ?: return false
            val message = Message.obtain()
            message.obj = outIntent
            messenger.send(message)
            return true
        } catch (e: Exception) {
            Log.w(TAG, e)
            return false
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind: $intent")
        super.onBind(intent)
        if (ACTION_C2DM_REGISTER == intent.action) {
            val messenger = Messenger(PushRegisterHandler(this, database, lifecycle))
            return messenger.binder
        }
        return null
    }
}

internal class PushRegisterHandler(private val context: Context, private val database: GcmDatabase, override val lifecycle: Lifecycle) : Handler(), LifecycleOwner {

    private var callingUid = 0
    override fun sendMessageAtTime(msg: Message, uptimeMillis: Long): Boolean {
        callingUid = Binder.getCallingUid()
        return super.sendMessageAtTime(msg, uptimeMillis)
    }

    private fun sendReplyViaMessage(what: Int, id: Int, replyTo: Messenger, messageData: Bundle) {
        val response = Message.obtain()
        response.what = what
        response.arg1 = id
        response.data = messageData
        try {
            replyTo.send(response)
        } catch (e: RemoteException) {
            Log.w(TAG, e)
        }
    }

    private fun sendReplyViaIntent(outIntent: Intent, replyTo: Messenger) {
        val message = Message.obtain()
        message.obj = outIntent
        try {
            replyTo.send(message)
        } catch (e: RemoteException) {
            Log.w(TAG, e)
        }
    }

    private fun sendReply(what: Int, id: Int, replyTo: Messenger, data: Bundle, oneWay: Boolean) {
        if (what == 0) {
            val outIntent = Intent(ACTION_C2DM_REGISTRATION)
            outIntent.putExtras(data)
            sendReplyViaIntent(outIntent, replyTo)
            return
        }
        val messageData = Bundle()
        messageData.putBundle("data", data)
        sendReplyViaMessage(what, id, replyTo, messageData)
    }

    private fun replyError(what: Int, id: Int, replyTo: Messenger, errorMessage: String, oneWay: Boolean) {
        val bundle = Bundle()
        bundle.putString(EXTRA_ERROR, errorMessage)
        sendReply(what, id, replyTo, bundle, oneWay)
    }

    private fun replyNotAvailable(what: Int, id: Int, replyTo: Messenger) {
        replyError(what, id, replyTo, ERROR_SERVICE_NOT_AVAILABLE, false)
    }

    private val selfAuthIntent: PendingIntent
        private get() {
            val intent = Intent()
            intent.setPackage("com.google.example.invalidpackage")
            return PendingIntent.getBroadcast(context, 0, intent, 0)
        }

    override fun handleMessage(msg: Message) {
        var msg = msg
        val obj = msg.obj
        if (msg.what == 0) {
            if (obj is Intent) {
                val nuMsg = Message.obtain()
                nuMsg.what = msg.what
                nuMsg.arg1 = 0
                nuMsg.replyTo = null
                val packageName = obj.appPackageName
                val data = Bundle()
                data.putBoolean("oneWay", false)
                data.putString("pkg", packageName)
                data.putBundle("data", msg.data)
                nuMsg.data = data
                msg = nuMsg
            } else {
                return
            }
        }
        val what = msg.what
        val id = msg.arg1
        val replyTo = msg.replyTo
        if (replyTo == null) {
            Log.w(TAG, "replyTo is null")
            return
        }
        val data = msg.data
        val packageName = data.getString("pkg") ?: return
        val subdata = data.getBundle("data")
        try {
            PackageUtils.checkPackageUid(context, packageName, callingUid)
        } catch (e: SecurityException) {
            Log.w(TAG, e)
            return
        }
        Log.d(TAG, "handleMessage: package=$packageName what=$what id=$id")
        val oneWay = data.getBoolean("oneWay", false)
        when (what) {
            0, 1 -> {
                lifecycleScope.launchWhenStarted {
                    try {
                        val sender = subdata?.getString("sender")
                        val delete = subdata?.get("delete") != null
                        ensureCheckinIsUpToDate(context)
                        if (!delete) ensureAppRegistrationAllowed(context, database, packageName)
                        val bundle = completeRegisterRequest(context, database,
                                RegisterRequest()
                                        .build(context)
                                        .sender(sender)
                                        .checkin(LastCheckinInfo.read(context))
                                        .app(packageName)
                                        .delete(delete)
                                        .extraParams(subdata))
                        sendReply(what, id, replyTo, bundle, oneWay)
                    } catch (e: Exception) {
                        Log.w(TAG, e)
                        replyNotAvailable(what, id, replyTo)
                    }
                }
            }
            2 -> {
                val messageId = subdata!!.getString("google.message_id")
                Log.d(TAG, "Ack $messageId for $packageName")
                val i = Intent(context, McsService::class.java)
                i.action = McsConstants.ACTION_ACK
                i.putExtra(EXTRA_APP, selfAuthIntent)
                ForegroundServiceContext(context).startService(i)
            }
            else -> {
                val bundle = Bundle()
                bundle.putBoolean("unsupported", true)
                sendReplyViaMessage(what, id, replyTo, bundle)
                return
            }
        }
        if (oneWay) {
            val bundle = Bundle()
            bundle.putBoolean("ack", true)
            sendReplyViaMessage(what, id, replyTo, bundle)
        }
    }
}

class PushRegisterReceiver : WakefulBroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val intent2 = Intent(context, PushRegisterService::class.java)
        if (intent.extras!!.get("delete") != null) {
            intent2.action = ACTION_C2DM_UNREGISTER
        } else {
            intent2.action = ACTION_C2DM_REGISTER
        }
        intent2.putExtras(intent.extras!!)
        startWakefulService(context, intent2)
    }
}
