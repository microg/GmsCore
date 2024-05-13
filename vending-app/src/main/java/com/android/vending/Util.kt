package com.android.vending

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.os.Bundle
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "FakeStoreUtil"

/**
 * From [StackOverflow](https://stackoverflow.com/a/46688434/), CC BY-SA 4.0 by Sergey Frolov, adapted.
 */
fun ByteArray.encodeGzip(): ByteArray {
    try {
        ByteArrayOutputStream().use { byteOutput ->
            GZIPOutputStream(byteOutput).use { gzipOutput ->
                gzipOutput.write(this)
                gzipOutput.finish()
                return byteOutput.toByteArray()
            }
        }
    } catch (e: IOException) {
        Log.e(TAG, "Failed to encode bytes as GZIP")
        return ByteArray(0)
    }
}

suspend fun AccountManager.getAuthToken(account: Account, authTokenType: String, notifyAuthFailure: Boolean) =
    suspendCoroutine { continuation ->
        getAuthToken(account, authTokenType, notifyAuthFailure, { future: AccountManagerFuture<Bundle> ->
            try {
                val result = future.result
                continuation.resume(result)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, null)
    }