/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import android.util.Log
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull

class LocationAppsDatabase(context: Context) : SQLiteOpenHelper(context, "geoapps.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_APPS($FIELD_PACKAGE TEXT NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_FORCE_COARSE INTEGER);")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_APPS_LAST_LOCATION($FIELD_PACKAGE TEXT NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_LATITUDE REAL NOT NULL, $FIELD_LONGITUDE REAL NOT NULL, $FIELD_ACCURACY REAL NOT NULL, $FIELD_PROVIDER TEXT NOT NULL);")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${TABLE_APPS}_index ON ${TABLE_APPS}(${FIELD_PACKAGE});")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${TABLE_APPS_LAST_LOCATION}_index ON ${TABLE_APPS_LAST_LOCATION}(${FIELD_PACKAGE});")
    }

    private fun insertOrUpdateApp(packageName: String, vararg pairs: Pair<String, Any?>) {
        val values = contentValuesOf(FIELD_PACKAGE to packageName, *pairs)
        if (writableDatabase.insertWithOnConflict(TABLE_APPS, null, values, SQLiteDatabase.CONFLICT_IGNORE) < 0) {
            writableDatabase.update(TABLE_APPS, values, "$FIELD_PACKAGE = ?", arrayOf(packageName))
        }
        close()
    }

    fun noteAppUsage(packageName: String) {
        insertOrUpdateApp(packageName, FIELD_TIME to System.currentTimeMillis())
    }

    fun getForceCoarse(packageName: String): Boolean {
        return readableDatabase.query(TABLE_APPS, arrayOf(FIELD_FORCE_COARSE), "$FIELD_PACKAGE = ?", arrayOf(packageName), null, null, null, "1").run {
            try {
                if (moveToNext()) {
                    getIntOrNull(0) == 1
                } else {
                    false
                }
            } finally {
                close()
            }
        }
    }

    fun setForceCoarse(packageName: String, forceCoarse: Boolean) {
        insertOrUpdateApp(packageName, FIELD_FORCE_COARSE to (if (forceCoarse) 1 else 0))
    }

    fun noteAppLocation(packageName: String, location: Location?) {
        noteAppUsage(packageName)
        if (location == null) return
        val values = contentValuesOf(
            FIELD_PACKAGE to packageName,
            FIELD_TIME to location.time,
            FIELD_LATITUDE to location.latitude,
            FIELD_LONGITUDE to location.longitude,
            FIELD_ACCURACY to location.accuracy,
            FIELD_PROVIDER to location.provider
        )
        writableDatabase.insertWithOnConflict(TABLE_APPS_LAST_LOCATION, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        close()
    }

    fun listAppsByAccessTime(limit: Int = Int.MAX_VALUE): List<Pair<String, Long>> {
        val res = arrayListOf<Pair<String, Long>>()
        readableDatabase.query(TABLE_APPS, arrayOf(FIELD_PACKAGE, FIELD_TIME), null, null, null, null, "$FIELD_TIME DESC", "$limit").apply {
            while (moveToNext()) {
                res.add(getString(0) to getLong(1))
            }
            close()
        }
        return res
    }

    fun getAppLocation(packageName: String): Location? {
        return readableDatabase.query(
            TABLE_APPS_LAST_LOCATION,
            arrayOf(FIELD_LATITUDE, FIELD_LONGITUDE, FIELD_ACCURACY, FIELD_TIME, FIELD_PROVIDER),
            "$FIELD_PACKAGE = ?",
            arrayOf(packageName),
            null,
            null,
            null,
            "1"
        ).run {
            try {
                if (moveToNext()) {
                    Location(getString(4)).also {
                        it.latitude = getDouble(0)
                        it.longitude = getDouble(1)
                        it.accuracy = getFloat(2)
                        it.time = getLong(3)
                    }
                } else {
                    null
                }
            } finally {
                close()
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_APPS ADD COLUMN IF NOT EXISTS $FIELD_FORCE_COARSE INTEGER;")
            } catch (ignored: Exception) {
                // Ignoring
            }
        }
    }

    companion object {
        private const val TABLE_APPS = "apps"
        private const val TABLE_APPS_LAST_LOCATION = "app_location"
        private const val FIELD_PACKAGE = "package"
        private const val FIELD_FORCE_COARSE = "force_coarse"
        private const val FIELD_LATITUDE = "lat"
        private const val FIELD_LONGITUDE = "lon"
        private const val FIELD_ACCURACY = "acc"
        private const val FIELD_TIME = "time"
        private const val FIELD_PROVIDER = "provider"
    }
}