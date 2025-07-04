/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import org.microg.gms.settings.SettingsContract

object GameProfileSettings {
    private fun <T> getSettings(context: Context, vararg projection: String, f: (Cursor) -> T): T = SettingsContract.getSettings(
        context, SettingsContract.GameProfile.getContentUri(context), projection, f
    )

    private fun setSettings(context: Context, v: ContentValues.() -> Unit) = SettingsContract.setSettings(context, SettingsContract.GameProfile.getContentUri(context), v)

    @JvmStatic
    fun setAllowCreatePlayer(context: Context, enabled: Boolean) {
        setSettings(context) { put(SettingsContract.GameProfile.ALLOW_CREATE_PLAYER, enabled) }
    }

    @JvmStatic
    fun setAllowUploadGamePlayed(context: Context, enabled: Boolean) {
        setSettings(context) { put(SettingsContract.GameProfile.ALLOW_UPLOAD_GAME_PLAYED, enabled) }
    }

    @JvmStatic
    fun getAllowCreatePlayer(context: Context): Boolean = getSettings(context, SettingsContract.GameProfile.ALLOW_CREATE_PLAYER) { c -> c.getInt(0) != 0 }

    @JvmStatic
    fun getAllowUploadGamePlayed(context: Context): Boolean = getSettings(context, SettingsContract.GameProfile.ALLOW_UPLOAD_GAME_PLAYED) { c -> c.getInt(0) != 0 }

}