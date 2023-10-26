/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.utils

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream

fun ByteArray.encodeBase64(noPadding: Boolean, noWrap: Boolean = true, urlSafe: Boolean = true): String {
    var flags = 0
    if (noPadding) flags = flags or Base64.NO_PADDING
    if (noWrap) flags = flags or Base64.NO_WRAP
    if (urlSafe) flags = flags or Base64.URL_SAFE
    return Base64.encodeToString(this, flags)
}

fun ByteArray.sha256(): ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(this)
}

fun ByteArray.gzip(): ByteArray {
    ByteArrayOutputStream().use { byteOutput ->
        GZIPOutputStream(byteOutput).use { gzipOutput ->
            gzipOutput.write(this)
            gzipOutput.finish()
            return byteOutput.toByteArray()
        }
    }
}
