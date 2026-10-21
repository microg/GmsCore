/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.cryptauth

enum class CryptAuthKeyType(
    /** Length in bytes for symmetric key types, or null for asymmetric key types. */
    val symmetricKeyLength: Int?,
) {
    RAW128(16),
    RAW256(32),
    P256(null),
}

/** Account-scoped CryptAuth key material and enrollment state. */
class CryptAuthKeyRecord(
    val account: String,
    val keyName: String,
    handle: ByteArray,
    val keyType: CryptAuthKeyType,
    val active: Boolean,
    val creationTime: Long,
    val expirationTime: Long,
    val exportable: Boolean,
    masterKey: ByteArray? = null,
    publicKey: ByteArray? = null,
    privateKey: ByteArray? = null,
) {
    val handle = handle.copyOf()
    val masterKey = masterKey?.copyOf()
    val publicKey = publicKey?.copyOf()
    val privateKey = privateKey?.copyOf()

    init {
        require(account.isNotBlank()) { "CryptAuth account must not be blank" }
        require(keyName.isNotBlank()) { "CryptAuth key name must not be blank" }
        require(this.handle.isNotEmpty()) { "CryptAuth key handle must not be empty" }
        require(creationTime >= 0L) { "CryptAuth creation time must not be negative" }
        require(expirationTime >= creationTime) { "CryptAuth expiration time precedes creation time" }
        when (keyType) {
            CryptAuthKeyType.RAW128, CryptAuthKeyType.RAW256 ->
                requireSymmetricKey(requireNotNull(keyType.symmetricKeyLength))
            CryptAuthKeyType.P256 -> {
                require(this.masterKey == null) { "P-256 records must not contain a master key" }
                require(this.publicKey != null && this.publicKey.isNotEmpty()) {
                    "P-256 public key must not be empty"
                }
            }
        }
    }

    private fun requireSymmetricKey(expectedLength: Int) {
        require(masterKey?.size == expectedLength) { "Invalid symmetric key length" }
        require(publicKey == null && privateKey == null) {
            "Symmetric records must not contain public or private key material"
        }
    }
}
