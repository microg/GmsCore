package org.microg.gms.constellation.core

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RpcClientTest {
    @Test
    fun usesExistingAppCertServiceAction() {
        assertEquals("com.google.android.gms.auth.be.appcert.AppCertService", APP_CERT_SERVICE_ACTION)
    }

    @Test
    fun addsSpatulaHeaderWhenAvailable() {
        val request = Request.Builder().url("https://example.invalid/").build()
        val result = addSpatulaHeader(request, "spatula-value")

        assertEquals("spatula-value", result.header("X-Goog-Spatula"))
    }

    @Test
    fun omitsSpatulaHeaderWhenUnavailable() {
        val request = Request.Builder().url("https://example.invalid/").build()
        val result = addSpatulaHeader(request, null)

        assertNull(result.header("X-Goog-Spatula"))
    }

    @Test
    fun omitsBlankSpatulaHeader() {
        val request = Request.Builder().url("https://example.invalid/").build()
        val result = addSpatulaHeader(request, "  ")

        assertNull(result.header("X-Goog-Spatula"))
    }
}
