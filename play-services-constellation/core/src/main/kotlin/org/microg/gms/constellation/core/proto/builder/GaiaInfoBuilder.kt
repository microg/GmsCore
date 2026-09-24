package org.microg.gms.constellation.core.proto.builder

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

private const val TAG = "GaiaInfoBuilder"


@RequiresApi(Build.VERSION_CODES.O)
suspend operator fun GaiaSignals.Companion.invoke(context: Context): GaiaSignals? =
    withContext(Dispatchers.IO) {
        val entries = mutableListOf<GaiaSignalEntry>()
        val accountManager = AccountManager.get(context)

        try {
            val accounts = accountManager.getAccountsByType("com.google")

            for (account in accounts) {
                var id = accountManager.getUserData(account, "GoogleUserId")
                if (id == "") {
                    try {
                        val future = accountManager.getAuthToken(
                            account,
                            "^^_account_id_^^",
                            null,
                            false,
                            null,
                            null
                        )
                        id = future.result?.getString(AccountManager.KEY_AUTHTOKEN)
                        accountManager.setUserData(account, "GoogleUserId", id)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not retrieve Gaia ID for account ${account.name}", e)
                        continue
                    }
                }
                entries.add(
                    GaiaSignalEntry(
                        gaia_id = id,
                        signal_type = GaiaAccountSignalType.GAIA_ACCOUNT_SIGNAL_AUTHENTICATED,
                        timestamp = Instant.now()
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not build Gaia signals", e)
        }
        if (entries.isNotEmpty()) GaiaSignals(gaia_signals = entries) else null
    }

@Suppress("DEPRECATION")
suspend fun GaiaToken.Companion.getList(context: Context): List<GaiaToken> =
    withContext(Dispatchers.IO) {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType("com.google")
        accounts.mapNotNull { account ->
            try {
                val future = accountManager.getAuthToken(
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
                Log.w(TAG, "Could not retrieve Gaia token for account ${account.name}", e)
                null
            }
        }
    }
