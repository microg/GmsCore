/*
 * SPDX-FileCopyrightText: 2023 e Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.profile

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import org.microg.gms.settings.SettingsContract

class ProfileProvider : ContentProvider() {

    val COLUMN_ID = "profile_id"
    val COLUMN_VALUE = "profile_value"

    override fun onCreate(): Boolean {
        ProfileManager.ensureInitialized(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor =
        MatrixCursor(arrayOf(COLUMN_ID, COLUMN_VALUE)).apply {
            ProfileManager.getActiveProfileData(context!!).entries
                .forEach {
                    addRow(arrayOf(it.key, it.value))
                }
        }

    override fun getType(uri: Uri): String {
        return "vnd.android.cursor.item/vnd.${SettingsContract.getAuthority(context!!)}.${uri.path}"
    }

    override fun insert(uri: Uri, values: ContentValues?): Nothing = throw UnsupportedOperationException()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Nothing =
        throw UnsupportedOperationException()

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
    ): Nothing = throw UnsupportedOperationException()

}
