/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import java.net.URI

/** Exact origins, never suffix/prefix matching of URLs received from the target. */
internal object SourceTransferWebPolicy {
    fun isNavigationAllowed(value: String) = host(value) == "accounts.google.com"

    fun isResourceAllowed(value: String) = host(value) in setOf("accounts.google.com",
        "www.gstatic.com", "ssl.gstatic.com", "fonts.gstatic.com", "lh3.googleusercontent.com")

    private fun host(value: String): String? = try {
        if (value.length !in 1..8192 || value.any { it <= ' ' || it == '\\' }) null
        else URI(value).let {
            if (it.scheme != "https" || it.rawUserInfo != null || it.port !in setOf(-1, 443) ||
                it.rawFragment != null || it.rawAuthority != it.host && it.rawAuthority != "${it.host}:443") null
            else it.host
        }
    } catch (_: Exception) { null }

    fun isCheckpointValid(value: String) = value.length in 1..65536 &&
        value.all { it.code in 0x21..0x7e && it != ';' && it != ',' }

    /** Only the isolated profile's exact GASC cookie can produce a checkpoint. */
    fun checkpoint(cookies: String?): String? {
        val matches = cookies.orEmpty().split(';').mapNotNull {
            val separator = it.indexOf('=')
            if (separator < 0 || it.substring(0, separator).trim() != "GASC") null
            else it.substring(separator + 1).trim()
        }
        return matches.singleOrNull()?.takeIf(::isCheckpointValid)
    }
}
