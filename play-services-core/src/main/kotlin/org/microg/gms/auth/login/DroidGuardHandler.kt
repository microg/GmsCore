/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.login

import androidx.lifecycle.lifecycleScope
import org.microg.gms.droidguard.core.DroidGuardResultCreator
import java.util.*

class DroidGuardHandler(private val activity: LoginActivity) {
    fun start(dg: String) {
        activity.lifecycleScope.launchWhenStarted {
            try {
                val result = DroidGuardResultCreator.getResults(activity, "minute_maid", Collections.singletonMap("dg_minutemaid", dg))
                activity.runScript("window.setDgResult('$result')")
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
