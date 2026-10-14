/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.signature.fake

import android.content.ContentProvider
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import com.huawei.signature.diff.AppListDatabaseOpenHelper

class FakeSignatureProvider : ContentProvider() {
    private lateinit var openHelper: AppListDatabaseOpenHelper

    override fun onCreate(): Boolean {
        val hostContext = context ?: return false
        openHelper = AppListDatabaseOpenHelper(hostContext)
        return true
    }

    private fun enforceTrustedCaller(): Context {
        val hostContext = checkNotNull(context) { "Fake signature provider is not initialized" }
        val callingUid = Binder.getCallingUid()
        val packageManager = hostContext.packageManager
        val callingPackages = packageManager.getPackagesForUid(callingUid)
        if (callingPackages.isNullOrEmpty() || callingPackages.any { it !in FakeSignatureManager.hostPackages } ||
            packageManager.checkSignatures(hostContext.applicationInfo.uid, callingUid) != PackageManager.SIGNATURE_MATCH) {
            throw SecurityException("Caller UID $callingUid is not allowed to access fake signature policies")
        }
        return hostContext
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val hostContext = enforceTrustedCaller()
        require(FakeSignatureManager.getContentUri(hostContext.packageName) == uri) {
            "Unsupported fake signature URI: $uri"
        }
        val packageName = values?.getAsString(AppListDatabaseOpenHelper.COLUMN_NAME)
        require(!packageName.isNullOrBlank()) { "Missing installed package name" }
        val policy = ContentValues().apply {
            put(AppListDatabaseOpenHelper.COLUMN_NAME, packageName)
            put(AppListDatabaseOpenHelper.COLUMN_FAKE, 1)
        }
        val rowId = openHelper.writableDatabase.insertWithOnConflict(
            AppListDatabaseOpenHelper.TABLE_APPLIST, null, policy, SQLiteDatabase.CONFLICT_REPLACE
        )
        check(rowId != -1L) { "Failed to enable fake signature for $packageName" }
        return Uri.withAppendedPath(uri, packageName)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        enforceTrustedCaller()
        throw UnsupportedOperationException("Fake signature policies are write-only")
    }

    override fun getType(uri: Uri): String? {
        // The transport can clear the caller identity for type queries, so never serve them.
        throw SecurityException("Fake signature content types are not exposed")
    }

    override fun getTypeAnonymous(uri: Uri): String? {
        throw SecurityException("Fake signature content types are not exposed")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        enforceTrustedCaller()
        throw UnsupportedOperationException("Deleting fake signature policies is not supported")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        enforceTrustedCaller()
        throw UnsupportedOperationException("Use insert to enable a fake signature policy")
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        enforceTrustedCaller()
        throw UnsupportedOperationException("Use insert to enable a fake signature policy")
    }

    override fun bulkInsert(uri: Uri, values: Array<out ContentValues>): Int {
        enforceTrustedCaller()
        throw UnsupportedOperationException("Use insert to enable a fake signature policy")
    }

    override fun applyBatch(operations: ArrayList<ContentProviderOperation>): Array<ContentProviderResult> {
        enforceTrustedCaller()
        throw UnsupportedOperationException("Use insert to enable a fake signature policy")
    }

    override fun shutdown() {
        if (::openHelper.isInitialized) openHelper.close()
        super.shutdown()
    }
}
