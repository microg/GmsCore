/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction

/**
 * - a: id
 * - b: timestamp
 * - c: seconds until expiry
 * - d: vm key
 * - e: ?
 * - f: byte code
 * - g: extra
 */
class DgDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "dg.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        // Note: "NON NULL" is actually not a valid sqlite constraint, but this is what we see in the original database 🤷
        db.execSQL("CREATE TABLE main (a TEXT NOT NULL, b LONG NOT NULL, c LONG NOT NULL, d TEXT NON NULL, e TEXT NON NULL,f BLOB NOT NULL,g BLOB NOT NULL);");
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.enableWriteAheadLogging()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS main;");
        this.onCreate(db);
    }

    private fun <T> retryOnLock(defaultValue: T, block: () -> T): T {
        var retries = 3
        while (retries > 0) {
            try {
                return block()
            } catch (_: SQLiteDatabaseLockedException) {
                retries--
                if (retries == 0) return defaultValue
                Thread.sleep(100)
            }
        }
        return defaultValue
    }

    /**
     * @return vm key, byte code, extra
     */
    fun get(id: String): Triple<String, ByteArray, ByteArray>? = retryOnLock(null) {
        val db = readableDatabase
        val time = System.currentTimeMillis() / 1000
        val cursor = db.query("main", arrayOf("f", "d", "e", "c", "g"), "a = ? AND b <= $time AND $time < (b + c)", arrayOf(id), null, null, "b DESC", "1")
        cursor.use { c ->
            if (c.moveToNext()) {
                Triple(c.getString(1), c.getBlob(0), c.getBlob(4))
            } else {
                null
            }
        }
    }

    fun put(id: String, expiry: Long, vmKey: String, byteCode: ByteArray, extra: ByteArray) {
        val dbData = ContentValues().apply {
            put("a", id)
            put("b", System.currentTimeMillis() / 1000)
            put("c", expiry)
            put("d", vmKey)
            put("e", "")
            put("f", byteCode)
            put("g", extra)
        }
        retryOnLock(Unit) {
            val db = writableDatabase
            db.transaction {
                if (expiry <= 0) {
                    delete("main", "a = ?", arrayOf(id))
                } else if (update("main", dbData, "a = ?", arrayOf(id)) <= 0) {
                    insert("main", null, dbData)
                } else {}
            }
        }
    }
}
