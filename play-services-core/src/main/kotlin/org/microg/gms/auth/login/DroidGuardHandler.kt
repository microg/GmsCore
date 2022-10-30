/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.login

import android.util.Base64
import androidx.lifecycle.lifecycleScope
import org.microg.gms.droidguard.core.DroidGuardResultCreator.Companion.getResult
import org.microg.gms.utils.toBase64
import java.util.*

class DroidGuardHandler(private val activity: LoginActivity) {
    fun start(dg: String) {
        activity.lifecycleScope.launchWhenStarted {
            try {
                val result = getResult(activity, "minute_maid", Collections.singletonMap("dg_minutemaid", dg))
                    .toBase64(Base64.NO_WRAP, Base64.NO_PADDING, Base64.URL_SAFE)
                activity.runScript("window.setDgResult('$result')")
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
