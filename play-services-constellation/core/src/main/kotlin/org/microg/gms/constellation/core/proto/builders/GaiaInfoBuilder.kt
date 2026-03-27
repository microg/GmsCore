
package org.microg.gms.constellation.core.proto.builders

import android.accounts.AccountManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.squareup.wire.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.constellation.core.proto.GaiaAccountSignalType
import org.microg.gms.constellation.core.proto.GaiaSignalEntry
import org.microg.gms.constellation.core.proto.GaiaSignals
import org.microg.gms.constellation.core.proto.GaiaToken
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.absoluteValue

private const val TAG = "GaiaInfoBuilder"


@RequiresApi(Build.VERSION_CODES.O)
operator fun GaiaSignals.Companion.invoke(context: Context): GaiaSignals? {
    val entries = mutableListOf<GaiaSignalEntry>()
    try {
        val accounts = AccountManager.get(context).getAccountsByType("com.google")
        val md = MessageDigest.getInstance("SHA-256")

        for (account in accounts) {
            // Note: Simple implementation, maybe do actual obfuscated Gaia ID retrieval later?
            val hash = md.digest(account.name.toByteArray(Charsets.UTF_8))
            val number =
                hash.take(8).fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }
            val obfuscatedId = try {
                number.absoluteValue.toString()
            } catch (_: Exception) {
                abs(account.name.hashCode().toLong()).toString()
            }

            entries.add(
                GaiaSignalEntry(
                    gaia_id = obfuscatedId,
                    signal_type = GaiaAccountSignalType.GAIA_ACCOUNT_SIGNAL_AUTHENTICATED,
                    timestamp = Instant.ofEpochMilli(System.currentTimeMillis())
                )
            )
        }
    } catch (e: Exception) {
        Log.w(TAG, "Could not build Gaia signals", e)
    }
    return if (entries.isNotEmpty()) GaiaSignals(gaia_signals = entries) else null
}

@Suppress("DEPRECATION")
suspend fun GaiaToken.Companion.getList(context: Context): List<GaiaToken> =
    withContext(Dispatchers.IO) {
        val accounts = AccountManager.get(context).getAccountsByType("com.google")
        accounts.mapNotNull { account ->
            try {
                val future = AccountManager.get(context).getAuthToken(
                    account,
                    "oauth2:https://www.googleapis.com/auth/numberer",
                    null,
                    false,
                    null,
                    null
                )
                val token = future.result?.getString(AccountManager.KEY_AUTHTOKEN)

                if (!token.isNullOrBlank()) GaiaToken(token = token) else null

            } catch (e: Exception) {
                Log.w(TAG, "Could not retrieve Gaia token for an account", e)
                null
            }
        }
    }
