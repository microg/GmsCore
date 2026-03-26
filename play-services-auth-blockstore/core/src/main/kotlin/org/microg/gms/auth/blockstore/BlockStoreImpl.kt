/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.blockstore

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.google.android.gms.auth.blockstore.BlockstoreClient
import com.google.android.gms.auth.blockstore.BlockstoreStatusCodes
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesResponse
import com.google.android.gms.auth.blockstore.StoreBytesData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.utils.toBase64

private const val SHARED_PREFS_NAME = "com.google.android.gms.blockstore"

private const val TAG = "BlockStoreImpl"

class BlockStoreImpl(context: Context, val callerPackage: String) {

    private val blockStoreSp: SharedPreferences by lazy {
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun initSpByPackage(): Map<String, *>? {
        val map = blockStoreSp.all
        if (map.isNullOrEmpty() || map.all { !it.key.startsWith(callerPackage) }) return null
        return map.filter { it.key.startsWith(callerPackage) }
    }

    suspend fun deleteBytesWithRequest(request: DeleteBytesRequest?): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "deleteBytesWithRequest: callerPackage: $callerPackage")
        val localData = initSpByPackage()
        if (request == null || localData.isNullOrEmpty()) return@withContext false
        if (request.deleteAll) {
            localData.keys.forEach { blockStoreSp.edit()?.remove(it)?.commit() }
        } else {
            request.keys.forEach { blockStoreSp.edit()?.remove("$callerPackage:$it")?.commit() }
        }
        true
    }

    suspend fun retrieveBytesWithRequest(request: RetrieveBytesRequest?): RetrieveBytesResponse? = withContext(Dispatchers.IO) {
        Log.d(TAG, "retrieveBytesWithRequest: callerPackage: $callerPackage")
        val localData = initSpByPackage()
        if (request == null || localData.isNullOrEmpty()) return@withContext null
        val data = mutableListOf<RetrieveBytesResponse.BlockstoreData>()
        val filterKeys = if (request.keys.isNullOrEmpty()) emptyList<String>() else request.keys
        for (key in localData.keys) {
            val bytesKey = key.substring(callerPackage.length + 1)
            if (filterKeys.isNotEmpty() && !filterKeys.contains(bytesKey)) continue
            val bytes = blockStoreSp.getString(key, null)?.let { Base64.decode(it, Base64.URL_SAFE) } ?: continue
            data.add(RetrieveBytesResponse.BlockstoreData(bytes, bytesKey))
        }
        RetrieveBytesResponse(Bundle.EMPTY, data)
    }

    suspend fun retrieveBytes(): ByteArray? = withContext(Dispatchers.IO) {
        Log.d(TAG, "retrieveBytes: callerPackage: $callerPackage")
        val localData = initSpByPackage()
        if (localData.isNullOrEmpty()) return@withContext null
        val savedKey = localData.keys.firstOrNull { it == "$callerPackage:${BlockstoreClient.DEFAULT_BYTES_DATA_KEY}" } ?: return@withContext null
        blockStoreSp.getString(savedKey, null)?.let { Base64.decode(it, Base64.URL_SAFE) }
    }

    suspend fun storeBytes(data: StoreBytesData?): Int = withContext(Dispatchers.IO) {
        if (data == null || data.bytes == null) return@withContext 0
        val localData = initSpByPackage()
        if ((localData?.size ?: 0) >= BlockstoreClient.MAX_ENTRY_COUNT) {
            return@withContext BlockstoreStatusCodes.TOO_MANY_ENTRIES
        }
        val bytes = data.bytes
        if (bytes.size > BlockstoreClient.MAX_SIZE) {
            return@withContext BlockstoreStatusCodes.MAX_SIZE_EXCEEDED
        }
        val savedKey = "$callerPackage:${data.key ?: BlockstoreClient.DEFAULT_BYTES_DATA_KEY}"
        val base64 = bytes.toBase64(Base64.URL_SAFE)
        val bool = blockStoreSp.edit()?.putString(savedKey, base64)?.commit()
        if (bool == true) bytes.size else 0
    }
}