package org.microg.gms.rcs

import android.content.Context
import android.util.Log

class AsterismManager(private val context: Context) {
    companion object {
        private const val TAG = "AsterismManager"
        private const val PREF_NAME = "microg_rcs_asterism_prefs"
        private const val KEY_RCS_ACTIVATED = "rcs_activated"
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isRcsActivated(): Boolean {
        val activated = prefs.getBoolean(KEY_RCS_ACTIVATED, false)
        Log.d(TAG, "Checking RCS activation status: $activated")
        return activated
    }

    fun setRcsActivated(activated: Boolean) {
        prefs.edit().putBoolean(KEY_RCS_ACTIVATED, activated).apply()
        Log.d(TAG, "RCS activation status updated to: $activated")
    }

    fun clearConfiguration() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Asterism RCS configuration safely cleared.")
    }
}