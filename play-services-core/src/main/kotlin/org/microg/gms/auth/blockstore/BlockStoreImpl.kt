/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.blockstore

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.google.android.gms.auth.blockstore.BlockstoreClient
import com.google.android.gms.auth.blockstore.StoreBytesData
import org.microg.gms.utils.toBase64

private const val SHARED_PREFS_NAME = "com.google.android.gms.blockstore"

private const val TAG = "BlockStoreImpl"

class BlockStoreImpl(context: Context, val callerPackage: String) {

    private val blockStoreSp: SharedPreferences by lazy {
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun retrieveBytes(): ByteArray? {
        Log.d(TAG, "retrieveBytes: callerPackage: $callerPackage")
        val map = blockStoreSp.all
        if (map.isNullOrEmpty()) return null
        if (map.all { !it.key.startsWith(callerPackage) }) return null
        val savedKey = map.keys.firstOrNull { it == "$callerPackage:${BlockstoreClient.DEFAULT_BYTES_DATA_KEY}" } ?: map.keys.firstOrNull { it.startsWith(callerPackage) } ?: return null
        val base64 = blockStoreSp.getString(savedKey, null)
        return Base64.decode(base64, Base64.URL_SAFE)
    }

    fun storeBytes(data: StoreBytesData?): Int {
        Log.d(TAG, "storeBytes: callerPackage: $callerPackage")
        if (data != null) {
            val savedKey = "$callerPackage:${data.key ?: BlockstoreClient.DEFAULT_BYTES_DATA_KEY}"
            val base64 = data.bytes.toBase64(Base64.URL_SAFE)
            blockStoreSp.edit()?.putString(savedKey, base64)?.apply()
            return data.bytes.size
        }
        return 0
    }
}