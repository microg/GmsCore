/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.gcm

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.util.Log
import org.microg.gms.common.ForegroundServiceContext
import java.util.concurrent.atomic.AtomicInteger

abstract class ReceiverService(private val tag: String): Service() {
    @Volatile
    private var handlerThread: HandlerThread? = null

    @Volatile
    private var handler: Handler? = null
    private val atomicFlag = AtomicInteger(0)

    private fun release() {
        if (atomicFlag.decrementAndGet() == 0) {
            Log.d(tag, "release: ")
            stopSelf()
        }
    }

    override fun onCreate() {
        if (!allowed()) {
            Log.d(tag, "onCreate: not allowed")
            return
        }
        handlerThread = HandlerThread(tag)
        handlerThread?.start()
        handler = object : Handler(handlerThread?.looper!!) {
            override fun handleMessage(msg: Message) {
                val intent = msg.obj as Intent
                try {
                    receiver(intent)
                } catch (e: Exception) {
                    Log.w(tag, "handleMessage: ", e)
                } finally {
                    release()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(tag, "onDestroy: ")
        handler?.removeCallbacksAndMessages(null)
        handlerThread?.quit()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ForegroundServiceContext.completeForegroundService(this, intent, tag)
        if (!allowed()) {
            Log.d(tag, "onStartCommand: not allowed")
            stopSelf()
            return START_NOT_STICKY
        }
        Log.d(tag, "onStartCommand: start ")
        atomicFlag.incrementAndGet()
        val messageObtain = Message.obtain()
        messageObtain.obj = intent
        handler?.sendMessage(messageObtain)
        return START_NOT_STICKY
    }

    abstract fun allowed(): Boolean
    abstract fun receiver(intent: Intent)
}