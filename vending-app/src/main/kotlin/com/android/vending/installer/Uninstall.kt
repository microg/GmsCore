/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.annotation.RequiresApi
import androidx.core.app.PendingIntentCompat
import kotlinx.coroutines.CompletableDeferred

@RequiresApi(21)
suspend fun Context.uninstallPackage(packageName: String) {
    val installer = packageManager.packageInstaller
    val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
    val session = installer.createSession(sessionParams)

    val deferred = CompletableDeferred<Unit>()

    SessionResultReceiver.pendingSessions[session] = SessionResultReceiver.OnResult(
        onSuccess = { deferred.complete(Unit) },
        onFailure = { message -> deferred.completeExceptionally(RuntimeException(message)) }
    )

    installer.uninstall(
        packageName, PendingIntentCompat.getBroadcast(
            this, session, Intent(this, SessionResultReceiver::class.java).apply {
                // for an unknown reason, the session ID is not added to the response automatically :(
                putExtra(PackageInstaller.EXTRA_SESSION_ID, session)
            },
            PendingIntent.FLAG_UPDATE_CURRENT, true
        )!!.intentSender
    )

    deferred.await()

}
