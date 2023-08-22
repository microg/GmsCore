/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.phone

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log

private const val TAG = "SmsContentProvider"

class SmsContentProvider: ContentProvider() {
    override fun onCreate(): Boolean {
        Log.d(TAG, "the smsContentProvider is created.")
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
//        val instance = context?.let { SmsRetrieverCore.getInstance(it.applicationContext) }
        val result = Bundle()
//        if (method == "queryMessage") {
//            var token = extras?.getString("token")
//            if (token == null) {
//                token = ""
//            }
//            val message = instance?.queryMessage(extras?.getString("packageName"),
//                SmsRetrieverUtil.convertStringToVerityToken(token))
//            result.putString("message", message)
//        } else {
//            instance?.consumeMessage(extras?.getString("packageName"), extras?.getString("token"))
//        }
        return result
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(url: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return null
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }
}