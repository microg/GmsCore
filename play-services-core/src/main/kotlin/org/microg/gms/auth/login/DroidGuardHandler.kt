/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.login

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.droidguard.DroidGuardClient
import com.google.android.gms.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections

private const val TAG = "DroidGuardHandler"

class DroidGuardHandler(private val activity: LoginActivity) {
    fun start(dg: String) {
        activity.lifecycleScope.launch {
            Log.d(TAG, "getDroidGuardResult start ${Thread.currentThread().name}")
            val start = System.currentTimeMillis()
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    withTimeoutOrNull(5000) {
                        DroidGuardClient.getResults(activity, "minute_maid", Collections.singletonMap("dg_minutemaid", dg)).await()
                    }
                }
            }.getOrNull()
            Log.d(TAG, "start: result: $result")
            withContext(Dispatchers.Main) {
                // Always resolve the page's setDgResult callback. On builds without a DroidGuard
                // service (or when it times out), report null so the sign-in page proceeds
                // immediately instead of waiting ~60s for a result that will never arrive.
                activity.runScript("window.setDgResult('${result ?: "null"}')")
            }
            Log.d(TAG, "getDroidGuardResult end " + (System.currentTimeMillis() - start))
        }
    }
}
