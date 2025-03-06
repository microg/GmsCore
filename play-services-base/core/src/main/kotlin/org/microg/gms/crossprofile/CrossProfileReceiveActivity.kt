package org.microg.gms.crossprofile

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import org.microg.gms.settings.SettingsContract.CROSS_PROFILE_PERMISSION
import org.microg.gms.settings.SettingsContract.CROSS_PROFILE_SHARED_PREFERENCES_NAME
import androidx.core.content.edit

@RequiresApi(Build.VERSION_CODES.R)
class CrossProfileReceiveActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, intent.data.toString())

        val data = intent.data
        if (data == null) {
            Log.w(TAG, "expected to receive data, but intent did not contain any.")
            finish()
            return
        }

        contentResolver.takePersistableUriPermission(data, 0)

        val preferences = getSharedPreferences(CROSS_PROFILE_SHARED_PREFERENCES_NAME, MODE_PRIVATE)
        if (preferences.contains(CROSS_PROFILE_PERMISSION)) {
            Log.v(TAG, "using work profile stored URI")
            preferences.edit(commit = true) { putString(CROSS_PROFILE_PERMISSION, data.toString()) }
        }

        finish()
    }

    companion object {
        const val TAG = "GmsCrossProfileReceive"
    }
}