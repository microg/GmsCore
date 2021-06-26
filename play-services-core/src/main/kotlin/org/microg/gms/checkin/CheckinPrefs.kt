/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.checkin

import android.content.Context
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.CheckIn

object CheckinPrefs {

    @JvmStatic
    fun isEnabled(context: Context): Boolean {
        val projection = arrayOf(CheckIn.ENABLED)
        return SettingsContract.getSettings(context, CheckIn.CONTENT_URI, projection) { c ->
            c.getInt(0) != 0
        }
    }

}
