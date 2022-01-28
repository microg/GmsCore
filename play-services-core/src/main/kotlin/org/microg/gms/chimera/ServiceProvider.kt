/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.chimera

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import org.microg.gms.DummyService
import org.microg.gms.common.GmsService

class ServiceProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        Log.d(TAG, "onCreate")
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        when (method) {
            "serviceIntentCall" -> {
                val serviceAction = extras?.getString("serviceActionBundleKey") ?: return null
                val ourServiceAction = GmsService.byAction(serviceAction)?.takeIf { it.SERVICE_ID > 0 }?.ACTION ?: serviceAction
                val context = context!!
                val intent = Intent(ourServiceAction).apply { `package` = context.packageName }
                val resolveInfo = context.packageManager.resolveService(intent, 0)
                if (resolveInfo != null) {
                    intent.setClassName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name)
                } else {
                    intent.setClass(context, DummyService::class.java)
                }
                Log.d(TAG, "$method: $serviceAction -> $intent")
                return bundleOf(
                        "serviceResponseIntentKey" to intent
                )
            }
            else -> {
                Log.d(TAG, "$method: $arg, $extras")
                return super.call(method, arg, extras)
            }
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val cursor = MatrixCursor(COLUMNS)
        Log.d(TAG, "query: $uri")
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Log.d(TAG, "insert: $uri, $values")
        return uri
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        Log.d(TAG, "update: $uri, $values, $selection, $selectionArgs")
        return 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        Log.d(TAG, "delete: $uri, $selection, $selectionArgs")
        return 0
    }

    override fun getType(uri: Uri): String {
        Log.d(TAG, "getType: $uri")
        return "vnd.android.cursor.item/com.google.android.gms.chimera"
    }

    companion object {
        private const val TAG = "ChimeraServiceProvider"
        private val COLUMNS = arrayOf("version", "apkPath", "loaderPath", "apkDescStr")
    }
}