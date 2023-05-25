/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import org.microg.gms.location.network.cell.CellDetails
import org.microg.gms.location.network.cell.isValid
import org.microg.gms.location.network.wifi.WifiDetails
import org.microg.gms.utils.toHexString
import java.nio.ByteBuffer

internal class LocationCacheDatabase(context: Context?) : SQLiteOpenHelper(context, "geocache.db", null, 2) {
    fun getCellLocation(cell: CellDetails): Location? {
        readableDatabase.query(
            TABLE_CELLS,
            arrayOf(FIELD_LATITUDE, FIELD_LONGITUDE, FIELD_ACCURACY, FIELD_TIME, FIELD_PRECISION),
            CELLS_SELECTION,
            getCellSelectionArgs(cell),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getLocation(MAX_CELL_AGE).let { return it }
            }
        }
        readableDatabase.query(TABLE_CELLS_PRE, arrayOf(FIELD_TIME), CELLS_PRE_SELECTION, getCellPreSelectionArgs(cell), null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                if (cursor.getLong(1) > System.currentTimeMillis() - MAX_CELL_AGE) {
                    return NEGATIVE_CACHE_ENTRY
                }
            }
        }
        return null
    }

    fun putCellLocation(cell: CellDetails, location: Location) {
        if (!cell.isValid) return
        val cv = ContentValues().apply {
            put(FIELD_MCC, cell.mcc)
            put(FIELD_MNC, cell.mnc)
            put(FIELD_LAC_TAC, cell.lac ?: cell.tac ?: 0)
            put(FIELD_TYPE, cell.type.ordinal)
            put(FIELD_CID, cell.cid)
            put(FIELD_PSC, cell.psc ?: 0)
            putLocation(location)
        }
        writableDatabase.insertWithOnConflict(TABLE_CELLS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getWifiScanLocation(wifis: List<WifiDetails>): Location? {
        val hash = wifis.hash() ?: return null
        readableDatabase.query(
            TABLE_WIFI_SCANS,
            arrayOf(FIELD_LATITUDE, FIELD_LONGITUDE, FIELD_ACCURACY, FIELD_TIME, FIELD_PRECISION),
            "$FIELD_SCAN_HASH = x'${hash.toHexString()}'",
            arrayOf(),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getLocation(MAX_WIFI_AGE).let { return it }
            }
        }
        return null
    }

    fun putWifiScanLocation(wifis: List<WifiDetails>, location: Location) {
        val cv = ContentValues().apply {
            put(FIELD_SCAN_HASH, wifis.hash())
            putLocation(location)
        }
        writableDatabase.insertWithOnConflict(TABLE_WIFI_SCANS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CELLS($FIELD_MCC INTEGER NOT NULL, $FIELD_MNC INTEGER NOT NULL, $FIELD_TYPE INTEGER NOT NULL, $FIELD_LAC_TAC INTEGER NOT NULL, $FIELD_CID INTEGER NOT NULL, $FIELD_PSC INTEGER NOT NULL, $FIELD_LATITUDE REAL NOT NULL, $FIELD_LONGITUDE REAL NOT NULL, $FIELD_ACCURACY REAL NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_PRECISION REAL NOT NULL);")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CELLS_PRE($FIELD_MCC INTEGER NOT NULL, $FIELD_MNC INTEGER NOT NULL, $FIELD_TIME INTEGER NOT NULL);")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_WIFIS($FIELD_MAC BLOB, $FIELD_LATITUDE REAL NOT NULL, $FIELD_LONGITUDE REAL NOT NULL, $FIELD_ACCURACY REAL NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_PRECISION REAL NOT NULL);")
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_WIFI_SCANS($FIELD_SCAN_HASH BLOB, $FIELD_LATITUDE REAL NOT NULL, $FIELD_LONGITUDE REAL NOT NULL, $FIELD_ACCURACY REAL NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_PRECISION REAL NOT NULL);")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${TABLE_CELLS}_index ON $TABLE_CELLS($FIELD_MCC, $FIELD_MNC, $FIELD_TYPE, $FIELD_LAC_TAC, $FIELD_CID, $FIELD_PSC);")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${TABLE_CELLS_PRE}_index ON $TABLE_CELLS_PRE($FIELD_MCC, $FIELD_MNC);")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${TABLE_WIFIS}_index ON $TABLE_WIFIS($FIELD_MAC);")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${TABLE_WIFI_SCANS}_index ON $TABLE_WIFI_SCANS($FIELD_SCAN_HASH);")
        db.execSQL("CREATE INDEX IF NOT EXISTS ${TABLE_CELLS}_time_index ON $TABLE_CELLS($FIELD_TIME);")
        db.execSQL("CREATE INDEX IF NOT EXISTS ${TABLE_CELLS_PRE}_time_index ON $TABLE_CELLS_PRE($FIELD_TIME);")
        db.execSQL("CREATE INDEX IF NOT EXISTS ${TABLE_WIFIS}_time_index ON $TABLE_WIFIS($FIELD_TIME);")
        db.execSQL("CREATE INDEX IF NOT EXISTS ${TABLE_WIFI_SCANS}_time_index ON $TABLE_WIFI_SCANS($FIELD_TIME);")
    }

    fun cleanup(db: SQLiteDatabase) {
        db.delete(TABLE_CELLS, "$FIELD_TIME < ?", arrayOf((System.currentTimeMillis() - MAX_CELL_AGE).toString()))
        db.delete(TABLE_CELLS_PRE, "$FIELD_TIME < ?", arrayOf((System.currentTimeMillis() - MAX_CELL_AGE).toString()))
        db.delete(TABLE_WIFIS, "$FIELD_TIME < ?", arrayOf((System.currentTimeMillis() - MAX_WIFI_AGE).toString()))
        db.delete(TABLE_WIFI_SCANS, "$FIELD_TIME < ?", arrayOf((System.currentTimeMillis() - MAX_WIFI_AGE).toString()))
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

    companion object {
        private const val PROVIDER_CACHE = "cache"
        val NEGATIVE_CACHE_ENTRY = Location(PROVIDER_CACHE)
        private const val MAX_CELL_AGE = 1000L * 60 * 60 * 24 * 365 // 1 year
        private const val MAX_WIFI_AGE = 1000L * 60 * 60 * 24 * 14 // 14 days
        private const val TABLE_CELLS = "cells"
        private const val TABLE_CELLS_PRE = "cells_pre"
        private const val TABLE_WIFIS = "wifis"
        private const val TABLE_WIFI_SCANS = "wifi_scans"
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

private val WifiDetails.macClean: String
    get() = macAddress.lowercase().replace(":", "")

fun List<WifiDetails>.hash(): ByteArray? {
    val filtered = sortedBy { it.macClean }
        .filter { it.timestamp == null || it.timestamp > System.currentTimeMillis() - 60000 }
        .filter { it.signalStrength == null || it.signalStrength > -90 }
    if (filtered.size < 3) return null
    val maxTimestamp = maxOf { it.timestamp ?: 0L }
    fun WifiDetails.hashBytes(): ByteArray {
        val mac = macClean
        return byteArrayOf(
            mac.substring(0, 2).toInt(16).toByte(),
            mac.substring(2, 4).toInt(16).toByte(),
            mac.substring(4, 6).toInt(16).toByte(),
            mac.substring(6, 8).toInt(16).toByte(),
            mac.substring(8, 10).toInt(16).toByte(),
            mac.substring(10, 12).toInt(16).toByte(),
            ((maxTimestamp - (timestamp ?: 0L)) / (60 * 1000)).toByte(), // timestamp
            ((signalStrength ?: 0) / 10).toByte() // signal strength
        )
    }

    val buffer = ByteBuffer.allocate(filtered.size * 8)
    for (wifi in filtered) {
        buffer.put(wifi.hashBytes())
    }
    return buffer.array().digest("SHA-256")
}