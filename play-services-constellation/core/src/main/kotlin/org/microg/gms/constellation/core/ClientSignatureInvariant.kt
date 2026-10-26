package org.microg.gms.constellation.core

internal fun requireNonEmptyClientSignature(signature: ByteArray): ByteArray {
    require(signature.isNotEmpty()) { "Client signature is empty" }
    return signature
}
