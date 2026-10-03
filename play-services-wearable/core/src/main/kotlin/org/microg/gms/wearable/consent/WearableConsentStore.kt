/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable.consent

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import java.io.Closeable
import java.io.File

/** Presence records explicit global terms consent, without account, watch or optional service consent. */
data class WearableConsentRecord(val acceptedAtMillis: Long)

/** SQLite supplies transactions and fresh reads between the UI and wearable service processes. */
class WearableConsentStore(context: Context) : Closeable {
    private val helper = object : SQLiteOpenHelper(context, path(context), null, 1) {
        override fun onConfigure(db: SQLiteDatabase) {
            // A successful transaction must not acknowledge a write that exists only in memory.
            db.execSQL("PRAGMA synchronous=FULL")
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE consent (id INTEGER PRIMARY KEY CHECK(id = 1), accepted_at INTEGER NOT NULL CHECK(accepted_at > 0))")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            throw IllegalStateException("Unsupported wearable consent schema")
        }
    }

    fun read(): WearableConsentRecord? {
        helper.readableDatabase.query("consent", arrayOf("accepted_at"), "id = 1", null, null, null, null).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val timestamp = cursor.getLong(0)
            check(timestamp > 0 && !cursor.moveToNext()) { "Invalid wearable consent record" }
            return WearableConsentRecord(timestamp)
        }
    }

    /** Returns only after a durable transaction; I/O and schema errors propagate to the caller. */
    fun acceptTerms(beforeCommit: () -> Unit) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val timestamp = System.currentTimeMillis()
            check(timestamp > 0) { "Invalid device time" }
            val values = ContentValues().apply { put("id", 1); put("accepted_at", timestamp) }
            // Repeating an explicit decision preserves the first acceptance rather than refreshing it.
            db.insertWithOnConflict("consent", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            check(read() != null) { "Wearable consent was not saved" }
            beforeCommit()
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun close() { helper.close() }

    companion object {
        private fun path(context: Context): String {
            check(Build.VERSION.SDK_INT >= 21) { "Private consent storage unavailable" }
            return File(context.noBackupFilesDir, "wearable-consent.db").absolutePath
        }
    }
}
