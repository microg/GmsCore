/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.login

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.droidguard.DroidGuardClient
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.TimeUnit

private const val TAG = "DroidGuardHandler"

class DroidGuardHandler(private val activity: LoginActivity) {
    fun start(dg: String) {
        activity.lifecycleScope.launch {
            Log.d(TAG, "getDroidGuardResult start ${Thread.currentThread().name}")
            val start = System.currentTimeMillis()
            try {
                val result = withContext(Dispatchers.IO) {
                    val resultTask = DroidGuardClient.getResults(activity, "minute_maid", Collections.singletonMap("dg_minutemaid", dg))
                    Tasks.await(resultTask, 5000, TimeUnit.MILLISECONDS)
                }
                Log.d(TAG, "start: result: $result")
                withContext(Dispatchers.Main) {
                    activity.runScript("window.setDgResult('$result')")
                }
            } catch (e: Exception) {
                // Ignore
            }
            Log.d(TAG, "getDroidGuardResult end " + (System.currentTimeMillis() - start))
        }
    }
}
