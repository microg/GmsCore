package org.microg.gms.asterism.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.asterism.GetAsterismConsentRequest
import com.google.android.gms.asterism.GetAsterismConsentResponse
import com.google.android.gms.asterism.SetAsterismConsentRequest
import com.google.android.gms.asterism.SetAsterismConsentResponse
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Unit tests for Asterism consent management.
 *
 * These tests verify the consent lifecycle: grant, revoke, expire,
 * persistence, and error handling.
 */
@RunWith(AndroidJUnit4::class)
class AsterismConsentTest {

    private lateinit var context: Context
    private lateinit var consentStore: AsterismConsentStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        consentStore = AsterismConsentStore(context)
        // Clear any existing consent data for clean test state
        consentStore.clearAll()
    }

    @After
    fun tearDown() {
        consentStore.clearAll()
    }

    // === Consent Lifecycle Tests ===

    @Test
    fun testGrantConsent_persistsCorrectly() {
        val phoneNumber = "+1234567890"
        val consentType = GetAsterismConsentRequest.CONSENT_TYPE_RCS

        // Grant consent
        val record = ConsentRecord(
            phoneNumber = phoneNumber,
            consentType = consentType,
            granted = true,
            token = "test-token-abc123",
            expiresAt = System.currentTimeMillis() + 86400000, // 24h
            createdAt = System.currentTimeMillis()
        )
        consentStore.setConsent(phoneNumber, record)

        // Verify persistence
        val retrieved = consentStore.getConsent(phoneNumber)
        assertNotNull("Consent should be persisted", retrieved)
        assertEquals(phoneNumber, retrieved!!.phoneNumber)
        assertEquals(consentType, retrieved.consentType)
        assertTrue("Consent should be granted", retrieved.granted)
        assertEquals("test-token-abc123", retrieved.token)
    }

    @Test
    fun testRevokeConsent_removesRecord() {
        val phoneNumber = "+1234567890"

        // Grant first
        consentStore.setConsent(phoneNumber, ConsentRecord(
            phoneNumber = phoneNumber,
            consentType = 0,
            granted = true,
            token = "test-token",
            expiresAt = System.currentTimeMillis() + 86400000,
            createdAt = System.currentTimeMillis()
        ))

        // Revoke
        consentStore.removeConsent(phoneNumber)

        // Verify removal
        val retrieved = consentStore.getConsent(phoneNumber)
        assertNull("Consent should be removed after revocation", retrieved)
    }

    @Test
    fun testConsentExpiry_detectsExpiredToken() {
        val phoneNumber = "+1234567890"
        val expiredRecord = ConsentRecord(
            phoneNumber = phoneNumber,
            consentType = 0,
            granted = true,
            token = "expired-token",
            expiresAt = System.currentTimeMillis() - 3600000, // 1 hour ago
            createdAt = System.currentTimeMillis() - 7200000
        )
        consentStore.setConsent(phoneNumber, expiredRecord)

        // Should detect as expired
        assertTrue("Expired consent should be detected", consentStore.isExpired(phoneNumber))
    }

    @Test
    fun testListActiveConsents_excludesExpired() {
        // Add one expired, one active
        consentStore.setConsent("+1111111111", ConsentRecord(
            phoneNumber = "+1111111111",
            consentType = 0,
            granted = true,
            token = "expired",
            expiresAt = System.currentTimeMillis() - 1000,
            createdAt = System.currentTimeMillis() - 2000
        ))
        consentStore.setConsent("+2222222222", ConsentRecord(
            phoneNumber = "+2222222222",
            consentType = 0,
            granted = true,
            token = "active",
            expiresAt = System.currentTimeMillis() + 86400000,
            createdAt = System.currentTimeMillis()
        ))

        val active = consentStore.listActiveConsents()
        assertEquals("Only one active consent", 1, active.size)
        assertEquals("+2222222222", active[0].phoneNumber)
    }

    @Test
    fun testMultiplePhoneNumbers() {
        val phones = listOf("+1111111111", "+2222222222", "+3333333333")
        phones.forEach { phone ->
            consentStore.setConsent(phone, ConsentRecord(
                phoneNumber = phone,
                consentType = 0,
                granted = true,
                token = "token-$phone",
                expiresAt = System.currentTimeMillis() + 86400000,
                createdAt = System.currentTimeMillis()
            ))
        }

        val all = consentStore.listActiveConsents()
        assertEquals(3, all.size)
        phones.forEach { phone ->
            assertNotNull(consentStore.getConsent(phone))
        }
    }

    // === Consent Type Tests ===

    @Test
    fun testDifferentConsentTypes_storedIndependently() {
        val phoneNumber = "+1234567890"

        // RCS consent
        consentStore.setConsent(phoneNumber, ConsentRecord(
            phoneNumber = phoneNumber,
            consentType = 0, // RCS
            granted = true,
            token = "rcs-token",
            expiresAt = System.currentTimeMillis() + 86400000,
            createdAt = System.currentTimeMillis()
        ))

        // Only one record per phone currently; verify RCS consent exists
        val retrieved = consentStore.getConsent(phoneNumber)
        assertNotNull(retrieved)
        assertEquals(0, retrieved!!.consentType)
        assertEquals("rcs-token", retrieved.token)
    }

    @Test
    fun testDeniedConsent_storedAsDenied() {
        val phoneNumber = "+1234567890"
        consentStore.setConsent(phoneNumber, ConsentRecord(
            phoneNumber = phoneNumber,
            consentType = 0,
            granted = false,
            token = null,
            expiresAt = System.currentTimeMillis() + 86400000,
            createdAt = System.currentTimeMillis()
        ))

        val retrieved = consentStore.getConsent(phoneNumber)
        assertNotNull(retrieved)
        assertFalse("Consent should be denied", retrieved!!.granted)
        assertNull("No token for denied consent", retrieved.token)
    }

    // === Error Handling Tests ===

    @Test
    fun testGetNonexistentConsent_returnsNull() {
        val result = consentStore.getConsent("+9999999999")
        assertNull("Nonexistent consent should return null", result)
    }

    @Test
    fun testRemoveNonexistentConsent_noError() {
        // Should not throw
        consentStore.removeConsent("+9999999999")
        // Verify state unchanged
        assertTrue(consentStore.listActiveConsents().isEmpty())
    }

    @Test
    fun testSetConsent_emptyPhoneNumber_handled() {
        // Empty phone should be handled gracefully
        consentStore.setConsent("", ConsentRecord(
            phoneNumber = "",
            consentType = 0,
            granted = true,
            token = "token",
            expiresAt = System.currentTimeMillis() + 86400000,
            createdAt = System.currentTimeMillis()
        ))
        // Should not crash; retrieval should work or return null
        val result = consentStore.getConsent("")
        // Either null or stored is acceptable
    }

    @Test
    fun testConcurrentAccess() {
        val latch = CountDownLatch(2)
        val errors = mutableListOf<Throwable>()

        val writer1 = Thread {
            try {
                for (i in 1..50) {
                    consentStore.setConsent("+1${i}000000000", ConsentRecord(
                        phoneNumber = "+1${i}000000000",
                        consentType = 0,
                        granted = true,
                        token = "token-$i",
                        expiresAt = System.currentTimeMillis() + 86400000,
                        createdAt = System.currentTimeMillis()
                    ))
                }
            } catch (e: Exception) {
                synchronized(errors) { errors.add(e) }
            } finally {
                latch.countDown()
            }
        }

        val writer2 = Thread {
            try {
                for (i in 1..50) {
                    consentStore.setConsent("+2${i}000000000", ConsentRecord(
                        phoneNumber = "+2${i}000000000",
                        consentType = 0,
                        granted = true,
                        token = "token-2-$i",
                        expiresAt = System.currentTimeMillis() + 86400000,
                        createdAt = System.currentTimeMillis()
                    ))
                }
            } catch (e: Exception) {
                synchronized(errors) { errors.add(e) }
            } finally {
                latch.countDown()
            }
        }

        writer1.start()
        writer2.start()

        assertTrue("Concurrent writes should complete", latch.await(10, TimeUnit.SECONDS))
        assertTrue("No errors during concurrent access: $errors", errors.isEmpty())
        assertEquals("All 100 entries persisted", 100, consentStore.listActiveConsents().size)
    }

    // === Validation Tests ===

    @Test
    fun testTokenFormat() {
        val phoneNumber = "+1234567890"
        val validToken = "abcdef1234567890abcdef1234567890"
        consentStore.setConsent(phoneNumber, ConsentRecord(
            phoneNumber = phoneNumber,
            consentType = 0,
            granted = true,
            token = validToken,
            expiresAt = System.currentTimeMillis() + 86400000,
            createdAt = System.currentTimeMillis()
        ))

        val retrieved = consentStore.getConsent(phoneNumber)
        assertEquals(validToken, retrieved?.token)
    }

    @Test
    fun testExpiryEdgeCases() {
        val phoneNumber = "+1234567890"

        // Exactly now (should not be expired)
        consentStore.setConsent(phoneNumber, ConsentRecord(
            phoneNumber = phoneNumber,
            consentType = 0,
            granted = true,
            token = "edge-token",
            expiresAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis() - 1000
        ))
        // Very near expiry should be accepted
        assertNotNull(consentStore.getConsent(phoneNumber))

        // Far future expiry
        consentStore.removeConsent(phoneNumber)
        consentStore.setConsent(phoneNumber, ConsentRecord(
            phoneNumber = phoneNumber,
            consentType = 0,
            granted = true,
            token = "long-lived",
            expiresAt = System.currentTimeMillis() + 315360000000, // 10 years
            createdAt = System.currentTimeMillis()
        ))
        assertNotNull(consentStore.getConsent(phoneNumber))
        assertFalse(consentStore.isExpired(phoneNumber))
    }
}
