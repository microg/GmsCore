/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils

import android.app.AlarmManager
import android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_NO_CREATE
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import androidx.core.content.getSystemService
import java.util.UUID

class IntentCacheManager<S : Service, T : Parcelable>(private val context: Context, private val clazz: Class<S>, private val type: Int) {
    private val lock = Any()
    private lateinit var content: ArrayList<T>
    private lateinit var id: String
    private var isReady: Boolean = false
    private val pendingActions: MutableList<() -> Unit> = arrayListOf()

    init {
        val pendingIntent = PendingIntent.getService(context, type, getIntent(), if (SDK_INT >= 31) FLAG_MUTABLE else 0)
        val alarmManager = context.getSystemService<AlarmManager>()
        if (SDK_INT >= 19) {
            alarmManager?.setWindow(ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + TEN_YEARS, -1, pendingIntent)
        } else {
            alarmManager?.set(ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + TEN_YEARS, pendingIntent)
        }
        pendingIntent.send()
    }

    private fun getIntent() = Intent(context, clazz).apply {
        action = ACTION
        putExtra(EXTRA_IS_CACHE, true)
        putExtra(EXTRA_CACHE_TYPE, this@IntentCacheManager.type)
    }

    fun add(entry: T, check: (T) -> Boolean = { false }) = runIfReady {
        val iterator = content.iterator()
        while (iterator.hasNext()) {
            if (check(iterator.next())) {
                iterator.remove()
            }
        }
        content.add(entry)
        updateIntent()
    }

    fun remove(entry: T) = runIfReady {
        if (content.remove(entry)) updateIntent()
    }

    fun removeIf(check: (T) -> Boolean) = runIfReady {
        var removed = false
        val iterator = content.iterator()
        while (iterator.hasNext()) {
            if (check(iterator.next())) {
                iterator.remove()
                removed = true
            }
        }
        if (removed) updateIntent()
    }

    fun clear() = runIfReady {
        content.clear()
        updateIntent()
    }

    fun getId(): String? = if (this::id.isInitialized) id else null

    fun getEntries(): List<T> = if (this::content.isInitialized) content else emptyList()

    fun processIntent(intent: Intent) {
        if (isCache(intent) && getType(intent) == type) {
            synchronized(lock) {
                content = intent.getParcelableArrayListExtra(EXTRA_DATA) ?: arrayListOf()
                id = intent.getStringExtra(EXTRA_ID) ?: UUID.randomUUID().toString()
                if (!intent.hasExtra(EXTRA_ID)) {
                    Log.d(TAG, "Created new intent cache with id $id")
                } else if (intent.hasExtra(EXTRA_DATA)) {
                    Log.d(TAG, "Recovered data from intent cache with id $id")
                }
                pendingActions.forEach { it() }
                pendingActions.clear()
                isReady = true
                updateIntent()
            }
        }
    }

    private fun runIfReady(action: () -> Unit) {
        synchronized(lock) {
            if (isReady) {
                action()
            } else {
                pendingActions.add(action)
            }
        }
    }

    private fun updateIntent() {
        synchronized(lock) {
            if (isReady) {
                val intent = getIntent().apply {
                    putExtra(EXTRA_ID, id)
                    putParcelableArrayListExtra(EXTRA_DATA, content)
                }
                val pendingIntent = PendingIntent.getService(context, type, intent, FLAG_NO_CREATE or FLAG_UPDATE_CURRENT or if (SDK_INT >= 31) FLAG_MUTABLE else 0)
                if (pendingIntent == null) {
                    Log.w(TAG, "Failed to update existing pending intent, will likely have a loss of information")
                }
            }
        }
    }

    companion object {
        private const val TAG = "IntentCacheManager"
        private const val TEN_YEARS = 315360000000L
        private const val ACTION = "org.microg.gms.ACTION_INTENT_CACHE_MANAGER"
        private const val EXTRA_IS_CACHE = "org.microg.gms.IntentCacheManager.is_cache"
        private const val EXTRA_CACHE_TYPE = "org.microg.gms.IntentCacheManager.cache_type"
        private const val EXTRA_ID = "org.microg.gms.IntentCacheManager.id"
        private const val EXTRA_DATA = "org.microg.gms.IntentCacheManager.data"

        inline fun<reified S: Service, T: Parcelable> create(context: Context, type: Int) = IntentCacheManager<S, T>(context, S::class.java, type)

        fun isCache(intent: Intent): Boolean = try {
            intent.getBooleanExtra(EXTRA_IS_CACHE, false)
        } catch (e: Exception) {
            false
        }

        fun getType(intent: Intent): Int {
            val ret = intent.getIntExtra(EXTRA_CACHE_TYPE, -1)
            if (ret == -1) throw IllegalArgumentException()
            return ret
        }
    }
}