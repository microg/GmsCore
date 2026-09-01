/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network

import android.net.Uri
import androidx.core.content.FileProvider

class DatabaseExportFileProvider : FileProvider() {
    override fun getType(uri: Uri): String? {
        try {
            if (uri.lastPathSegment?.startsWith("cell-") == true) {
                return "application/vnd.microg.location.cell+csv+gzip"
            }
            if (uri.lastPathSegment?.startsWith("wifi-") == true) {
                return "application/vnd.microg.location.wifi+csv+gzip"
            }
        } catch (ignored: Exception) {}
        return super.getType(uri)
    }
}