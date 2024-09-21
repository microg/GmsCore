package com.android.vending.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.annotation.RequiresApi
import com.google.android.finsky.splitinstallservice.SplitInstallManager.InstallResultReceiver

class Uninstaller {
}

@RequiresApi(android.os.Build.VERSION_CODES.LOLLIPOP)
fun Context.uninstallPackage(packageName: String) {
    val installer = packageManager.packageInstaller
    val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
    val session = installer.createSession(sessionParams)
    installer.uninstall(
        packageName, PendingIntent.getBroadcast(
            this, session, Intent(this, InstallResultReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        ).intentSender
    )

}