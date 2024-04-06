package org.microg.gms.accountaction

import android.accounts.Account
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.microg.gms.cryptauth.syncCryptAuthKeys
import java.io.IOException

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


fun <T> Resolution.initiateFromBackgroundBlocking(context: Context, account: Account, retryFunction: RetryFunction<T>): T? {
    when (this) {
        CryptAuthSyncKeys -> {
            runBlocking {
                syncCryptAuthKeys(context, account)
            }
            return retryFunction.run()
        }
        NoResolution -> TODO()
        is UserIntervention -> TODO()
    }
}

interface RetryFunction<T> {
    @Throws(IOException::class)
    fun run(): T
}