/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable

import android.content.Context
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.Wearable

object WearablePreferences {
    @JvmStatic
    fun isTosAccepted(context: Context): Boolean = SettingsContract.getSettings(
        context, Wearable.getContentUri(context), arrayOf(Wearable.TOS_ACCEPTED)
    ) { it.getInt(0) != 0 }

    @JvmStatic
    fun setTosAccepted(context: Context, accepted: Boolean) {
        SettingsContract.setSettings(context, Wearable.getContentUri(context)) {
            put(Wearable.TOS_ACCEPTED, accepted)
        }
    }

    @JvmStatic
    fun isNotificationsEnabled(context: Context): Boolean = SettingsContract.getSettings(
        context, Wearable.getContentUri(context), arrayOf(Wearable.NOTIFICATIONS_ENABLED)
    ) { it.getInt(0) != 0 }

    @JvmStatic
    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        SettingsContract.setSettings(context, Wearable.getContentUri(context)) {
            put(Wearable.NOTIFICATIONS_ENABLED, enabled)
        }
    }

    @JvmStatic
    fun isMediaControlEnabled(context: Context): Boolean = SettingsContract.getSettings(
        context, Wearable.getContentUri(context), arrayOf(Wearable.MEDIA_CONTROL_ENABLED)
    ) { it.getInt(0) != 0 }

    @JvmStatic
    fun setMediaControlEnabled(context: Context, enabled: Boolean) {
        SettingsContract.setSettings(context, Wearable.getContentUri(context)) {
            put(Wearable.MEDIA_CONTROL_ENABLED, enabled)
        }
    }

    @JvmStatic
    fun isCallControlEnabled(context: Context): Boolean = SettingsContract.getSettings(
        context, Wearable.getContentUri(context), arrayOf(Wearable.CALL_CONTROL_ENABLED)
    ) { it.getInt(0) != 0 }

    @JvmStatic
    fun setCallControlEnabled(context: Context, enabled: Boolean) {
        SettingsContract.setSettings(context, Wearable.getContentUri(context)) {
            put(Wearable.CALL_CONTROL_ENABLED, enabled)
        }
    }
}
