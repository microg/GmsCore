package org.microg.gms.crossprofile

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.pm.CrossProfileApps
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.getAuthority

@RequiresApi(Build.VERSION_CODES.R)
class CrossProfileSendActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check that we are primary profile
        val userManager = getSystemService(UserManager::class.java)
        if (userManager.isManagedProfile) {
            Log.w(TAG, "Cross-profile send request was received on work profile!")
            finish()
            return
        }

        // Check prerequisites
        val crossProfileApps = getSystemService(CrossProfileApps::class.java)
        val targetProfiles = crossProfileApps.targetUserProfiles

        if (!crossProfileApps.canInteractAcrossProfiles() || targetProfiles.isEmpty()) {
            Log.w(
                TAG, "received cross-profile request, but cannot answer, as prerequisites are not met: " +
                    "can interact = ${crossProfileApps.canInteractAcrossProfiles()}, " +
                    "#targetProfiles = ${targetProfiles.size}")
            finish()
            return
        }

        // Respond
        Log.d(TAG, "responding to cross-profile request")

        val intent = Intent(this, CrossProfileReceiveActivity::class.java)
        intent.setData("content://${getAuthority(this)}".toUri())
            .addFlags(FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

        crossProfileApps.startActivity(
            intent,
            targetProfiles.first(),
            null // response from receiver is not needed
        )

        finish()
    }

    companion object {
        const val TAG = "GmsCrossProfileSend"
    }
}