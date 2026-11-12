/* SPDX-License-Identifier: Apache-2.0 */
package com.google.android.gms.wearable.consent

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.test.InstrumentationTestCase
import org.microg.gms.wearable.consent.WearableConsentStore
import java.io.File
import java.io.IOException
import java.util.UUID

/** State and real SQLite tests; the visible companion Activity flow is validated separately. */
@Suppress("DEPRECATION")
class WearableTermsConsentTest : InstrumentationTestCase() {
    fun testNoStoredDecisionDefaultsToNoConsent() = withIsolatedDatabase { context ->
        val first = WearableConsentStore(context)
        try { assertNull(first.read()) } finally { first.close() }
        val reopened = WearableConsentStore(context)
        try { assertNull(reopened.read()) } finally { reopened.close() }
    }

    fun testAlreadyOpenReaderSeesAnotherStoresCommittedAcceptance() = withIsolatedDatabase { context ->
        val reader = WearableConsentStore(context)
        val writer = WearableConsentStore(context)
        try {
            assertNull(reader.read())
            writer.acceptTerms { }
            val accepted = writer.read()
            assertNotNull(accepted)
            assertEquals(accepted!!.acceptedAtMillis, reader.read()!!.acceptedAtMillis)
        } finally {
            writer.close()
            reader.close()
        }
    }

    fun testExplicitAcceptanceIsDurableBeforeSuccessAndDoesNotAutoAcceptNextRequest() = withIsolatedDatabase { context ->
        val store = WearableConsentStore(context)
        try {
            val before = System.currentTimeMillis()
            var callerChecks = 0
            var writes = 0
            val decision = WearableTermsConsentDecision({ callerChecks++ }, { guard ->
                store.acceptTerms(guard)
                writes++
            })
            assertTrue(decision.accept(true))
            assertEquals(2, callerChecks)
            assertEquals(1, writes)
            assertFalse(decision.accept(true))
            assertEquals(1, writes)
            val reopened = WearableConsentStore(context)
            try {
                val record = reopened.read()
                assertNotNull(record)
                assertTrue(record!!.acceptedAtMillis >= before)
                assertTrue(record.acceptedAtMillis <= System.currentTimeMillis())
            } finally { reopened.close() }
            val next = WearableTermsConsentDecision({ }, { guard -> writes++; store.acceptTerms(guard) })
            assertFalse(next.accept(false))
            assertEquals("A saved record does not replace a new explicit UI decision", 1, writes)
            next.cancel()
            assertFalse(next.accept(true))
        } finally { store.close() }
    }

    fun testUncheckedChoiceCannotPersistButCanBeCheckedLater() {
        var writes = 0
        val decision = WearableTermsConsentDecision({ }, { guard -> guard(); writes++ })
        assertFalse(decision.accept(false))
        assertEquals(0, writes)
        assertTrue(decision.accept(true))
        assertEquals(1, writes)
    }

    fun testCancelIsTerminalWithoutPersisting() {
        var writes = 0
        val decision = WearableTermsConsentDecision({ }, { guard -> guard(); writes++ })
        decision.cancel()
        decision.cancel()
        assertFalse(decision.accept(false))
        assertFalse(decision.accept(true))
        assertEquals(0, writes)
    }

    fun testCancelledRequestDoesNotPersistOrAuthorizeRecreatedRequest() = withIsolatedDatabase { context ->
        val store = WearableConsentStore(context)
        try {
            val first = WearableTermsConsentDecision({ }, { guard -> store.acceptTerms(guard) })
            first.cancel()
            assertFalse(first.accept(true))
            val recreated = WearableTermsConsentDecision({ }, { guard -> store.acceptTerms(guard) })
            assertFalse(recreated.accept(false))
            assertNull(store.read())
            recreated.cancel()
        } finally { store.close() }
    }

    fun testCallerRejectedBeforeWriteCannotAcceptOrReplay() {
        var trusted = false
        var writes = 0
        val decision = WearableTermsConsentDecision({ if (!trusted) throw SecurityException() }, { guard -> guard(); writes++ })
        assertFalse(decision.accept(true))
        trusted = true
        assertFalse(decision.accept(true))
        assertEquals(0, writes)
    }

    fun testCallerRevokedBeforeCommitRollsBackNewConsent() = withIsolatedDatabase { context ->
        val store = WearableConsentStore(context)
        try {
            var trusted = true
            var checks = 0
            val decision = WearableTermsConsentDecision({
                checks++
                if (!trusted) throw SecurityException()
            }, { guard -> store.acceptTerms { trusted = false; guard() } })
            assertFalse(decision.accept(true))
            assertEquals(2, checks)
            assertNull(store.read())
            trusted = true
            assertFalse(decision.accept(true))
            assertNull(store.read())
        } finally { store.close() }
    }

    fun testFailedPreCommitGuardPreservesExistingConsent() = withIsolatedDatabase { context ->
        val store = WearableConsentStore(context)
        try {
            store.acceptTerms { }
            val previous = store.read()!!.acceptedAtMillis
            var trusted = true
            val decision = WearableTermsConsentDecision({ if (!trusted) throw SecurityException() },
                { guard -> store.acceptTerms { trusted = false; guard() } })
            assertFalse(decision.accept(true))
            assertEquals(previous, store.read()!!.acceptedAtMillis)
        } finally { store.close() }
    }

    fun testPersistenceExceptionPreventsSuccessAndReplay() {
        var writes = 0
        var failure = true
        val decision = WearableTermsConsentDecision({ }, {
            writes++
            if (failure) throw IOException("synthetic storage failure")
        })
        assertFalse(decision.accept(true))
        failure = false
        assertFalse(decision.accept(true))
        assertEquals(1, writes)
    }

    fun testRealDatabaseOpenFailureDoesNotAcceptAndRequiresNewDecision() = withIsolatedDatabase { context ->
        val database = File(context.noBackupFilesDir, DATABASE)
        check(database.parentFile!!.exists() || database.parentFile!!.mkdirs())
        assertTrue(database.mkdir())
        val child = File(database, "synthetic-obstruction")
        child.writeText("not account data")
        val persist: (() -> Unit) -> Unit = { guard ->
            val store = WearableConsentStore(context)
            try { store.acceptTerms(guard) } finally { store.close() }
        }
        val decision = WearableTermsConsentDecision({ }, persist)
        try { assertFalse(decision.accept(true)) } finally {
            assertTrue(child.delete())
            assertTrue(database.delete())
        }
        val store = WearableConsentStore(context)
        try {
            assertNull(store.read())
            assertFalse(decision.accept(true))
            assertNull(store.read())
            val fresh = WearableTermsConsentDecision({ }, { guard -> store.acceptTerms(guard) })
            assertTrue(fresh.accept(true))
            assertNotNull(store.read())
        } finally { store.close() }
    }

    fun testGeneralRequestAcceptsOnlyTheSupportedDefaultScope() {
        WearableTermsConsentRequest.validate(Intent(ACTION))
        WearableTermsConsentRequest.validate(Intent(ACTION)
            .putExtra("terms_context", 0).putExtra("use_consent_per_watch", false)
            .putExtra("show_backup_consent", false).putExtra("is_watch_supervised", false))
    }

    fun testMissingEntryAndUnsupportedRequestScopeAreRejected() {
        val invalid = mutableListOf<Intent?>(null, Intent(), Intent("unrelated.action"),
                Intent(ACTION).putExtra("terms_context", 1),
                Intent(ACTION).putExtra("terms_context", -1),
                Intent(ACTION).putExtra("terms_context", "0"),
                Intent(ACTION).putExtra("terms_context", 0L))
        for (flag in listOf("use_consent_per_watch", "show_backup_consent", "is_watch_supervised")) {
            invalid += Intent(ACTION).putExtra(flag, true)
            invalid += Intent(ACTION).putExtra(flag, "false")
            invalid += Intent(ACTION).putExtra(flag, 0)
        }
        for (intent in invalid) {
            try {
                WearableTermsConsentRequest.validate(intent)
                fail("Unsupported terms request was accepted")
            } catch (_: IllegalArgumentException) { }
        }
    }

    fun testOrdinaryTestApplicationCannotAcquireTrustedCallerSnapshot() {
        val context = instrumentation.context
        assertTrue(context.packageName.endsWith(".test"))
        for (name in listOf("", context.packageName, "org.microg.synthetic.uninstalled")) {
            try {
                WearableTermsConsentCaller.capture(context, name)
                fail("Unsigned or absent caller was trusted")
            } catch (_: SecurityException) { }
        }
    }

    private fun withIsolatedDatabase(block: (Context) -> Unit) {
        // Instrumentation runs with the target UID, so the test APK's data directory is inaccessible.
        val target = instrumentation.targetContext
        val cacheDirectory = target.cacheDir.canonicalFile
        val fixtureDirectory = File(cacheDirectory, "wearable-consent-instrumentation-${UUID.randomUUID()}").canonicalFile
        assertEquals("The fixture must be an immediate child of the target cache", cacheDirectory, fixtureDirectory.parentFile)
        assertFalse(fixtureDirectory.exists())
        assertTrue(fixtureDirectory.mkdir())
        val context = object : ContextWrapper(target) {
            override fun getNoBackupFilesDir(): File = fixtureDirectory
        }
        try { block(context) } finally {
            clearOwnedDatabase(fixtureDirectory)
            // Delete only this newly created directory, and only when empty.
            assertTrue("The fixture must release every owned file", fixtureDirectory.delete())
        }
    }

    private fun clearOwnedDatabase(fixtureDirectory: File) {
        for (suffix in listOf("", "-journal", "-wal", "-shm")) {
            val owned = File(fixtureDirectory, DATABASE + suffix)
            assertEquals("Database cleanup must remain inside this fixture", fixtureDirectory, owned.canonicalFile.parentFile)
            if (owned.exists()) {
                assertTrue("Cleanup never recurses into a directory", owned.isFile)
                assertTrue("Only this fixture's exact database files are removed", owned.delete())
            }
        }
    }

    private companion object {
        const val DATABASE = "wearable-consent.db"
        const val ACTION = "com.google.android.gms.wearable.TOS"
    }
}
