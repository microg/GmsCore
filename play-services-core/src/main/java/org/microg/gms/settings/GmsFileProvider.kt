/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.settings

import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.FileProvider
import java.io.FileNotFoundException

private const val TAG = "GmsFileProvider"

class GmsFileProvider : FileProvider() {
    private val emptyProjection = arrayOfNulls<String>(0)
    private var isInitializationFailed = false

    override fun attachInfo(context: Context, info: ProviderInfo) {
        try {
            super.attachInfo(context, info)
        } catch (e: Exception) {
            isInitializationFailed = true
            Log.e(TAG, "attachInfo error:${e.message}")
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun getType(uri: Uri): String? {
        if (isInitializationFailed) {
            return null
        }
        return super.getType(uri)
    }

    override fun openFile(
        uri: Uri, mode: String, signal: CancellationSignal?
    ): ParcelFileDescriptor? {
        if (!isInitializationFailed) {
            return super.openFile(uri, mode, signal)
        }
        throw FileNotFoundException("FileProvider creation failed")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        if (isInitializationFailed) {
            return 0
        }
        return super.delete(uri, selection, selectionArgs)
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor {
        if (isInitializationFailed) {
            return MatrixCursor(emptyProjection)
        }
        return super.query(uri, projection, selection, selectionArgs, sortOrder)
    }
}