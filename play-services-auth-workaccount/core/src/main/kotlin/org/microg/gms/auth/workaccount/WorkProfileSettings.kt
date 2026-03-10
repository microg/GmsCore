/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.workaccount

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import org.microg.gms.settings.SettingsContract

class WorkProfileSettings(private val context: Context) {
    private fun <T> getSettings(vararg projection: String, f: (Cursor) -> T): T =
        SettingsContract.getSettings(
            context,
            SettingsContract.WorkProfile.getContentUri(context),
            projection,
            f
        )

    private fun setSettings(v: ContentValues.() -> Unit) =
        SettingsContract.setSettings(context, SettingsContract.WorkProfile.getContentUri(context), v)

    var allowCreateWorkAccount: Boolean
        get() = getSettings(SettingsContract.WorkProfile.CREATE_WORK_ACCOUNT) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.WorkProfile.CREATE_WORK_ACCOUNT, value) }
}