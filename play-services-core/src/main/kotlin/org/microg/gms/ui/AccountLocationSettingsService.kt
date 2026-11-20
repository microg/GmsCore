/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.accounts.Account
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.google.android.gms.semanticlocationhistory.db.backup.OdlhBackupService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.microg.gms.location.reporting.ManagedAccountLocationSettings
import org.microg.gms.location.reporting.ManagedAccountLocationSettingsUpdate
import org.microg.gms.location.reporting.fetchManagedAccountLocationSettings
import org.microg.gms.location.reporting.updateManagedAccountLocationSettings
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

private const val MSG_FETCH_SETTINGS = 1
private const val MSG_UPDATE_SETTINGS = 2
private const val TAG = "AccountLocationSettings"
private const val REQUEST_TIMEOUT_MS = 30_000L

private const val DATA_ACCOUNT = "account"
private const val DATA_FORCE_REFRESH = "force_refresh"
private const val DATA_HISTORY_ENABLED = "history_enabled"
private const val DATA_REPORTING_ENABLED = "reporting_enabled"
private const val DATA_SYNCHRONIZE_HISTORY_ENABLED = "synchronize_history_enabled"
private const val DATA_SUCCESS = "success"
private const val DATA_SETTINGS_PRESENT = "settings_present"
private const val DATA_HISTORY_PRESENT = "history_present"
private const val DATA_REPORTING_PRESENT = "reporting_present"
private const val DATA_USER_RESTRICTION_PRESENT = "user_restriction_present"
private const val DATA_USER_RESTRICTION = "user_restriction"

/**
 * Keeps account location settings access in the default GMS process. The settings UI runs in
 * `:ui`, while reporting and backup services run in the default process. Using this service
 * prevents the two processes from maintaining independent SharedPreferences caches.
 */
class AccountLocationSettingsService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messenger = Messenger(Handler(Looper.getMainLooper()) { message ->
        handleMessage(message)
        true
    })

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleMessage(message: Message) {
        val replyTo = message.replyTo ?: return
        val requestData = message.data.apply {
            classLoader = Account::class.java.classLoader
        }
        val requestType = message.what
        serviceScope.launch {
            val responseData = runCatching {
                when (requestType) {
                    MSG_FETCH_SETTINGS -> fetchSettings(requestData)
                    MSG_UPDATE_SETTINGS -> updateSettings(requestData)
                    else -> Bundle.EMPTY
                }
            }.onFailure {
                Log.w(TAG, "Failed to process account location settings request", it)
            }.getOrElse {
                failureResponse(requestType)
            }
            runCatching {
                replyTo.send(Message.obtain().apply {
                    what = requestType
                    data = responseData
                })
            }.onFailure {
                Log.w(TAG, "Failed to return account location settings response", it)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun Bundle.getAccount(): Account? = getParcelable(DATA_ACCOUNT)

    private fun fetchSettings(data: Bundle): Bundle {
        val account = data.getAccount() ?: return settingsResponse(null)
        val settings = runCatching {
            fetchManagedAccountLocationSettings(
                this,
                account,
                forceRefresh = data.getBoolean(DATA_FORCE_REFRESH)
            )
        }.onFailure {
            Log.w(TAG, "Failed to fetch account location settings", it)
        }.getOrNull()
        return settingsResponse(settings)
    }

    private fun updateSettings(data: Bundle): Bundle {
        val account = data.getAccount()
        val result = if (account == null) {
            ManagedAccountLocationSettingsUpdate(false, null)
        } else {
            runCatching {
                updateManagedAccountLocationSettings(
                    this,
                    account,
                    historyEnabled = data.getBoolean(DATA_HISTORY_ENABLED),
                    reportingEnabled = data.getBoolean(DATA_REPORTING_ENABLED),
                    synchronizeHistoryEnabled = data.getBoolean(DATA_SYNCHRONIZE_HISTORY_ENABLED)
                )
            }.onFailure {
                Log.w(TAG, "Failed to update account location settings", it)
            }.getOrElse { ManagedAccountLocationSettingsUpdate(false, null) }
        }
        runCatching {
            OdlhBackupService.scheduleBackup(this)
        }.onFailure {
            Log.w(TAG, "Failed to update location history backup schedule", it)
        }
        return settingsResponse(result.settings).apply {
            putBoolean(DATA_SUCCESS, result.success)
        }
    }

    companion object {
        suspend fun fetchSettings(
            context: Context,
            account: Account,
            forceRefresh: Boolean
        ): ManagedAccountLocationSettings? {
            val response = request(context, Message.obtain().apply {
                what = MSG_FETCH_SETTINGS
                data = Bundle().apply {
                    putParcelable(DATA_ACCOUNT, account)
                    putBoolean(DATA_FORCE_REFRESH, forceRefresh)
                }
            })
            return response?.getSettings()
        }

        suspend fun updateSettings(
            context: Context,
            account: Account,
            historyEnabled: Boolean,
            reportingEnabled: Boolean,
            synchronizeHistoryEnabled: Boolean
        ): ManagedAccountLocationSettingsUpdate {
            val response = request(context, Message.obtain().apply {
                what = MSG_UPDATE_SETTINGS
                data = Bundle().apply {
                    putParcelable(DATA_ACCOUNT, account)
                    putBoolean(DATA_HISTORY_ENABLED, historyEnabled)
                    putBoolean(DATA_REPORTING_ENABLED, reportingEnabled)
                    putBoolean(DATA_SYNCHRONIZE_HISTORY_ENABLED, synchronizeHistoryEnabled)
                }
            })
            return ManagedAccountLocationSettingsUpdate(
                success = response?.getBoolean(DATA_SUCCESS) == true,
                settings = response?.getSettings()
            )
        }

        private suspend fun request(context: Context, message: Message): Bundle? {
            var completed = false
            val response = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                requestFromService(context, message).also { completed = true }
            }
            if (!completed) Log.w(TAG, "Account location settings request timed out")
            return response
        }

        private suspend fun requestFromService(context: Context, message: Message): Bundle? =
            suspendCancellableCoroutine { continuation ->
                val completed = AtomicBoolean(false)
                var bound = false
                lateinit var connection: ServiceConnection

                fun complete(response: Bundle?) {
                    if (!completed.compareAndSet(false, true)) return
                    if (bound) runCatching { context.unbindService(connection) }
                    if (continuation.isActive) continuation.resume(response)
                }

                connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        if (service == null) {
                            complete(null)
                            return
                        }
                        message.replyTo = Messenger(Handler(Looper.getMainLooper()) { response ->
                            complete(response.data)
                            true
                        })
                        runCatching {
                            Messenger(service).send(message)
                        }.onFailure {
                            Log.w(TAG, "Failed to send account location settings request", it)
                            complete(null)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        Log.w(TAG, "Account location settings service disconnected")
                        complete(null)
                    }
                }

                val bindResult = runCatching {
                    context.bindService(
                        Intent(context, AccountLocationSettingsService::class.java),
                        connection,
                        Context.BIND_AUTO_CREATE or Context.BIND_ABOVE_CLIENT
                    )
                }.onFailure {
                    Log.w(TAG, "Failed to bind account location settings service", it)
                }
                bound = bindResult.getOrDefault(false)
                if (!bound) {
                    if (bindResult.isSuccess) {
                        Log.w(TAG, "Account location settings service binding was rejected")
                    }
                    complete(null)
                }

                continuation.invokeOnCancellation {
                    if (completed.compareAndSet(false, true) && bound) {
                        runCatching { context.unbindService(connection) }
                    }
                }
            }
    }
}

private fun failureResponse(requestType: Int): Bundle = settingsResponse(null).apply {
    if (requestType == MSG_UPDATE_SETTINGS) putBoolean(DATA_SUCCESS, false)
}

private fun settingsResponse(settings: ManagedAccountLocationSettings?): Bundle = Bundle().apply {
    putBoolean(DATA_SETTINGS_PRESENT, settings != null)
    if (settings == null) return@apply
    putBoolean(DATA_HISTORY_PRESENT, settings.historyEnabled != null)
    settings.historyEnabled?.let { putBoolean(DATA_HISTORY_ENABLED, it) }
    putBoolean(DATA_REPORTING_PRESENT, settings.reportingEnabled != null)
    settings.reportingEnabled?.let { putBoolean(DATA_REPORTING_ENABLED, it) }
    putBoolean(DATA_USER_RESTRICTION_PRESENT, settings.userRestriction != null)
    settings.userRestriction?.let { putInt(DATA_USER_RESTRICTION, it) }
}

private fun Bundle.getSettings(): ManagedAccountLocationSettings? {
    if (!getBoolean(DATA_SETTINGS_PRESENT)) return null
    return ManagedAccountLocationSettings(
        historyEnabled = getBoolean(DATA_HISTORY_ENABLED).takeIf {
            getBoolean(DATA_HISTORY_PRESENT)
        },
        reportingEnabled = getBoolean(DATA_REPORTING_ENABLED).takeIf {
            getBoolean(DATA_REPORTING_PRESENT)
        },
        userRestriction = getInt(DATA_USER_RESTRICTION).takeIf {
            getBoolean(DATA_USER_RESTRICTION_PRESENT)
        }
    )
}
