/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cryptauth

import android.content.Context
import android.util.Log
import cryptauthv2.SyncKeysResponse
import org.json.JSONObject
import org.microg.gms.auth.cryptauth.CryptAuthKeyRecord
import org.microg.gms.auth.cryptauth.CryptAuthKeyType
import org.microg.gms.fido.sourcedevice.FidoSourceKeyStore
import org.microg.gms.fido.sourcedevice.PendingFidoKey
import org.microg.gms.smartdevice.d2d.AuthZenWire

private const val TAG = "FidoKeyEnrollment"

/** Adds the source-device FIDO account key to the shared CryptAuth enrollment flow. */
internal class FidoKeyEnrollment(
    context: Context,
    private val accountName: String,
) {
    private val keyStore = FidoSourceKeyStore(context)
    private val handles = keyStore.getHandles(accountName, KEY_NAME)
    private var recordPresent = handles.isNotEmpty()
    private var active = keyStore.isActive(accountName, KEY_NAME)
    private var keyActions = emptyList<SyncKeysResponse.SyncSingleKeyResponse.KeyAction>()
    private var requestedKeyType: CryptAuthKeyType? = null
    private var pendingKey: PendingFidoKey? = null
    private var activeAfterEnrollment = recordPresent && active
    private var createdKeyActive = false

    var creationRequested: Boolean = false
        private set

    val isUsable: Boolean
        get() = recordPresent && active

    val willBeUsable: Boolean
        get() = activeAfterEnrollment

    fun syncRequest(): CryptAuthSyncKeyRequest {
        return CryptAuthSyncKeyRequest(
            name = KEY_NAME,
            handles = handles,
        )
    }

    fun applySyncResponse(response: JSONObject?) {
        val keyCreation = parseKeyCreation(response?.opt("keyCreation"))
        creationRequested = keyCreation != null &&
            keyCreation != SyncKeysResponse.SyncSingleKeyResponse.KeyCreation.NONE
        requestedKeyType = parseCryptAuthKeyType(response?.opt("keyType"))
            ?.takeIf { it == CryptAuthKeyType.P256 }
        val actions = response?.optJSONArray("keyActions")
        keyActions = if (actions == null) {
            emptyList()
        } else {
            (0 until actions.length()).mapNotNull { parseKeyAction(actions.opt(it)) }
        }
        val existingActiveAfterSync = handles.indices.any { index ->
            when (keyActions.getOrNull(index)) {
                SyncKeysResponse.SyncSingleKeyResponse.KeyAction.ACTIVATE -> true
                SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DEACTIVATE,
                SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DELETE -> false
                else -> keyStore.isActive(accountName, KEY_NAME, handles[index])
            }
        }
        if (!creationRequested) applyKeyActions()
        createdKeyActive = keyCreation == SyncKeysResponse.SyncSingleKeyResponse.KeyCreation.ACTIVE
        activeAfterEnrollment = existingActiveAfterSync || createdKeyActive
    }

    private fun applyKeyActions() {
        handles.forEachIndexed { index, handle ->
            when (keyActions.getOrNull(index)) {
                SyncKeysResponse.SyncSingleKeyResponse.KeyAction.ACTIVATE ->
                    keyStore.setActive(accountName, KEY_NAME, handle, true)
                SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DEACTIVATE ->
                    keyStore.setActive(accountName, KEY_NAME, handle, false)
                SyncKeysResponse.SyncSingleKeyResponse.KeyAction.DELETE ->
                    keyStore.delete(accountName, KEY_NAME, handle)
                else -> Unit
            }
        }
        recordPresent = keyStore.hasRecord(accountName, KEY_NAME)
        active = keyStore.isActive(accountName, KEY_NAME)
    }

    fun hasSupportedCreationType(): Boolean {
        if (!creationRequested) return true
        if (requestedKeyType != null) return true
        Log.w(TAG, "CryptAuth requested an unsupported FIDO account key type")
        return false
    }

    fun createEnrollmentKey(randomSessionId: ByteArray): CryptAuthEnrollmentKey? {
        if (!creationRequested) return null
        val keyType = requestedKeyType ?: return null
        val newKey = runCatching {
            keyStore.createPendingKey(keyType)
        }.getOrElse {
            Log.w(TAG, "Unable to create the requested FIDO account key", it)
            return null
        }
        pendingKey = newKey
        return CryptAuthEnrollmentKey(
            name = KEY_NAME,
            handle = newKey.handle,
            material = AuthZenWire.encodeGenericPublicKey(newKey.publicKey),
            proof = keyStore.createKeyProof(newKey, randomSessionId),
        )
    }

    fun keyRecord(creationTime: Long, expirationTime: Long): CryptAuthKeyRecord? {
        val newKey = pendingKey ?: return null
        applyKeyActions()
        return keyStore.keyRecord(
            accountName = accountName,
            keyName = KEY_NAME,
            pendingKey = newKey,
            active = createdKeyActive,
            creationTime = creationTime,
            expirationTime = expirationTime,
        )
    }

    fun finishCommit() {
        if (pendingKey == null) return
        keyStore.finishKeyStore(accountName, KEY_NAME)
        recordPresent = true
        active = active || createdKeyActive
        pendingKey = null
    }

    companion object {
        const val KEY_NAME = FidoSourceKeyStore.SOFTWARE_KEY_NAME
    }
}
