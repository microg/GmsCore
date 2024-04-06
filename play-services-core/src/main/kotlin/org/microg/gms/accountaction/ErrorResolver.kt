package org.microg.gms.accountaction

import android.accounts.Account
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.cryptauth.syncCryptAuthKeys

/**
 * @return `null` if it is unknown how to resolve the problem, an
 * appropriate `Resolution` otherwise
 */
fun fromErrorMessage(s: String): Resolution? = if (s.startsWith("Error=")) {
    fromErrorMessage(s.drop("Error=".length))
} else when (s) {
    "DeviceManagementScreenlockRequired" -> {
        if (true) {
            CryptAuthSyncKeys
        } else {
            UserIntervention(
                setOf(
                    UserAction.ENABLE_CHECKIN, UserAction.ENABLE_GCM, UserAction.ENABLE_LOCKSCREEN
                )
            )
        }
    }

    "DeviceManagementRequired" -> NoResolution
    "DeviceManagementAdminPendingApproval" -> NoResolution
    else -> null
}


suspend fun Resolution.initiateBackground(context: Context, account: Account, retryFunction: () -> Any) {
    when (this) {
        CryptAuthSyncKeys -> {
            withContext(Dispatchers.IO) {
                syncCryptAuthKeys(context, account)
            }
            retryFunction()
        }
        NoResolution -> TODO()
        is UserIntervention -> TODO()
    }
}