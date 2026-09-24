package org.microg.gms.constellation.core

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ClientSignatureInvariantTest {
    @Test(expected = IllegalArgumentException::class)
    fun emptySignatureIsRejected() {
        requireNonEmptyClientSignature(byteArrayOf())
    }

    @Test
    fun nonEmptySignatureIsPreserved() {
        val value = byteArrayOf(1, 2, 3)
        assertArrayEquals(value, requireNonEmptyClientSignature(value))
    }
}
