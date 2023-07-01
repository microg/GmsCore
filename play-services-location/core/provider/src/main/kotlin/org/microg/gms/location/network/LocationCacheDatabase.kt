/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import androidx.core.content.contentValuesOf
import androidx.core.os.bundleOf
import org.microg.gms.location.network.cell.CellDetails
import org.microg.gms.location.network.cell.isValid
import org.microg.gms.location.network.wifi.WifiDetails
import org.microg.gms.location.network.wifi.isRequestable
import org.microg.gms.location.network.wifi.macBytes
import org.microg.gms.utils.toHexString
import java.io.PrintWriter
import kotlin.math.max

internal class LocationCacheDatabase(context: Context?) : SQLiteOpenHelper(context, "geocache.db", null, 3) {
    fun getCellLocation(cell: CellDetails, allowLearned: Boolean = true): Location? {
        val cellLocation = readableDatabase.query(
            TABLE_CELLS,
            arrayOf(FIELD_LATITUDE, FIELD_LONGITUDE, FIELD_ACCURACY, FIELD_TIME, FIELD_PRECISION),
            CELLS_SELECTION,
            getCellSelectionArgs(cell),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getLocation(MAX_CACHE_AGE)
            } else {
                null
            }
        }
        if (cellLocation?.precision?.let { it >= 1f } == true) return cellLocation
        if (allowLearned) {
            readableDatabase.query(
                TABLE_CELLS_LEARN,
                arrayOf(FIELD_LATITUDE_HIGH, FIELD_LATITUDE_LOW, FIELD_LONGITUDE_HIGH, FIELD_LONGITUDE_LOW, FIELD_TIME, FIELD_BAD_TIME),
                CELLS_SELECTION,
                getCellSelectionArgs(cell),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToNext()) {
                    val badTime = cursor.getLong(5)
                    val time = cursor.getLong(4)
                    if (badTime > time - LEARN_BAD_CUTOFF) return@use
                    cursor.getMidLocation(MAX_CACHE_AGE, LEARN_BASE_ACCURACY_CELL)?.let { return it }
                }
            }
        }
        if (cellLocation != null) return cellLocation
        readableDatabase.query(TABLE_CELLS_PRE, arrayOf(FIELD_TIME), CELLS_PRE_SELECTION, getCellPreSelectionArgs(cell), null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                if (cursor.getLong(1) > System.currentTimeMillis() - MAX_CACHE_AGE) {
                    return NEGATIVE_CACHE_ENTRY
                }
            }
        }
        return null
    }

    fun getWifiLocation(wifi: WifiDetails): Location? {
        val wifiLocation = readableDatabase.query(
            TABLE_WIFIS,
            arrayOf(FIELD_LATITUDE, FIELD_LONGITUDE, FIELD_ACCURACY, FIELD_TIME, FIELD_PRECISION),
            getWifiSelection(wifi),
            null,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getLocation(MAX_CACHE_AGE)
            } else {
                null
            }
        }
        if (wifiLocation?.precision?.let { it >= 1f } == true) return wifiLocation
        readableDatabase.query(
            TABLE_WIFI_LEARN,
            arrayOf(FIELD_LATITUDE_HIGH, FIELD_LATITUDE_LOW, FIELD_LONGITUDE_HIGH, FIELD_LONGITUDE_LOW, FIELD_TIME, FIELD_BAD_TIME),
            getWifiSelection(wifi),
            null,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToNext()) {
                val badTime = cursor.getLong(5)
                val time = cursor.getLong(4)
                if (badTime > time - LEARN_BAD_CUTOFF) return@use
                cursor.getMidLocation(MAX_CACHE_AGE, LEARN_BASE_ACCURACY_WIFI, 0.5)?.let { return it }
            }
        }
        return wifiLocation
    }

    fun putCellLocation(cell: CellDetails, location: Location) {
        if (!cell.isValid) return
        val cv = contentValuesOf(
            FIELD_MCC to cell.mcc,
            FIELD_MNC to cell.mnc,
            FIELD_LAC_TAC to (cell.lac ?: cell.tac ?: 0),
            FIELD_TYPE to cell.type.ordinal,
            FIELD_CID to cell.cid,
            FIELD_PSC to (cell.psc ?: 0)
        ).apply { putLocation(location) }
        writableDatabase.insertWithOnConflict(TABLE_CELLS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun learnCellLocation(cell: CellDetails, location: Location) {
        if (!cell.isValid) return
        val (exists, isBad) = readableDatabase.query(
            TABLE_CELLS_LEARN,
            arrayOf(FIELD_LATITUDE_HIGH, FIELD_LATITUDE_LOW, FIELD_LONGITUDE_HIGH, FIELD_LONGITUDE_LOW, FIELD_TIME),
            CELLS_SELECTION,
            getCellSelectionArgs(cell),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToNext()) {
                val midLocation = cursor.getMidLocation(Long.MAX_VALUE, 0f)
                (midLocation != null) to (midLocation?.let { it.distanceTo(location) > LEARN_BAD_SIZE_CELL } == true)
            } else {
                false to false
            }
        }
        if (exists && isBad) {
            writableDatabase.update(
                TABLE_CELLS_LEARN, contentValuesOf(
                FIELD_LATITUDE_HIGH to location.latitude,
                FIELD_LATITUDE_LOW to location.latitude,
                FIELD_LONGITUDE_HIGH to location.longitude,
                FIELD_LONGITUDE_LOW to location.longitude,
                FIELD_TIME to location.time,
                FIELD_BAD_TIME to location.time
            ), CELLS_SELECTION, getCellSelectionArgs(cell)
            )
        } else if (!exists) {
            writableDatabase.insertWithOnConflict(
                TABLE_CELLS_LEARN, null, contentValuesOf(
                FIELD_MCC to cell.mcc,
                FIELD_MNC to cell.mnc,
                FIELD_LAC_TAC to (cell.lac ?: cell.tac ?: 0),
                FIELD_TYPE to cell.type.ordinal,
                FIELD_CID to cell.cid,
                FIELD_PSC to (cell.psc ?: 0),
                FIELD_LATITUDE_HIGH to location.latitude,
                FIELD_LATITUDE_LOW to location.latitude,
                FIELD_LONGITUDE_HIGH to location.longitude,
                FIELD_LONGITUDE_LOW to location.longitude,
                FIELD_TIME to location.time,
                FIELD_BAD_TIME to 0
            ), SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            writableDatabase.rawQuery("UPDATE $TABLE_CELLS_LEARN SET $FIELD_LATITUDE_HIGH = max($FIELD_LATITUDE_HIGH, ?), $FIELD_LATITUDE_LOW = min($FIELD_LATITUDE_LOW, ?), $FIELD_LONGITUDE_HIGH = max($FIELD_LONGITUDE_HIGH, ?), $FIELD_LONGITUDE_LOW = min($FIELD_LONGITUDE_LOW, ?), $FIELD_TIME = ? WHERE $CELLS_SELECTION", arrayOf(location.latitude.toString(), location.latitude.toString(), location.longitude.toString(), location.longitude.toString()) + getCellSelectionArgs(cell)).close()
        }
    }

    fun learnWifiLocation(wifi: WifiDetails, location: Location) {
        if (!wifi.isRequestable) return
        val (exists, isBad) = readableDatabase.query(
            TABLE_WIFI_LEARN,
            arrayOf(FIELD_LATITUDE_HIGH, FIELD_LATITUDE_LOW, FIELD_LONGITUDE_HIGH, FIELD_LONGITUDE_LOW, FIELD_TIME),
            getWifiSelection(wifi),
            null,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToNext()) {
                val midLocation = cursor.getMidLocation(Long.MAX_VALUE, 0f)
                (midLocation != null) to (midLocation?.let { it.distanceTo(location) > LEARN_BAD_SIZE_WIFI } == true)
            } else {
                false to false
            }
        }
        if (exists && isBad) {
            writableDatabase.update(
                TABLE_WIFI_LEARN, contentValuesOf(
                FIELD_LATITUDE_HIGH to location.latitude,
                FIELD_LATITUDE_LOW to location.latitude,
                FIELD_LONGITUDE_HIGH to location.longitude,
                FIELD_LONGITUDE_LOW to location.longitude,
                FIELD_TIME to location.time,
                FIELD_BAD_TIME to location.time
            ), getWifiSelection(wifi), null)
        } else if (!exists) {
            writableDatabase.insertWithOnConflict(
                TABLE_WIFI_LEARN, null, contentValuesOf(
                FIELD_MAC to wifi.macBytes,
                FIELD_LATITUDE_HIGH to location.latitude,
                FIELD_LATITUDE_LOW to location.latitude,
                FIELD_LONGITUDE_HIGH to location.longitude,
                FIELD_LONGITUDE_LOW to location.longitude,
                FIELD_TIME to location.time,
                FIELD_BAD_TIME to 0
            ), SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            writableDatabase.rawQuery("UPDATE $TABLE_WIFI_LEARN SET $FIELD_LATITUDE_HIGH = max($FIELD_LATITUDE_HIGH, ?), $FIELD_LATITUDE_LOW = min($FIELD_LATITUDE_LOW, ?), $FIELD_LONGITUDE_HIGH = max($FIELD_LONGITUDE_HIGH, ?), $FIELD_LONGITUDE_LOW = min($FIELD_LONGITUDE_LOW, ?), $FIELD_TIME = ? WHERE ${getWifiSelection(wifi)}", arrayOf(location.latitude.toString(), location.latitude.toString(), location.longitude.toString(), location.longitude.toString())).close()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CELLS($FIELD_MCC INTEGER NOT NULL, $FIELD_MNC INTEGER NOT NULL, $FIELD_TYPE INTEGER NOT NULL, $FIELD_LAC_TAC INTEGER NOT NULL, $FIELD_CID INTEGER NOT NULL, $FIELD_PSC INTEGER NOT NULL, $FIELD_LATITUDE REAL NOT NULL, $FIELD_LONGITUDE REAL NOT NULL, $FIELD_ACCURACY REAL NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_PRECISION REAL NOT NULL);")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CELLS_PRE($FIELD_MCC INTEGER NOT NULL, $FIELD_MNC INTEGER NOT NULL, $FIELD_TIME INTEGER NOT NULL);")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_WIFIS($FIELD_MAC BLOB, $FIELD_LATITUDE REAL NOT NULL, $FIELD_LONGITUDE REAL NOT NULL, $FIELD_ACCURACY REAL NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_PRECISION REAL NOT NULL);")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CELLS_LEARN($FIELD_MCC INTEGER NOT NULL, $FIELD_MNC INTEGER NOT NULL, $FIELD_TYPE INTEGER NOT NULL, $FIELD_LAC_TAC INTEGER NOT NULL, $FIELD_CID INTEGER NOT NULL, $FIELD_PSC INTEGER NOT NULL, $FIELD_LATITUDE_HIGH REAL NOT NULL, $FIELD_LATITUDE_LOW REAL NOT NULL, $FIELD_LONGITUDE_HIGH REAL NOT NULL, $FIELD_LONGITUDE_LOW REAL NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_BAD_TIME INTEGER);")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_WIFI_LEARN($FIELD_MAC BLOB, $FIELD_LATITUDE_HIGH REAL NOT NULL, $FIELD_LATITUDE_LOW REAL NOT NULL, $FIELD_LONGITUDE_HIGH REAL NOT NULL, $FIELD_LONGITUDE_LOW REAL NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_BAD_TIME INTEGER);")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${TABLE_CELLS}_index ON $TABLE_CELLS($FIELD_MCC, $FIELD_MNC, $FIELD_TYPE, $FIELD_LAC_TAC, $FIELD_CID, $FIELD_PSC);")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${TABLE_CELLS_PRE}_index ON $TABLE_CELLS_PRE($FIELD_MCC, $FIELD_MNC);")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${TABLE_WIFIS}_index ON $TABLE_WIFIS($FIELD_MAC);")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${TABLE_CELLS_LEARN}_index ON $TABLE_CELLS_LEARN($FIELD_MCC, $FIELD_MNC, $FIELD_TYPE, $FIELD_LAC_TAC, $FIELD_CID, $FIELD_PSC);")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${TABLE_WIFI_LEARN}_index ON $TABLE_WIFI_LEARN($FIELD_MAC);")
        db.execSQL("CREATE INDEX IF NOT EXISTS ${TABLE_CELLS}_time_index ON $TABLE_CELLS($FIELD_TIME);")
        db.execSQL("CREATE INDEX IF NOT EXISTS ${TABLE_CELLS_PRE}_time_index ON $TABLE_CELLS_PRE($FIELD_TIME);")
        db.execSQL("CREATE INDEX IF NOT EXISTS ${TABLE_WIFIS}_time_index ON $TABLE_WIFIS($FIELD_TIME);")
        db.execSQL("CREATE INDEX IF NOT EXISTS ${TABLE_CELLS_LEARN}_time_index ON $TABLE_CELLS_LEARN($FIELD_TIME);")
        db.execSQL("CREATE INDEX IF NOT EXISTS ${TABLE_WIFI_LEARN}_time_index ON $TABLE_WIFI_LEARN($FIELD_TIME);")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WIFI_SCANS;")
    }

    fun cleanup(db: SQLiteDatabase) {
        db.delete(TABLE_CELLS, "$FIELD_TIME < ?", arrayOf((System.currentTimeMillis() - MAX_CACHE_AGE).toString()))
        db.delete(TABLE_CELLS_PRE, "$FIELD_TIME < ?", arrayOf((System.currentTimeMillis() - MAX_CACHE_AGE).toString()))
        db.delete(TABLE_WIFIS, "$FIELD_TIME < ?", arrayOf((System.currentTimeMillis() - MAX_CACHE_AGE).toString()))
        db.delete(TABLE_CELLS_LEARN, "$FIELD_TIME < ?", arrayOf((System.currentTimeMillis() - MAX_LEARN_AGE).toString()))
        db.delete(TABLE_WIFI_LEARN, "$FIELD_TIME < ?", arrayOf((System.currentTimeMillis() - MAX_LEARN_AGE).toString()))
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            cleanup(db)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    fun dump(writer: PrintWriter) {
        writer.println("Cache: cells(fetched)=${DatabaseUtils.queryNumEntries(readableDatabase, TABLE_CELLS)}, cells(learnt)=${DatabaseUtils.queryNumEntries(readableDatabase, TABLE_CELLS_LEARN)}, wifis(fetched)=${DatabaseUtils.queryNumEntries(readableDatabase, TABLE_WIFIS)}, wifis(learnt)=${DatabaseUtils.queryNumEntries(readableDatabase, TABLE_WIFI_LEARN)}")
    }

    companion object {
        const val PROVIDER_CACHE = "cache"
        const val EXTRA_HIGH_LOCATION = "high"
        const val EXTRA_LOW_LOCATION = "low"
        const val DEBUG = false
        val NEGATIVE_CACHE_ENTRY = Location(PROVIDER_CACHE)
        private const val MAX_CACHE_AGE = 1000L * 60 * 60 * 24 * 14 // 14 days
        private const val MAX_LEARN_AGE = 1000L * 60 * 60 * 24 * 365 // 1 year
        private const val TABLE_CELLS = "cells"
        private const val TABLE_CELLS_PRE = "cells_pre"
        private const val TABLE_WIFIS = "wifis"
        private const val TABLE_WIFI_SCANS = "wifi_scans"
        private const val TABLE_CELLS_LEARN = "cells_learn"
        private const val TABLE_WIFI_LEARN = "wifis_learn"
        private const val FIELD_MCC = "mcc"
        private const val FIELD_MNC = "mnc"
        private const val FIELD_TYPE = "type"
        private const val FIELD_LAC_TAC = "lac"
        private const val FIELD_CID = "cid"
        private const val FIELD_PSC = "psc"
        private const val FIELD_LATITUDE = "lat"
        private const val FIELD_LONGITUDE = "lon"
        private const val FIELD_ACCURACY = "acc"
        private const val FIELD_TIME = "time"
        private const val FIELD_PRECISION = "prec"
        private const val FIELD_MAC = "mac"
        private const val FIELD_SCAN_HASH = "hash"
        private const val FIELD_LATITUDE_HIGH = "lath"
        private const val FIELD_LATITUDE_LOW = "latl"
        private const val FIELD_LONGITUDE_HIGH = "lonh"
        private const val FIELD_LONGITUDE_LOW = "lonl"
        private const val FIELD_BAD_TIME = "btime"
        private const val LEARN_BASE_ACCURACY_CELL = 5_000f
        private const val LEARN_BASE_ACCURACY_WIFI = 100f
        private const val LEARN_BAD_SIZE_CELL = 10_000
        private const val LEARN_BAD_SIZE_WIFI = 200
        private const val LEARN_BAD_CUTOFF = 1000L * 60 * 60 * 24 * 14
        private const val CELLS_SELECTION = "$FIELD_MCC = ? AND $FIELD_MNC = ? AND $FIELD_TYPE = ? AND $FIELD_LAC_TAC = ? AND $FIELD_CID = ? AND $FIELD_PSC = ?"
        private const val CELLS_PRE_SELECTION = "$FIELD_MCC = ? AND $FIELD_MNC = ?"
        private fun getCellSelectionArgs(cell: CellDetails): Array<String> {
            return arrayOf(
                cell.mcc.toString(),
                cell.mnc.toString(),
                cell.type.ordinal.toString(),
                (cell.lac ?: cell.tac ?: 0).toString(),
                cell.cid.toString(),
                (cell.psc ?: 0).toString(),
            )
        }

        private fun getWifiSelection(wifi: WifiDetails): String {
            return "$FIELD_MAC = x'${wifi.macBytes.toHexString()}'"
        }

        private fun getCellPreSelectionArgs(cell: CellDetails): Array<String> {
            return arrayOf(
                cell.mcc.toString(),
                cell.mnc.toString()
            )
        }

        private fun Cursor.getLocation(maxAge: Long): Location? {
            if (getLong(3) > System.currentTimeMillis() - maxAge) {
                if (getDouble(2) == 0.0) return NEGATIVE_CACHE_ENTRY
                return Location(PROVIDER_CACHE).apply {
                    latitude = getDouble(0)
                    longitude = getDouble(1)
                    accuracy = getDouble(2).toFloat()
                    precision = getDouble(4)
                }
            }
            return null
        }

        private fun Cursor.getMidLocation(maxAge: Long, minAccuracy: Float, precision: Double = 1.0): Location? {
            if (maxAge == Long.MAX_VALUE || getLong(4) > System.currentTimeMillis() - maxAge) {
                val high = Location(PROVIDER_CACHE).apply { latitude = getDouble(0); longitude = getDouble(2) }
                val low = Location(PROVIDER_CACHE).apply { latitude = getDouble(1); longitude = getDouble(3) }
                return Location(PROVIDER_CACHE).apply {
                    latitude = (high.latitude + low.latitude) / 2.0
                    longitude = (high.longitude + low.longitude) / 2.0
                    accuracy = max(high.distanceTo(low), minAccuracy)
                    this.precision = precision
                    if (DEBUG) {
                        extras = bundleOf(
                            EXTRA_HIGH_LOCATION to high,
                            EXTRA_LOW_LOCATION to low
                        )
                    }
                }
            }
            return null
        }

        private fun ContentValues.putLocation(location: Location) {
            if (location != NEGATIVE_CACHE_ENTRY) {
                put(FIELD_LATITUDE, location.latitude)
                put(FIELD_LONGITUDE, location.longitude)
                put(FIELD_ACCURACY, location.accuracy)
                put(FIELD_TIME, location.time)
                put(FIELD_PRECISION, location.precision)
            } else {
                put(FIELD_LATITUDE, 0.0)
                put(FIELD_LONGITUDE, 0.0)
                put(FIELD_ACCURACY, 0.0)
                put(FIELD_TIME, System.currentTimeMillis())
                put(FIELD_PRECISION, 0.0)
            }
        }
    }
}