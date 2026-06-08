/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ads.rewarded

import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.internal.AdapterResponseInfoParcel
import com.google.android.gms.ads.internal.client.IResponseInfo

private const val TAG = "RewardedAdResponseInfo"

class ResponseInfoImpl : IResponseInfo.Stub() {
    override fun getMediationAdapterClassName(): String? {
        Log.d(TAG, "getMediationAdapterClassName")
        return null
    }

    override fun getResponseId(): String? {
        Log.d(TAG, "getResponseId")
        return null
    }

    override fun getAdapterResponseInfo(): List<AdapterResponseInfoParcel> {
        Log.d(TAG, "getAdapterResponseInfo")
        return arrayListOf()
    }

    override fun getLoadedAdapterResponse(): AdapterResponseInfoParcel? {
        Log.d(TAG, "getLoadedAdapterResponse")
        return null
    }

    override fun getResponseExtras(): Bundle {
        Log.d(TAG, "getResponseExtras")
        return Bundle()
    }
}