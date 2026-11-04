/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification.ts43

import org.junit.Assert.assertEquals
import org.junit.Test
import org.microg.gms.constellation.core.proto.ServiceEntitlementRequest

class ServiceEntitlementTerminalMetadataTest {

    @Test
    fun buildBaseUrl_usesDeviceMetadataWhenRequestOmitsIt() {
        val url = builder(ServiceEntitlementRequest()).buildBaseUrl("https://example.com/entitlement")

        assertEquals("Acme", url.queryParameter("terminal_vendor"))
        assertEquals("Model-1234", url.queryParameter("terminal_model"))
        assertEquals("Android-Release-1234", url.queryParameter("terminal_sw_version"))
    }

    @Test
    fun buildBaseUrl_explicitRequestMetadataTakesPrecedenceAndIsTruncated() {
        val request = ServiceEntitlementRequest(
            terminal_vendor = "RequestVendor",
            terminal_model = "RequestModelLong",
            terminal_software_version = "RequestSoftwareVersionThatIsLong"
        )
        val url = builder(request).buildBaseUrl("https://example.com/entitlement")

        assertEquals("Requ", url.queryParameter("terminal_vendor"))
        assertEquals("RequestMod", url.queryParameter("terminal_model"))
        assertEquals("RequestSoftwareVersi", url.queryParameter("terminal_sw_version"))
    }

    @Test
    fun resolveTerminalValue_fallsBackBeforeTruncating() {
        assertEquals("Acme", resolveTerminalValue("", "AcmeDevices", 4))
        assertEquals("Requ", resolveTerminalValue("RequestVendor", "AcmeDevices", 4))
    }

    private fun builder(request: ServiceEntitlementRequest) = ServiceEntitlementBuilder(
        imsi = "",
        iccid = null,
        terminalId = "device-imei",
        terminalVendor = "AcmeDevices",
        terminalModel = "Model-123456789",
        terminalSoftwareVersion = "Android-Release-123456789",
        groupIdLevel1 = null,
        eapId = "",
        appIds = listOf("ap2014"),
        req = request
    )
}
