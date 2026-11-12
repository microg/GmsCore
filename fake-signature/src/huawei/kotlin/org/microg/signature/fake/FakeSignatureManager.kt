/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.signature.fake

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.huawei.signature.diff.AppListDatabaseOpenHelper

object FakeSignatureManager {
    internal val hostPackages = setOf("com.google.android.gms", "com.android.vending")

    internal fun getContentUri(hostPackage: String): Uri =
        Uri.parse("content://$hostPackage.signature.fake/applist")

    @JvmStatic
    fun enableForPackage(context: Context, packageName: String) {
        require(packageName.isNotBlank()) { "Missing installed package name" }
        val values = ContentValues().apply {
            put(AppListDatabaseOpenHelper.COLUMN_NAME, packageName)
        }
        var failure: RuntimeException? = null
        for (hostPackage in hostPackages) {
            try {
                val uri = getContentUri(hostPackage)
                val result = context.contentResolver.insert(uri, values)
                check(Uri.withAppendedPath(uri, packageName) == result) {
                    "Fake signature write was not acknowledged by $hostPackage"
                }
            } catch (e: RuntimeException) {
                val previousFailure = failure
                if (previousFailure == null) {
                    failure = e
                } else {
                    previousFailure.addSuppressed(e)
                }
            }
        }
        failure?.let { throw it }
    }
}
