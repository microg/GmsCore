/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fonts

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_ONLY
import android.util.Log
import java.io.File

class FontsProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        Log.d(TAG, "onCreate")
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        Log.d(TAG, "call $method $arg $extras")
        return null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        Log.e(TAG, "query: $uri ${projection?.toList()} $selection ${selectionArgs?.joinToString(prefix = "[", postfix = "]")}")
        val cursor = MatrixCursor(COLUMNS)
        // We could also return an empty cursor here, but some apps have been reported to crash
        // when their expected font is not returned by Google's font provider.
        cursor.addRow(
            arrayOf(
                1337L, // file_id
                0, // font_ttc_index
                null, // font_variation_settings
                400, // font_weight
                0, // font_italic
                0, // result_code: RESULT_CODE_OK
            )
        )
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        Log.d(TAG, "insert: $uri, $values")
        return uri
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        Log.d(TAG, "update: $uri, $values, $selection, $selectionArgs")
        return 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        Log.d(TAG, "delete: $uri, $selection, $selectionArgs")
        return 0
    }

    override fun getType(uri: Uri): String {
        Log.d(TAG, "getType: $uri")
        return "font/ttf"
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        Log.d(TAG, "openFile: $uri mode: $mode")
        val file = File("/system/fonts/Roboto-Regular.ttf")
        return ParcelFileDescriptor.open(file, MODE_READ_ONLY)
    }

    companion object {
        private const val TAG = "FontsProvider"
        private val COLUMNS = arrayOf(
            "file_id",
            "font_ttc_index",
            "font_variation_settings",
            "font_weight",
            "font_italic",
            "result_code"
        )
    }
}
