/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cryptauth

import cryptauthv2.ClientDirective
import cryptauthv2.ClientMetadata
import cryptauthv2.EnrollKeysRequest
import cryptauthv2.EnrollKeysResponse
import cryptauthv2.KeyDirective
import cryptauthv2.KeyType
import cryptauthv2.SyncKeysRequest
import cryptauthv2.SyncKeysResponse
import cryptauthv2.SyncKeysResponse.SyncSingleKeyResponse.KeyAction
import cryptauthv2.SyncKeysResponse.SyncSingleKeyResponse.KeyCreation
import cryptauthv2.SyncKeysResponse.SyncSingleKeyResponse.KeyStorageLevel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import java.security.interfaces.ECPublicKey

// Enrollment and counter reservation share one lock, including across store instances.
internal val cryptAuthStateMutex = Mutex()

internal data class CryptAuthKey(
    val name: String, val handle: ByteString, val type: KeyType,
    val publicKey: ByteString = ByteString.EMPTY, val secret: ByteString, val active: Boolean
) {
    // Generated data-class toString must never reveal private key material.
    override fun toString() = "CryptAuthKey(redacted)"
}

internal data class CryptAuthState(
    val instanceId: String,
    val keys: List<CryptAuthKey> = emptyList(),
    val directives: Map<String, KeyDirective> = emptyMap(),
    val clientDirective: ClientDirective? = null,
    // Durable material whose server acceptance is not yet confirmed. Never expose it as enrolled.
    val pendingKeys: List<CryptAuthKey> = emptyList(),
    // Next assertion counter for each confirmed authzen key. Never log these values.
    val otpCounters: Map<ByteString, Long> = emptyMap()
) {
    override fun toString() = "CryptAuthState(redacted)"
}

internal interface CryptAuthTransport {
    suspend fun sync(request: SyncKeysRequest): SyncKeysResponse
    suspend fun enroll(request: EnrollKeysRequest): EnrollKeysResponse
}

internal interface CryptAuthStateStore {
    fun read(): CryptAuthState
    fun write(state: CryptAuthState)
}

/** Public CryptAuth v2 enrollment. Caller must serialize a complete transaction per store. */
internal class CryptAuthEnrollment(private val transport: CryptAuthTransport, private val store: CryptAuthStateStore) {
    companion object {
        val KEY_NAMES = listOf("PublicKey", "authzen")
        val DEVICE_KEY_HANDLE = "device_key".encodeUtf8()
    }

    suspend fun enroll(metadata: ByteString) {
        val state = store.read()
        val heldKeys = (state.keys + state.pendingKeys).distinctBy { it.name to it.handle }
        val request = SyncKeysRequest(
            application_name = "com.google.android.gms", client_version = "1.0.0",
            sync_single_key_requests = KEY_NAMES.map { name -> SyncKeysRequest.SyncSingleKeyRequest(
                key_name = name, key_handles = heldKeys.filter { it.name == name }.map { it.handle },
                policy_reference = state.directives[name]?.policy_reference
            ) },
            policy_reference = state.clientDirective?.policy_reference,
            client_metadata = ClientMetadata(invocation_reason = if (state.keys.isEmpty())
                ClientMetadata.InvocationReason.NEW_ACCOUNT else ClientMetadata.InvocationReason.FEATURE_TOGGLED),
            client_app_metadata = metadata
        )
        val response = transport.sync(request)
        currentCoroutineContext().ensureActive()
        require(response.unknownFields.size == 0) { "Unsupported CryptAuth server instruction" }
        require(response.server_status == SyncKeysResponse.ServerStatus.SERVER_OK) { "CryptAuth server unavailable" }
        require(response.random_session_id.size in 1..4096) { "Invalid CryptAuth session" }
        require(response.sync_single_key_responses.size == KEY_NAMES.size) { "Invalid CryptAuth key response count" }
        val clientDirective = requireNotNull(response.client_directive) { "Missing CryptAuth client directive" }
        require(clientDirective.checkin_delay_millis > 0 && clientDirective.retry_attempts >= 0 && clientDirective.retry_period_millis > 0) {
            "Invalid CryptAuth client directive"
        }
        // Validate all actions before applying them. Revocations must not wait for EnrollKeys.
        response.sync_single_key_responses.forEachIndexed { index, single ->
            require(single.unknownFields.size == 0) { "Unsupported CryptAuth key instruction" }
            require(single.key_actions.size == request.sync_single_key_requests[index].key_handles.size) { "Invalid CryptAuth key action count" }
            require(single.key_actions.count { it == KeyAction.ACTIVATE } <= 1) { "Multiple active CryptAuth keys" }
            require(single.key_actions.all { it == KeyAction.DELETE } || single.key_actions.any { it == KeyAction.ACTIVATE }) {
                "CryptAuth key actions do not select an active key"
            }
        }
        val retainedKeys = KEY_NAMES.flatMapIndexed { index, name ->
            val single = response.sync_single_key_responses[index]
            heldKeys.filter { it.name == name }.mapIndexedNotNull { keyIndex, key ->
                when (single.key_actions[keyIndex]) {
                    KeyAction.DELETE -> null
                    KeyAction.ACTIVATE -> key.copy(active = true)
                    KeyAction.DEACTIVATE -> key.copy(active = false)
                    // ACTIVATE selects the bundle's only active key, including when other actions are no-ops.
                    KeyAction.KEY_ACTION_UNSPECIFIED -> key.copy(active = false)
                }
            }.also { keys -> require(keys.count { it.active } <= 1) { "Multiple active CryptAuth keys" } }
        }
        fun wasEnrolled(key: CryptAuthKey) = state.keys.any { it.name == key.name && it.handle == key.handle }
        val confirmedBySync = KEY_NAMES.flatMapIndexed { index, name ->
            val actions = response.sync_single_key_responses[index].key_actions
            heldKeys.filter { it.name == name }.mapIndexedNotNull { keyIndex, key ->
                (name to key.handle).takeIf { actions[keyIndex] == KeyAction.ACTIVATE }
            }
        }.toSet()
        val afterActions = state.copy(keys = retainedKeys.filter { wasEnrolled(it) },
            pendingKeys = retainedKeys.filterNot { wasEnrolled(it) },
            otpCounters = state.otpCounters.filterKeys { handle -> retainedKeys.any { it.name == "authzen" && it.handle == handle } })
        if (afterActions != state) store.write(afterActions)
        response.sync_single_key_responses.forEachIndexed { index, single ->
            require(single.key_directive?.crossproof_key_names.isNullOrEmpty()) { "CryptAuth crossproof policy is unsupported" }
            require(single.key_storage_level in listOf(KeyStorageLevel.KEY_STORAGE_LEVEL_UNSPECIFIED, KeyStorageLevel.SOFTWARE) &&
                !single.hardware_user_presence_required && !single.user_verification_required) { "CryptAuth requires protected hardware" }
            if (single.key_creation != KeyCreation.NONE) {
                require(index != 0 || single.key_creation == KeyCreation.ACTIVE) { "CryptAuth identity must be active" }
                require(if (index == 0) single.key_type == KeyType.P256 else single.key_type in listOf(KeyType.RAW128, KeyType.RAW256)) {
                    "Unsupported CryptAuth key algorithm"
                }
            }
        }
        val needsSymmetricKey = response.sync_single_key_responses[1].key_creation != KeyCreation.NONE
        val ephemeral = if (needsSymmetricKey) CryptAuthCrypto.generateKeyPair() else null
        val sharedSecret = ephemeral?.let { CryptAuthCrypto.agreement(it, response.server_ephemeral_dh) }
        val updated = mutableListOf<CryptAuthKey>()
        val created = mutableListOf<CryptAuthKey>()
        val directives = state.directives.toMutableMap()
        try {
            KEY_NAMES.forEachIndexed { index, name ->
                val single = response.sync_single_key_responses[index]
                val oldKeys = retainedKeys.filter { it.name == name }
                val retained = retainedKeys.filter { it.name == name }.toMutableList()
                if (single.key_creation != KeyCreation.NONE) {
                    val active = single.key_creation == KeyCreation.ACTIVE
                    val newKey = if (name == "PublicKey") {
                        // Re-enroll a retained PublicKey without rotating it. Explicitly deleted keys stay deleted.
                        oldKeys.singleOrNull()?.copy(active = active) ?: CryptAuthCrypto.generateKeyPair().let { pair ->
                            CryptAuthKey(name, DEVICE_KEY_HANDLE, KeyType.P256,
                                CryptAuthCrypto.publicKey(pair.public as ECPublicKey), pair.private.encoded.toByteString(), active)
                        }
                    } else {
                        val secret = CryptAuthCrypto.derive(requireNotNull(sharedSecret), name, if (single.key_type == KeyType.RAW128) 16 else 32)
                        CryptAuthKey(name, secret.sha256().base64Url().encodeUtf8(), single.key_type, secret = secret, active = active)
                    }
                    if (active) for (i in retained.indices) retained[i] = retained[i].copy(active = false)
                    retained.removeAll { it.handle == newKey.handle }
                    retained.add(newKey)
                    created.add(newKey)
                }
                require(retained.size <= 32 && retained.count { it.active } <= 1) { "Invalid CryptAuth key state" }
                updated.addAll(retained)
                single.key_directive?.let { directives[name] = it }
            }
            if (created.isNotEmpty()) {
                val enrollment = EnrollKeysRequest(
                    random_session_id = response.random_session_id,
                    client_ephemeral_dh = ephemeral?.let { CryptAuthCrypto.publicKey(it.public as ECPublicKey) } ?: ByteString.EMPTY,
                    enroll_single_key_requests = created.map { key -> EnrollKeysRequest.EnrollSingleKeyRequest(
                        key_name = key.name, new_key_handle = key.handle, key_material = key.publicKey,
                        key_proof = CryptAuthCrypto.proof(key, response.random_session_id)
                    ) }
                )
                // Persist unconfirmed key material before sending it. A lost response, cancellation,
                // or crash must not lose the private half of an identity already accepted remotely.
                val pending = updated.filter { key -> !wasEnrolled(key) || created.any { it.name == key.name && it.handle == key.handle } }
                val beforeEnrollment = afterActions.copy(pendingKeys = pending)
                if (beforeEnrollment != afterActions) store.write(beforeEnrollment)
                currentCoroutineContext().ensureActive()
                val enrolled = transport.enroll(enrollment)
                require(enrolled.enroll_single_key_responses.isEmpty() || enrolled.enroll_single_key_responses.size == created.size) {
                    "Invalid CryptAuth enrollment response count"
                }
            }
            // Public CryptAuth v2 finishes after SyncKeys when no creation was requested.
            // Metadata was already sent with SyncKeys; an empty EnrollKeys POST is not needed.
            // A pending handle becomes confirmed only after explicit ACTIVATE or a successful
            // enrollment proof, never merely because a no-op retained its local material.
            // Chromium CryptAuthV2EnrollerImpl::OnSyncKeysSuccess, revision
            // 9e85c2dcfd5346dc545992315b13e8cac5ce3155, ash/services/device_sync/.
            currentCoroutineContext().ensureActive()
            fun confirmed(key: CryptAuthKey) = wasEnrolled(key) || (key.name to key.handle) in confirmedBySync ||
                created.any { it.name == key.name && it.handle == key.handle }
            store.write(afterActions.copy(keys = updated.filter(::confirmed), directives = directives.toMap(),
                clientDirective = clientDirective, pendingKeys = updated.filterNot(::confirmed)))
        } finally { sharedSecret?.fill(0) }
    }
}
