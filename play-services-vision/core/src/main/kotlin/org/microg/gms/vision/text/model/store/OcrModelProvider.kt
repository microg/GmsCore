/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.model.store

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import org.microg.gms.vision.text.ui.OcrModelPrompt
import java.io.FileNotFoundException

class OcrModelProvider : ContentProvider() {
    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val hostContext = context ?: return null
        if (method != "request") return null
        val pack = arg ?: return null
        if (!OcrModels.supported || pack !in OcrModels.packs) return null

        val ready = OcrModels.isReady(hostContext, pack)
        if (!ready) {
            val identity = Binder.clearCallingIdentity()
            try {
                OcrModelPrompt.notifyMissing(hostContext, pack, OcrModels.remainingBytes(hostContext, pack))
            } finally {
                Binder.restoreCallingIdentity(identity)
            }
        }
        return Bundle().apply {
            putBoolean("ready", ready)
            putLong("version", if (ready) MODEL_VERSION else 0L)
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val segments = uri.pathSegments
        if (mode != "r" || segments.size != 4 || segments[0] != "file") throw FileNotFoundException()
        val hostContext = context ?: throw FileNotFoundException()
        val version = segments[2].toLongOrNull() ?: throw FileNotFoundException()
        return OcrModels.open(hostContext, segments[1], version, segments[3])
    }

    override fun getType(uri: Uri): String? = null
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
}
