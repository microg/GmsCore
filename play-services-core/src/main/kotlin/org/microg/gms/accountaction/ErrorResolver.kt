package org.microg.gms.accountaction

import android.accounts.Account
import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.microg.gms.common.Constants
import org.microg.gms.cryptauth.isLockscreenConfigured
import org.microg.gms.cryptauth.syncCryptAuthKeys
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.GcmPrefs
import org.microg.gms.settings.SettingsContract
import java.io.IOException


/**
 * High-level resolution: tell server that user has configured a lock screen
 */
const val DEVICE_MANAGEMENT_SCREENLOCK_REQUIRED = "DeviceManagementScreenlockRequired"

/**
 * Indicates that the user is using an enterprise account that is set up to use Advanced
 * device management features, for which it is required to install a device manager.
 * This is not supported by microG.
 */
const val DEVICE_MANAGEMENT_REQUIRED = "DeviceManagementRequired"

/**
 * Indicates that the user is using an enterprise account that is set up to use Advanced
 * device management features, for which it is required to install a device manager,
 * and that the device also needs manual admin approval.
 * This is not supported by microG.
 */
const val DEVICE_MANAGEMENT_ADMIN_PENDING_APPROVAL = "DeviceManagementAdminPendingApproval"

const val TAG = "GmsAccountErrorResolve"

/**
 * @return `null` if it is unknown how to resolve the problem, an
 * appropriate `Resolution` otherwise
 */
fun Context.resolveAuthErrorMessage(s: String): Resolution? = if (s.startsWith("Error=")) {
    resolveAuthErrorMessage(s.drop("Error=".length))
} else when (s) {
    DEVICE_MANAGEMENT_SCREENLOCK_REQUIRED -> {

        val actions = mutableSetOf<UserAction>()



        val settingsProjection = arrayOf(
            SettingsContract.CheckIn.ENABLED,
            SettingsContract.CheckIn.LAST_CHECK_IN
        )
        SettingsContract.getSettings(this, SettingsContract.CheckIn.getContentUri(this), settingsProjection) { cursor ->
            val checkInEnabled = cursor.getInt(0) != 0
            val lastCheckIn = cursor.getLong(1)

            if (lastCheckIn <= 0 && !checkInEnabled) {
                actions += UserAction.ENABLE_CHECKIN
            }
        }

        val gcmPrefs = GcmPrefs.get(this)
        if (!gcmPrefs.isEnabled) {
            actions += UserAction.ENABLE_GCM
        }

        val gcmDatabaseEntry = GcmDatabase(this).use {
            it.getApp(Constants.GMS_PACKAGE_NAME)
        }
        if (gcmDatabaseEntry != null &&
            !gcmDatabaseEntry.allowRegister ||
            gcmDatabaseEntry == null &&
            gcmPrefs.confirmNewApps
        ) {
            actions += UserAction.ALLOW_MICROG_GCM
        }

        if (!isLockscreenConfigured()) {
            actions += UserAction.ENABLE_LOCKSCREEN
        }

        if (actions.isEmpty()) {
            CryptAuthSyncKeys
        } else {
            UserIntervention(actions)
        }
    }

    DEVICE_MANAGEMENT_ADMIN_PENDING_APPROVAL, DEVICE_MANAGEMENT_REQUIRED ->
        NoResolution(NoResolutionReason.ADVANCED_DEVICE_MANAGEMENT_NOT_SUPPORTED)
    else -> null
}.also { Log.d(TAG, "Error was: $s. Diagnosis: $it.") }


fun <T> Resolution.initiateFromBackgroundBlocking(context: Context, account: Account, retryFunction: RetryFunction<T>): T? {
    when (this) {
        CryptAuthSyncKeys -> {
            Log.d(TAG, "Resolving account error by performing cryptauth sync keys call.")
            runBlocking {
                syncCryptAuthKeys(context, account)
            }
            return retryFunction.run()
        }
        is NoResolution -> {
            Log.w(TAG, "This account cannot be used with microG due to $reason")
            return null
        }
        is UserIntervention -> {
            Log.w(TAG, "User intervention required! You need to ${actions.joinToString(", ")}.")
            return null
        }
    }
}

interface RetryFunction<T> {
    @Throws(IOException::class)
    fun run(): T
}