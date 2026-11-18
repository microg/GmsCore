/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending.installer

import android.app.Notification
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresApi
import com.android.vending.buildRequestHeaders
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
import org.microg.vending.ui.notifyInstallProgress
import kotlin.random.Random

@RequiresApi(21)
internal class EnterpriseAppInstaller(
    private val context: Context,
    private val auth: AuthData,
    private val apps: MutableMap<EnterpriseApp, AppState>,
    private val onProgressNotification: (sessionId: Int, notification: Notification, isDependency: Boolean) -> Unit
) {

    suspend fun install(app: EnterpriseApp, isUpdate: Boolean) {

        val previousState = apps[app]!!
        apps[app] = Pending

        val client = HttpClient()

        // Purchase app (only needs to be done once, in theory – behaviour seems flaky)
        // Ignore failures
        runCatching {
            if (!app.isRequired) {
                val parameters = mapOf(
                    "ot" to "1",
                    "doc" to app.packageName,
                    "vc" to app.versionCode.toString()
                )
                client.post(
                    url = URL_PURCHASE,
                    headers = buildRequestHeaders(
                        auth.authToken,
                        auth.gsfId.toLong(16)
                    ),
                    params = parameters,
                    adapter = GoogleApiResponse.ADAPTER
                )
            }
        }.onFailure { Log.i(TAG, "couldn't purchase ${app.packageName}: ${it.message}") }
            .onSuccess { Log.d(TAG, "purchased ${app.packageName} successfully") }

        // Install dependencies (different package name → needs to happen in a separate transaction)
        if (android.os.Build.VERSION.SDK_INT >= 26) for (dependency in app.dependencies) {

            val installedDetails = context.packageManager.getSharedLibraries(0)
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
                    context,
                    dependency.packageName,
                    dependency.versionCode!!.toLong(),
                    auth
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
                context.installPackagesFromNetwork(
                    packageName = dependency.packageName,
                    components = downloadUrls.getOrThrow(),
                    httpClient = client,
                    isUpdate = false // static libraries may never be installed as updates
                ) { session, progress ->

                    // Android rate limits notification updates by some vague rule of "not too many in less than one second"
                    if (progress !is Downloading || lastNotification + NOTIFICATION_INTERVAL < System.currentTimeMillis()) {
                        context.notifyInstallProgress(app.displayName, session, progress, isDependency = true)?.let {
                            onProgressNotification(session, it, /* isDependency */ true)
                        }
                        lastNotification = System.currentTimeMillis()
                    }
                }
            }.onFailure { exception ->
                Log.w(TAG, "Installation from network unsuccessful.", exception)
                context.notifyInstallProgress(app.displayName, sessionId = Random.nextInt(), progress = InstallError("unused"), false)
                apps[app] = previousState
                return
            }
        }

        // Get download links for requested package
        val downloadUrls = runCatching {

            client.requestDownloadUrls(
                context,
                app.packageName,
                app.versionCode!!.toLong(),
                auth,
                deliveryToken = app.deliveryToken
            ) }

        if (downloadUrls.isFailure) {
            Log.w(TAG, "Failed to request download URLs: ${downloadUrls.exceptionOrNull()!!.message}")
            apps[app] = previousState
            return
        }

        runCatching {

            var lastNotification = 0L
            context.installPackagesFromNetwork(
                packageName = app.packageName,
                components = downloadUrls.getOrThrow(),
                httpClient = client,
                isUpdate = isUpdate
            ) { session, progress ->


                // Android rate limits notification updates by some vague rule of "not too many in less than one second"
                if (progress !is Downloading || lastNotification + NOTIFICATION_INTERVAL < System.currentTimeMillis()) {
                    context.notifyInstallProgress(app.displayName, session, progress)?.let {
                        onProgressNotification(session, it, /* isDependency */ false)
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

    companion object {
        private const val TAG = "GmsEnterpriseInstaller"

        private const val NOTIFICATION_INTERVAL = 500
    }
}
