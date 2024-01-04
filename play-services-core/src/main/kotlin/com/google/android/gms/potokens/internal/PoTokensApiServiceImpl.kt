/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.potokens.internal

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.potokens.PoToken
import com.google.android.gms.potokens.utils.PoTokenHelper
import org.microg.gms.profile.ProfileManager

class PoTokensApiServiceImpl(
    private val context: Context,
    private val packageName: String,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope
) : IPoTokensService.Stub() {

    override fun responseStatus(call: IStatusCallback, code: Int) {
        Log.e(TAG, "responseStatus success")
        call.onResult(Status.SUCCESS)
    }

    override fun responseStatusToken(call: ITokenCallbacks, i: Int, bArr: ByteArray) {
        Log.d(TAG, "responseStatusToken packageName: $packageName")
        ProfileManager.ensureInitialized(context)

        lifecycleCoroutineScope.launchWhenCreated {
            Log.d(TAG, "responseStatusToken start")
            val bytes = PoTokenHelper.get(context).callPoToken(context, packageName, bArr)
            Log.d(TAG, "responseStatusToken result: ${bytes.size}")
            call.responseToken(Status.SUCCESS, PoToken(bytes))
            Log.d(TAG, "responseStatusToken end")
        }
    }

}