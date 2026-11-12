/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import com.google.security.cryptauth.lib.securemessage.GenericPublicKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class NearbyCryptoTest {

    @Test
    fun testUkey2HandshakeAndKeyExchange() {
        val clientCrypto = NearbyCrypto(isServer = false)
        val serverCrypto = NearbyCrypto(isServer = true)

        val clientPubKey = clientCrypto.getGenericPublicKey()
        val serverPubKey = serverCrypto.getGenericPublicKey()
        assertNotNull(clientPubKey)
        assertNotNull(serverPubKey)

        val clientInitRaw = "ClientInitTestMessage".toByteArray(StandardCharsets.UTF_8)
        val serverInitRaw = "ServerInitTestMessage".toByteArray(StandardCharsets.UTF_8)

        // Both complete key exchange
        serverCrypto.completeKeyExchange(clientPubKey, clientInitRaw, serverInitRaw)
        clientCrypto.completeKeyExchange(serverPubKey, clientInitRaw, serverInitRaw)

        // Auth keys and PIN codes must match
        val clientAuth = clientCrypto.authKey
        val serverAuth = serverCrypto.authKey
        assertNotNull(clientAuth)
        assertNotNull(serverAuth)
        assertArrayEquals(clientAuth, serverAuth)

        val clientPin = clientCrypto.pinCode
        val serverPin = serverCrypto.pinCode
        assertEquals(clientPin, serverPin)
        assertEquals(4, clientPin.length)
        assertTrue(clientPin.all { it.isDigit() })

        // Test bidirectional message encryption / decryption
        val messageFromClient = "Hello from microG client!".toByteArray(StandardCharsets.UTF_8)
        val encryptedByClient = clientCrypto.encryptOfflineFrame(messageFromClient)
        val decryptedByServer = serverCrypto.decryptSecureMessage(encryptedByClient)
        assertArrayEquals(messageFromClient, decryptedByServer)

        val messageFromServer = "Hello back from microG server!".toByteArray(StandardCharsets.UTF_8)
        val encryptedByServer = serverCrypto.encryptOfflineFrame(messageFromServer)
        val decryptedByClient = clientCrypto.decryptSecureMessage(encryptedByServer)
        assertArrayEquals(messageFromServer, decryptedByClient)
    }

    @Test
    fun testTamperedEncryptedMessageThrowsSecurityException() {
        val clientCrypto = NearbyCrypto(isServer = false)
        val serverCrypto = NearbyCrypto(isServer = true)

        val clientPubKey = clientCrypto.getGenericPublicKey()
        val serverPubKey = serverCrypto.getGenericPublicKey()
        val clientInitRaw = "ClientInitRaw".toByteArray(StandardCharsets.UTF_8)
        val serverInitRaw = "ServerInitRaw".toByteArray(StandardCharsets.UTF_8)

        serverCrypto.completeKeyExchange(clientPubKey, clientInitRaw, serverInitRaw)
        clientCrypto.completeKeyExchange(serverPubKey, clientInitRaw, serverInitRaw)

        val message = "Secret payload".toByteArray(StandardCharsets.UTF_8)
        val encrypted = clientCrypto.encryptOfflineFrame(message)

        // Corrupt a byte in the encrypted message
        val tampered = encrypted.copyOf()
        tampered[tampered.size - 5] = (tampered[tampered.size - 5].toInt() xor 0xAA).toByte()

        try {
            serverCrypto.decryptSecureMessage(tampered)
            fail("Expected SecurityException on tampered message")
        } catch (e: SecurityException) {
            // Success: HMAC verification caught tampering
        }
    }

    @Test
    fun testReplayedSequenceNumberThrowsSecurityException() {
        val clientCrypto = NearbyCrypto(isServer = false)
        val serverCrypto = NearbyCrypto(isServer = true)

        val clientPubKey = clientCrypto.getGenericPublicKey()
        val serverPubKey = serverCrypto.getGenericPublicKey()
        val clientInitRaw = "ClientInitRaw".toByteArray(StandardCharsets.UTF_8)
        val serverInitRaw = "ServerInitRaw".toByteArray(StandardCharsets.UTF_8)

        serverCrypto.completeKeyExchange(clientPubKey, clientInitRaw, serverInitRaw)
        clientCrypto.completeKeyExchange(serverPubKey, clientInitRaw, serverInitRaw)

        val msg1 = clientCrypto.encryptOfflineFrame("First".toByteArray(StandardCharsets.UTF_8))
        serverCrypto.decryptSecureMessage(msg1)

        // Replaying msg1 should fail because sequence number must increment
        try {
            serverCrypto.decryptSecureMessage(msg1)
            fail("Expected SecurityException on replayed sequence number")
        } catch (e: SecurityException) {
            // Success: replay detected
        }
    }

    @Test
    fun testPinCodeFormatting() {
        val keyZeros = ByteArray(32) { 0 }
        val pinZeros = NearbyCrypto.pinCodeFromAuthKey(keyZeros)
        assertEquals("0000", pinZeros)

        val keyOnes = ByteArray(32) { 1 }
        val pinOnes = NearbyCrypto.pinCodeFromAuthKey(keyOnes)
        assertEquals(4, pinOnes.length)
        assertTrue(pinOnes.all { it.isDigit() })
    }
}
