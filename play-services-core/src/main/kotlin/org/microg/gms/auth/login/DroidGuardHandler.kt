/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.login

import androidx.lifecycle.lifecycleScope
import com.google.android.gms.droidguard.DroidGuardClient
import com.google.android.gms.tasks.await
import java.util.*

class DroidGuardHandler(private val activity: LoginActivity) {
    fun start(dg: String) {
        activity.lifecycleScope.launchWhenStarted {
            try {
                val result = DroidGuardClient.getResults(activity, "minute_maid", Collections.singletonMap("dg_minutemaid", dg)).await()
                activity.runScript("window.setDgResult('$result')")
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
