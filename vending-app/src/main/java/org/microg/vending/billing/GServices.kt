package org.microg.vending.billing

import android.content.ContentResolver
import android.net.Uri
import com.android.vending.BuildConfig

// TODO: Move
object GServices {
    // This fork hosts its own GServices provider under the renamed base package; fall back
    // to the real Google Services Framework provider when present (e.g. stock devices).
    private val MICROG_CONTENT_URI: Uri =
        Uri.parse("content://${BuildConfig.BASE_PACKAGE_NAME}.android.gsf.gservices")
    private val GSF_CONTENT_URI: Uri = Uri.parse("content://com.google.android.gsf.gservices")

    fun getString(resolver: ContentResolver, key: String, defaultValue: String?): String? {
        return queryString(resolver, MICROG_CONTENT_URI, key)
            ?: queryString(resolver, GSF_CONTENT_URI, key)
            ?: defaultValue
    }

    private fun queryString(resolver: ContentResolver, uri: Uri, key: String): String? {
        val cursor = resolver.query(uri, null, null, arrayOf(key), null) ?: return null
        return cursor.use {
            if (it.moveToNext()) it.getString(1) else null
        }
    }
}
