package org.microg.gms.constellation

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri

// TODO: This is taken from vending-app, can we have a common client for GServices please?

object GServices {
    private val CONTENT_URI: Uri = "content://com.google.android.gsf.gservices".toUri()

    fun getString(resolver: ContentResolver, key: String, defaultValue: String?): String? {
        var result = defaultValue
        val cursor = resolver.query(CONTENT_URI, null, null, arrayOf(key), null)
        cursor?.use {
            if (cursor.moveToNext()) {
                result = cursor.getString(1)
            }
        }
        return result
    }

    fun getLong(resolver: ContentResolver, key: String, defaultValue: Long): Long {
        val result = getString(resolver, key, null)
        if (result != null) {
            try {
                return result.toLong()
            } catch (_: NumberFormatException) {
            }
        }
        return defaultValue
    }
}
