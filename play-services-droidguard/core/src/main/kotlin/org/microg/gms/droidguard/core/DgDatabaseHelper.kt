/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS main;");
        this.onCreate(db);
    }

    /**
     * @return vm key, byte code, extra
     */
    fun get(id: String): Triple<String, ByteArray, ByteArray>? = readableDatabase.use { db ->
        val time = System.currentTimeMillis() / 1000
        db.query("main", arrayOf("f", "d", "e", "c", "g"), "a = ? AND b <= $time AND $time < (b + c)", arrayOf(id), null, null, "b DESC", "1").use {
            if (it.moveToNext()) {
                Triple(it.getString(1), it.getBlob(0), it.getBlob(4))
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
        writableDatabase.use {
            it.beginTransaction()
            if (expiry <= 0) {
                it.delete("main", "a = ?", arrayOf(id))
            } else if (it.update("main", dbData, "a = ?", arrayOf(id)) <= 0) {
                it.insert("main", null, dbData)
            }
            it.setTransactionSuccessful()
            it.endTransaction()
        }
    }
}
