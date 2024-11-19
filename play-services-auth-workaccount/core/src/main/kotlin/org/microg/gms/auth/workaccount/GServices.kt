package org.microg.gms.auth.workaccount

import android.content.ContentResolver
import android.net.Uri

// TODO: Deduplicate & Move
object GServices {
    private val CONTENT_URI: Uri = Uri.parse("content://com.google.android.gsf.gservices")

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
}