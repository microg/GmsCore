/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.safetynet

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import com.google.android.gms.common.api.Status

class SafetyNetDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    init {
        if (SDK_INT >= 16) {
            setWriteAheadLoggingEnabled(true)
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        if (!db.isReadOnly) clearOldRequests(db)
    }

    private fun createSafetyNetSummary(cursor: Cursor): SafetyNetSummary {
        val summary = SafetyNetSummary(
            SafetyNetRequestType.valueOf(
                cursor.getString(cursor.getColumnIndexOrThrow(FIELD_REQUEST_TYPE))
            ),
            cursor.getString(cursor.getColumnIndexOrThrow(FIELD_PACKAGE_NAME)),
            cursor.getBlob(cursor.getColumnIndexOrThrow(FIELD_NONCE)),
            cursor.getLong(cursor.getColumnIndexOrThrow(FIELD_TIMESTAMP))
        )
        summary.id = cursor.getInt(cursor.getColumnIndexOrThrow(FIELD_ID))
        if (cursor.isNull(cursor.getColumnIndexOrThrow(FIELD_RESULT_STATUS_CODE))) return summary
        summary.responseStatus = Status(
            cursor.getInt(cursor.getColumnIndexOrThrow(FIELD_RESULT_STATUS_CODE)),
            cursor.getString(cursor.getColumnIndexOrThrow(FIELD_RESULT_STATUS_MSG))
        )
        summary.responseData = cursor.getString(cursor.getColumnIndexOrThrow(FIELD_RESULT_DATA))
        return summary
    }

    val recentApps: List<Pair<String, Long>>
        get() {
            val db = readableDatabase
            val cursor = db.query(TABLE_RECENTS, arrayOf(FIELD_PACKAGE_NAME, "MAX($FIELD_TIMESTAMP)"), null, null, FIELD_PACKAGE_NAME, null, "MAX($FIELD_TIMESTAMP) DESC")
            if (cursor != null) {
                val result = ArrayList<Pair<String, Long>>()
                while (cursor.moveToNext()) {
                    result.add(cursor.getString(0) to cursor.getLong(1))
                }
                cursor.close()
                return result
            }
            return emptyList()
        }

    fun getRecentRequests(packageName: String): List<SafetyNetSummary> {
        val db = readableDatabase
        val cursor = db.query(TABLE_RECENTS, null, "$FIELD_PACKAGE_NAME = ?", arrayOf(packageName), null, null, "$FIELD_TIMESTAMP DESC")
        if (cursor != null) {
            val result: MutableList<SafetyNetSummary> = ArrayList()
            while (cursor.moveToNext()) {
                result.add(createSafetyNetSummary(cursor))
            }
            cursor.close()
            return result
        }
        return emptyList()
    }

    fun insertRecentRequestStart(
        requestType: SafetyNetRequestType,
        packageName: String?,
        nonce: ByteArray?,
        timestamp: Long
    ): Long {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(FIELD_REQUEST_TYPE, requestType.name)
        cv.put(FIELD_PACKAGE_NAME, packageName)
        cv.put(FIELD_NONCE, nonce)
        cv.put(FIELD_TIMESTAMP, timestamp)
        return db.insert(TABLE_RECENTS, null, cv)
    }

    fun insertRecentRequestEnd(id: Long, status: Status, resultData: String?) {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(FIELD_RESULT_STATUS_CODE, status.statusCode)
        cv.put(FIELD_RESULT_STATUS_MSG, status.statusMessage)
        cv.put(FIELD_RESULT_DATA, resultData)
        db.update(TABLE_RECENTS, cv, "$FIELD_ID = ?", arrayOf(id.toString()))
    }

    fun clearOldRequests(db: SQLiteDatabase) {
        val timeout = 1000 * 60 * 60 * 24 * 14 // 14 days
        val maxRequests = 150
        var rows = 0

        rows += db.compileStatement(
            "DELETE FROM $TABLE_RECENTS WHERE $FIELD_ID NOT IN " +
                    "(SELECT $FIELD_ID FROM $TABLE_RECENTS ORDER BY $FIELD_TIMESTAMP LIMIT $maxRequests)"
        ).executeUpdateDelete()

        val sqLiteStatement = db.compileStatement("DELETE FROM $TABLE_RECENTS WHERE $FIELD_TIMESTAMP + ? < ?")
        sqLiteStatement.bindLong(1, timeout.toLong())
        sqLiteStatement.bindLong(2, System.currentTimeMillis())
        rows += sqLiteStatement.executeUpdateDelete()

        if (rows != 0) Log.d(TAG, "Cleared $rows old request(s)")
    }

    fun clearAllRequests() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_RECENTS")
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_RECENTS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException("Upgrades not supported")
    }

    companion object {
        private val TAG = SafetyNetDatabase::class.java.simpleName
        private const val DB_NAME = "snet.db"
        private const val DB_VERSION = 1
        private const val CREATE_TABLE_RECENTS = "CREATE TABLE recents (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT ," +
                "request_type TEXT," +
                "package_name TEXT," +
                "nonce TEXT," +
                "timestamp INTEGER," +
                "result_status_code INTEGER DEFAULT NULL," +
                "result_status_msg TEXT DEFAULT NULL," +
                "result_data TEXT DEFAULT NULL)"
        private const val TABLE_RECENTS = "recents"
        private const val FIELD_ID = "id"
        private const val FIELD_REQUEST_TYPE = "request_type"
        private const val FIELD_PACKAGE_NAME = "package_name"
        private const val FIELD_NONCE = "nonce"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_RESULT_STATUS_CODE = "result_status_code"
        private const val FIELD_RESULT_STATUS_MSG = "result_status_msg"
        private const val FIELD_RESULT_DATA = "result_data"
    }
}
