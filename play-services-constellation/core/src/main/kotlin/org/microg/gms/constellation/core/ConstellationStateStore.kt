@file:RequiresApi(Build.VERSION_CODES.O)

package org.microg.gms.constellation.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import com.squareup.wire.Instant
import okio.ByteString.Companion.toByteString
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.ConsentSource
import org.microg.gms.constellation.core.proto.ConsentVersion
import org.microg.gms.constellation.core.proto.DroidguardToken
import org.microg.gms.constellation.core.proto.ProceedResponse
import org.microg.gms.constellation.core.proto.ServerTimestamp
import org.microg.gms.constellation.core.proto.SyncResponse
import org.microg.gms.constellation.core.proto.VerificationToken

private const val STATE_PREFS_NAME = "constellation_prefs"
private const val TOKEN_PREFS_NAME = "com.google.android.gms.constellation"
private const val KEY_VERIFICATION_TOKENS = "verification_tokens_v1"
private const val KEY_DROIDGUARD_TOKEN = "droidguard_token"
private const val KEY_DROIDGUARD_TOKEN_TTL = "droidguard_token_ttl"
private const val KEY_NEXT_SYNC_TIMESTAMP_MS = "next_sync_timestamp_in_millis"
private const val KEY_PUBLIC_KEY_ACKED = "is_public_key_acked"
private const val KEY_PNVR_NOTICE_CONSENT = "pnvr_notice_consent"
private const val KEY_PNVR_NOTICE_SOURCE = "pnvr_notice_source"
private const val KEY_PNVR_NOTICE_VERSION = "pnvr_notice_version"
private const val KEY_PNVR_NOTICE_UPDATED_AT_MS = "pnvr_notice_updated_at_ms"

object ConstellationStateStore {
    fun loadVerificationTokens(context: Context): List<VerificationToken> {
        val prefs = tokenPrefs(context)
        val serialized = prefs.getString(KEY_VERIFICATION_TOKENS, null) ?: return emptyList()
        return serialized.split(",").mapNotNull { entry ->
            val parts = entry.split("|", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val tokenBytes = runCatching {
                Base64.decode(parts[0], Base64.DEFAULT).toByteString()
            }.getOrNull() ?: return@mapNotNull null
            val expirationMillis = parts[1].toLongOrNull() ?: return@mapNotNull null
            if (expirationMillis <= System.currentTimeMillis()) return@mapNotNull null
            VerificationToken(
                token = tokenBytes,
                expiration_time = Instant.ofEpochMilli(expirationMillis)
            )
        }
    }

    fun storeSyncResponse(context: Context, response: SyncResponse) {
        storeVerificationTokens(context, response.verification_tokens)
        storeDroidGuardToken(context, response.droidguard_token)
        storeNextSyncTime(context, response.next_sync_time)
    }

    fun storeProceedResponse(context: Context, response: ProceedResponse) {
        storeDroidGuardToken(context, response.droidguard_token)
        storeNextSyncTime(context, response.next_sync_time)
    }

    fun loadDroidGuardToken(context: Context): String? {
        val statePrefs = statePrefs(context)
        var token = statePrefs.getString(KEY_DROIDGUARD_TOKEN, null)
        var expiration = statePrefs.getLong(KEY_DROIDGUARD_TOKEN_TTL, 0L)

        if (token.isNullOrBlank() || expiration == 0L) {
            val legacyPrefs = tokenPrefs(context)
            val legacyToken = legacyPrefs.getString(KEY_DROIDGUARD_TOKEN, null)
            val legacyExpiration = legacyPrefs.getLong(KEY_DROIDGUARD_TOKEN_TTL, 0L)
            if (!legacyToken.isNullOrBlank() && legacyExpiration > 0L) {
                statePrefs.edit {
                    putString(KEY_DROIDGUARD_TOKEN, legacyToken)
                    putLong(KEY_DROIDGUARD_TOKEN_TTL, legacyExpiration)
                }
                token = legacyToken
                expiration = legacyExpiration
            }
        }

        if (!token.isNullOrBlank() && expiration > System.currentTimeMillis()) {
            return token
        }

        if (!token.isNullOrBlank() || expiration > 0L) {
            clearDroidGuardToken(context)
        }
        return null
    }

    private fun storeCachedDroidGuardToken(
        context: Context,
        token: String,
        expirationMillis: Long
    ) {
        if (token.isBlank() || expirationMillis <= System.currentTimeMillis()) return
        statePrefs(context).edit {
            putString(KEY_DROIDGUARD_TOKEN, token)
            putLong(KEY_DROIDGUARD_TOKEN_TTL, expirationMillis)
        }
    }

    fun clearDroidGuardToken(context: Context) {
        statePrefs(context).edit {
            remove(KEY_DROIDGUARD_TOKEN)
            remove(KEY_DROIDGUARD_TOKEN_TTL)
        }
        tokenPrefs(context).edit {
            remove(KEY_DROIDGUARD_TOKEN)
            remove(KEY_DROIDGUARD_TOKEN_TTL)
        }
    }

    fun isPublicKeyAcked(context: Context): Boolean {
        return statePrefs(context).getBoolean(KEY_PUBLIC_KEY_ACKED, false)
    }

    @SuppressLint("ApplySharedPref")
    fun setPublicKeyAcked(context: Context, acked: Boolean) {
        statePrefs(context).edit { putBoolean(KEY_PUBLIC_KEY_ACKED, acked) }
    }

    fun loadPnvrNoticeConsent(context: Context): Consent {
        val value =
            statePrefs(context).getInt(KEY_PNVR_NOTICE_CONSENT, Consent.CONSENT_UNKNOWN.value)
        return Consent.fromValue(value) ?: Consent.CONSENT_UNKNOWN
    }

    fun storePnvrNotice(
        context: Context,
        consent: Consent,
        source: ConsentSource,
        version: ConsentVersion
    ) {
        statePrefs(context).edit {
            putInt(KEY_PNVR_NOTICE_CONSENT, consent.value)
            putInt(KEY_PNVR_NOTICE_SOURCE, source.value)
            putInt(KEY_PNVR_NOTICE_VERSION, version.value)
            putLong(KEY_PNVR_NOTICE_UPDATED_AT_MS, System.currentTimeMillis())
        }
    }

    private fun storeVerificationTokens(context: Context, tokens: List<VerificationToken>) {
        if (tokens.isEmpty()) return
        val filtered = tokens.filter {
            (it.expiration_time?.toEpochMilli() ?: 0L) > System.currentTimeMillis()
        }
        if (filtered.isEmpty()) return

        val serialized = filtered.joinToString(",") { token ->
            val encoded = Base64.encodeToString(token.token.toByteArray(), Base64.NO_WRAP)
            val expiration = token.expiration_time?.toEpochMilli() ?: 0L
            "$encoded|$expiration"
        }
        tokenPrefs(context).edit { putString(KEY_VERIFICATION_TOKENS, serialized) }
    }

    private fun storeDroidGuardToken(context: Context, token: DroidguardToken?) {
        val tokenValue = token?.token?.takeIf { it.isNotEmpty() } ?: return
        val expiration = token.ttl?.toEpochMilli() ?: return
        storeCachedDroidGuardToken(context, tokenValue, expiration)
    }

    private fun storeNextSyncTime(context: Context, timestamp: ServerTimestamp?) {
        val nextSyncDelayMillis = timestamp?.let(::nextSyncDelayMillis) ?: return
        statePrefs(context).edit {
            // GMS stores the next sync deadline as an absolute wall-clock timestamp
            putLong(KEY_NEXT_SYNC_TIMESTAMP_MS, System.currentTimeMillis() + nextSyncDelayMillis)
        }
    }

    private fun nextSyncDelayMillis(timestamp: ServerTimestamp): Long {
        val serverMillis = timestamp.timestamp?.toEpochMilli() ?: 0L
        val localMillis = timestamp.now?.toEpochMilli() ?: 0L
        return serverMillis - localMillis
    }

    private fun statePrefs(context: Context) =
        context.getSharedPreferences(STATE_PREFS_NAME, Context.MODE_PRIVATE)

    private fun tokenPrefs(context: Context) =
        context.getSharedPreferences(TOKEN_PREFS_NAME, Context.MODE_PRIVATE)
}
