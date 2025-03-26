/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending.installer

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import com.android.vending.buildRequestHeaders
import kotlinx.coroutines.runBlocking
import org.microg.vending.billing.core.AuthData
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_PURCHASE
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.billing.proto.GoogleApiResponse
import org.microg.vending.delivery.requestDownloadUrls
import org.microg.vending.enterprise.AppState
import org.microg.vending.enterprise.CommitingSession
import org.microg.vending.enterprise.Downloading
import org.microg.vending.enterprise.EnterpriseApp
import org.microg.vending.enterprise.InstallError
import org.microg.vending.enterprise.Installed
import org.microg.vending.enterprise.Pending
import org.microg.vending.enterprise.proto.AppInstallPolicy
import org.microg.vending.ui.notifyInstallProgress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

@RequiresApi(android.os.Build.VERSION_CODES.LOLLIPOP)
class InstallService : Service() {

    private val binder = LocalBinder()

    private val runningThreads = AtomicInteger(0)

    /**
     * Note: `isForeground` can be `false` even if the service is actually
     * running in the foreground while installing a dependency, see below.
     * State `false` means that the next notification must be designated as
     * foreground notification.
     */
    private val isForeground = AtomicBoolean(false)

    internal var auth: AuthData? = null
    internal lateinit var apps: MutableMap<EnterpriseApp, AppState>

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun installAsync(app: EnterpriseApp, isUpdate: Boolean) =
        Thread {
            runningThreads.incrementAndGet()
            runBlocking { install(app, isUpdate) }
            if (runningThreads.decrementAndGet() == 0) {
                // Demote ourselves explicitly – notification cannot be removed otherwise
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }.start()

    suspend fun install(app: EnterpriseApp, isUpdate: Boolean) {

        val previousState = apps[app]!!
        apps[app] = Pending

        val client = HttpClient()

        // Purchase app (only needs to be done once, in theory – behaviour seems flaky)
        // Ignore failures
        runCatching {
            if (app.policy != AppInstallPolicy.MANDATORY) {
                val parameters = mapOf(
                    "ot" to "1",
                    "doc" to app.packageName,
                    "vc" to app.versionCode.toString()
                )
                client.post(
                    url = URL_PURCHASE,
                    headers = buildRequestHeaders(
                        auth!!.authToken,
                        auth!!.gsfId.toLong(16)
                    ),
                    params = parameters,
                    adapter = GoogleApiResponse.ADAPTER
                )
            }
        }.onFailure { Log.i(TAG, "couldn't purchase ${app.packageName}: ${it.message}") }
            .onSuccess { Log.d(TAG, "purchased ${app.packageName} successfully") }

        // Install dependencies (different package name → needs to happen in a separate transaction)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) for (dependency in app.dependencies) {

            val installedDetails = packageManager.getSharedLibraries(0)
                .map { it.declaringPackage }
                // multiple different library versions can be installed at the same time
                .filter { it.packageName == dependency.packageName }
                .maxByOrNull { it.versionCode }

            val upToDate = installedDetails?.let {
                it.versionCode >= dependency.versionCode!!
            }

            if (upToDate == true) {
                Log.d(
                    TAG, "not installing ${dependency.packageName} as it is already up to date " +
                        "(need version ${dependency.versionCode}, we have version ${installedDetails.versionCode})")
                continue
            } else if (upToDate == false) {
                Log.d(
                    TAG, "${dependency.packageName} is already installed, but an update is necessary " +
                        "(need version ${dependency.versionCode}, we only have version ${installedDetails.versionCode})")
            }

            val downloadUrls = runCatching {

                client.requestDownloadUrls(
                    dependency.packageName,
                    dependency.versionCode!!.toLong(),
                    auth!!
                    // no delivery token available
                ) }

            if (downloadUrls.isFailure) {
                Log.w(TAG, "Failed to request download URLs for dependency ${dependency.packageName}: ${downloadUrls.exceptionOrNull()!!.message}")
                apps[app] = previousState
                return
            }

            runCatching {

                var lastNotification = 0L
                // This method posts its first notification as soon as the install session is created (i.e. before network interaction)
                installPackagesFromNetwork(
                    packageName = dependency.packageName,
                    components = downloadUrls.getOrThrow(),
                    httpClient = client,
                    isUpdate = false // static libraries may never be installed as updates
                ) { session, progress ->

                    // Android rate limits notification updates by some vague rule of "not too many in less than one second"
                    if (progress !is Downloading || lastNotification + NOTIFICATION_INTERVAL < System.currentTimeMillis()) {
                        notifyInstallProgress(app.displayName, session, progress, isDependency = true)?.let {
                            /* We can tolerate if this notification is removed by the Android platform,
                             * since we would post it again while download is running / discard it ourselves
                             * after download has finished.
                             * On the other hand, we couldn't tolerate Android system to prevent us from
                             * cancelling this notification ourselves, since it has a different ID
                             * (separate session) compared to the main download that happens after all
                             * dependencies are loaded.
                             * Therefore, do _not_ set `isForeground` to `true` here, so that another
                             * notification takes this role soon.
                             */
                            if (!isForeground.get()) {
                                ServiceCompat.startForeground(
                                    this, session, it, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                                )
                            }
                        }
                        lastNotification = System.currentTimeMillis()
                    }
                }
            }.onFailure { exception ->
                Log.w(TAG, "Installation from network unsuccessful.", exception)
                notifyInstallProgress(app.displayName, sessionId = Random.nextInt(), progress = InstallError("unused"), false)
                apps[app] = previousState
                return
            }
        }

        // Get download links for requested package
        val downloadUrls = runCatching {

            client.requestDownloadUrls(
                app.packageName,
                app.versionCode!!.toLong(),
                auth!!,
                deliveryToken = app.deliveryToken
            ) }

        if (downloadUrls.isFailure) {
            Log.w(TAG, "Failed to request download URLs: ${downloadUrls.exceptionOrNull()!!.message}")
            apps[app] = previousState
            return
        }

        runCatching {

            var lastNotification = 0L
            installPackagesFromNetwork(
                packageName = app.packageName,
                components = downloadUrls.getOrThrow(),
                httpClient = client,
                isUpdate = isUpdate
            ) { session, progress ->


                // Android rate limits notification updates by some vague rule of "not too many in less than one second"
                if (progress !is Downloading || lastNotification + NOTIFICATION_INTERVAL < System.currentTimeMillis()) {
                    notifyInstallProgress(app.displayName, session, progress)?.let {
                        if (!isForeground.getAndSet(true)) {
                            ServiceCompat.startForeground(
                                this, session, it, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                            )
                        }
                    }
                    lastNotification = System.currentTimeMillis()
                }

                if (progress is Downloading) apps[app] = progress
                else if (progress is CommitingSession) apps[app] = Pending
            }
        }.onSuccess {
            apps[app] = Installed
        }.onFailure { exception ->
            Log.w(TAG, "Installation from network unsuccessful.", exception)
            apps[app] = previousState
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): InstallService = this@InstallService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    companion object {
        const val TAG = "GmsInstallService"

        const val NOTIFICATION_INTERVAL = 500
    }

}