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
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.TimeUnit

private const val TAG = "DroidGuardHandler"

interface DroidGuardCallback {
    fun onSuccess(result: String)
    fun onError(error: Throwable)
}

class DroidGuardHandler(private val activity: LoginActivity) {
    fun start(dg: String) {
        getDroidGuardResultAsync("minute_maid", Collections.singletonMap("dg_minutemaid", dg), object : DroidGuardCallback {
            override fun onSuccess(result: String) {
                Log.d(TAG, "start: result: $result")
                activity.runScript("window.setDgResult('$result')")
            }

            override fun onError(error: Throwable) {
                Log.w(TAG, "onError: ", error)
            }})
    }

    fun getDroidGuardResultAsync(flow: String, data: Map<String, String>, callback: DroidGuardCallback) {
        activity.lifecycleScope.launchWhenStarted {
            val start = System.currentTimeMillis()
            try {
                val result = withContext(Dispatchers.IO) {
                    val resultTask = DroidGuardClient.getResults(activity, flow, data)
                    Tasks.await(resultTask, 5000, TimeUnit.MILLISECONDS)
                }
                Log.d(TAG, "getDroidGuardResultAsync flow:$flow result:$result")
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                callback.onError(e)
                Log.w(TAG, "getDroidGuardResultAsync : ", e)
            }
            Log.d(TAG, "getDroidGuardResultAsync end " + (System.currentTimeMillis() - start))
        }
    }
}
