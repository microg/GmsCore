package com.android.vending

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream

object Util {
    private const val TAG = "FakeStoreUtil"

    /**
     * From [StackOverflow](https://stackoverflow.com/a/46688434/), CC BY-SA 4.0 by Sergey Frolov, adapted.
     */
    fun encodeGzip(input: ByteArray): ByteArray {
        try {
            ByteArrayOutputStream().use { byteOutput ->
                GZIPOutputStream(byteOutput).use { gzipOutput ->
                    gzipOutput.write(input)
                    gzipOutput.finish()
                    return byteOutput.toByteArray()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to encode bytes as GZIP")
            return ByteArray(0)
        }
    }
}
