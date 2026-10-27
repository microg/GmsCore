/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cryptauth

import cryptauthv2.KeyType
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Account bootstrap's counter contract, observed through GetAndAdvanceOtpCounter in GMS 25.26.35:
 * derive the THOTP secret from authzen, then derive a non-negative little-endian counter seed.
 * HKDF is RFC 5869 / SHA-256. This is an interoperability implementation, not a claim of server
 * acceptance. The independent fixtures cover the derivation and durable reservation semantics.
 *
 * The existing account store owns the next counter; no second key database or backup is created.
 */
internal class CryptAuthOtpCounter(private val store: CryptAuthStateStore) {
    /**
     * The caller must already have obtained account-specific consent and refreshed enrollment.
     * Keep assertion creation inside [use] so enrollment cannot rotate/revoke its key meanwhile.
     * Once durably reserved, a counter is consumed even when [use] fails or its response is lost.
     */
    suspend fun <T> useNextCounter(expectedAuthzenHandle: ByteString, use: (CryptAuthState, Long) -> T): T =
        cryptAuthStateMutex.withLock {
            currentCoroutineContext().ensureActive()
            val state = store.read()
            require(state.pendingKeys.isEmpty()) { "CryptAuth enrollment is unconfirmed" }
            val key = requireNotNull(state.keys.singleOrNull { it.name == "authzen" && it.active }) {
                "Missing active CryptAuth authzen key"
            }
            require(key.handle == expectedAuthzenHandle) { "CryptAuth authzen key changed" }
            validateKey(key)
            val current = state.otpCounters[key.handle] ?: initialCounter(key.secret)
            require(current >= 0) { "Invalid CryptAuth counter" }
            val next = if (current == Long.MAX_VALUE) 0L else current + 1
            // The checked, synced file commit must succeed before any assertion uses this counter.
            store.write(state.copy(otpCounters = state.otpCounters + (key.handle to next)))
            currentCoroutineContext().ensureActive()
            use(state, current)
        }

    companion object {
        private val salt = "GoogleOfflineOTP".encodeUtf8().sha256()

        internal fun initialCounter(authzen: ByteString): Long {
            require(authzen.size == 32) { "Bootstrap requires an authzen RAW256 key" }
            val raw = authzen.toByteArray()
            val secret = try { CryptAuthCrypto.hkdf(raw, salt.toByteArray(), "THOTP".toByteArray(Charsets.UTF_8), 32) }
                finally { raw.fill(0) }
            val derived = try { CryptAuthCrypto.hkdf(secret, salt.toByteArray(), "THOTP-Counter".toByteArray(Charsets.UTF_8), 32) }
                finally { secret.fill(0) }
            return try { ByteBuffer.wrap(derived).order(ByteOrder.LITTLE_ENDIAN).long and Long.MAX_VALUE }
                finally { derived.fill(0) }
        }

        private fun validateKey(key: CryptAuthKey) {
            require(key.type == KeyType.RAW256 && key.secret.size == 32 && key.publicKey.size == 0 &&
                key.handle == key.secret.sha256().base64Url().encodeUtf8()) { "Invalid bootstrap counter key" }
        }
    }
}
