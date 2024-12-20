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
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.contentValuesOf
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getStringOrNull
import androidx.core.os.BundleCompat
import org.microg.gms.location.NAME_CELL
import org.microg.gms.location.NAME_WIFI
import org.microg.gms.location.network.cell.CellDetails
import org.microg.gms.location.network.cell.isValid
import org.microg.gms.location.network.wifi.WifiDetails
import org.microg.gms.location.network.wifi.isRequestable
import org.microg.gms.location.network.wifi.macBytes
import org.microg.gms.utils.toHexString
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val CURRENT_VERSION = 8

internal class LocationDatabase(private val context: Context) : SQLiteOpenHelper(context, "geocache.db", null, CURRENT_VERSION) {
    private data class Migration(val apply: String?, val revert: String?, val allowApplyFailure: Boolean, val allowRevertFailure: Boolean)
    private val migrations: Map<Int, List<Migration>>

    init {
        val migrations = mutableMapOf<Int, MutableList<Migration>>()
        fun declare(version: Int, apply: String, revert: String? = null, allowFailure: Boolean = false, allowApplyFailure: Boolean = allowFailure, allowRevertFailure: Boolean = allowFailure) {
            if (!migrations.containsKey(version))
                migrations[version] = arrayListOf()
            migrations[version]!!.add(Migration(apply, revert, allowApplyFailure, allowRevertFailure))
        }

        declare(3, "CREATE TABLE $TABLE_CELLS($FIELD_MCC INTEGER NOT NULL, $FIELD_MNC INTEGER NOT NULL, $FIELD_TYPE INTEGER NOT NULL, $FIELD_LAC_TAC INTEGER NOT NULL, $FIELD_CID INTEGER NOT NULL, $FIELD_PSC INTEGER NOT NULL, $FIELD_LATITUDE REAL NOT NULL, $FIELD_LONGITUDE REAL NOT NULL, $FIELD_ACCURACY REAL NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_PRECISION REAL NOT NULL);")
        declare(3, "CREATE TABLE $TABLE_CELLS_PRE($FIELD_MCC INTEGER NOT NULL, $FIELD_MNC INTEGER NOT NULL, $FIELD_TIME INTEGER NOT NULL);")
        declare(3, "CREATE TABLE $TABLE_WIFIS($FIELD_MAC BLOB, $FIELD_LATITUDE REAL NOT NULL, $FIELD_LONGITUDE REAL NOT NULL, $FIELD_ACCURACY REAL NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_PRECISION REAL NOT NULL);")
        declare(3, "CREATE TABLE $TABLE_CELLS_LEARN($FIELD_MCC INTEGER NOT NULL, $FIELD_MNC INTEGER NOT NULL, $FIELD_TYPE INTEGER NOT NULL, $FIELD_LAC_TAC INTEGER NOT NULL, $FIELD_CID INTEGER NOT NULL, $FIELD_PSC INTEGER NOT NULL, $FIELD_LATITUDE_HIGH REAL NOT NULL, $FIELD_LATITUDE_LOW REAL NOT NULL, $FIELD_LONGITUDE_HIGH REAL NOT NULL, $FIELD_LONGITUDE_LOW REAL NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_BAD_TIME INTEGER);")
        declare(3, "CREATE TABLE $TABLE_WIFI_LEARN($FIELD_MAC BLOB, $FIELD_LATITUDE_HIGH REAL NOT NULL, $FIELD_LATITUDE_LOW REAL NOT NULL, $FIELD_LONGITUDE_HIGH REAL NOT NULL, $FIELD_LONGITUDE_LOW REAL NOT NULL, $FIELD_TIME INTEGER NOT NULL, $FIELD_BAD_TIME INTEGER);")
        declare(3, "CREATE UNIQUE INDEX ${TABLE_CELLS}_index ON $TABLE_CELLS($FIELD_MCC, $FIELD_MNC, $FIELD_TYPE, $FIELD_LAC_TAC, $FIELD_CID, $FIELD_PSC);")
        declare(3, "CREATE UNIQUE INDEX ${TABLE_CELLS_PRE}_index ON $TABLE_CELLS_PRE($FIELD_MCC, $FIELD_MNC);")
        declare(3, "CREATE UNIQUE INDEX ${TABLE_WIFIS}_index ON $TABLE_WIFIS($FIELD_MAC);")
        declare(3, "CREATE UNIQUE INDEX ${TABLE_CELLS_LEARN}_index ON $TABLE_CELLS_LEARN($FIELD_MCC, $FIELD_MNC, $FIELD_TYPE, $FIELD_LAC_TAC, $FIELD_CID, $FIELD_PSC);")
        declare(3, "CREATE UNIQUE INDEX ${TABLE_WIFI_LEARN}_index ON $TABLE_WIFI_LEARN($FIELD_MAC);")
        declare(3, "CREATE INDEX ${TABLE_CELLS}_time_index ON $TABLE_CELLS($FIELD_TIME);")
        declare(3, "CREATE INDEX ${TABLE_CELLS_PRE}_time_index ON $TABLE_CELLS_PRE($FIELD_TIME);")
        declare(3, "CREATE INDEX ${TABLE_WIFIS}_time_index ON $TABLE_WIFIS($FIELD_TIME);")
        declare(3, "CREATE INDEX ${TABLE_CELLS_LEARN}_time_index ON $TABLE_CELLS_LEARN($FIELD_TIME);")
        declare(3, "CREATE INDEX ${TABLE_WIFI_LEARN}_time_index ON $TABLE_WIFI_LEARN($FIELD_TIME);")
        declare(3, "DROP TABLE IF EXISTS $TABLE_WIFI_SCANS;", allowFailure = true)

        declare(4, "ALTER TABLE $TABLE_CELLS_LEARN ADD COLUMN $FIELD_LEARN_RECORD_COUNT INTEGER;")
        declare(4, "ALTER TABLE $TABLE_WIFI_LEARN ADD COLUMN $FIELD_LEARN_RECORD_COUNT INTEGER;")

        declare(5, "ALTER TABLE $TABLE_CELLS ADD COLUMN $FIELD_ALTITUDE REAL;")
        declare(5, "ALTER TABLE $TABLE_CELLS ADD COLUMN $FIELD_ALTITUDE_ACCURACY REAL;")
        declare(5, "ALTER TABLE $TABLE_WIFIS ADD COLUMN $FIELD_ALTITUDE REAL;")
        declare(5, "ALTER TABLE $TABLE_WIFIS ADD COLUMN $FIELD_ALTITUDE_ACCURACY REAL;")

        declare(6, "ALTER TABLE $TABLE_CELLS_LEARN ADD COLUMN $FIELD_ALTITUDE_HIGH REAL;")
        declare(6, "ALTER TABLE $TABLE_CELLS_LEARN ADD COLUMN $FIELD_ALTITUDE_LOW REAL;")
        declare(6, "ALTER TABLE $TABLE_WIFI_LEARN ADD COLUMN $FIELD_ALTITUDE_HIGH REAL;")
        declare(6, "ALTER TABLE $TABLE_WIFI_LEARN ADD COLUMN $FIELD_ALTITUDE_LOW REAL;")

        declare(8, "DELETE FROM $TABLE_WIFIS WHERE $FIELD_ACCURACY = 0.0 AND ($FIELD_LATITUDE != 0.0 OR $FIELD_LONGITUDE != 0.0);", allowRevertFailure = true)
        declare(8, "DELETE FROM $TABLE_CELLS WHERE $FIELD_ACCURACY = 0.0 AND ($FIELD_LATITUDE != 0.0 OR $FIELD_LONGITUDE != 0.0);", allowRevertFailure = true)
        declare(8, "UPDATE $TABLE_CELLS_LEARN SET $FIELD_ALTITUDE_LOW = NULL WHERE $FIELD_ALTITUDE_LOW = 0.0;", allowRevertFailure = true)
        declare(8, "UPDATE $TABLE_CELLS_LEARN SET $FIELD_ALTITUDE_HIGH = NULL WHERE $FIELD_ALTITUDE_HIGH = 0.0;", allowRevertFailure = true)
        declare(8, "UPDATE $TABLE_WIFI_LEARN SET $FIELD_ALTITUDE_LOW = NULL WHERE $FIELD_ALTITUDE_LOW = 0.0;", allowRevertFailure = true)
        declare(8, "UPDATE $TABLE_WIFI_LEARN SET $FIELD_ALTITUDE_HIGH = NULL WHERE $FIELD_ALTITUDE_HIGH = 0.0;", allowRevertFailure = true)

        this.migrations = migrations
    }

    private fun migrate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int, allowFailure: Boolean = false) {
        var currentVersion = oldVersion
        while (currentVersion < newVersion) {
            val nextVersion = currentVersion + 1
            val migrations = this.migrations[nextVersion].orEmpty()
            for (migration in migrations) {
                if (migration.apply == null && !migration.allowApplyFailure && !allowFailure)
                    throw SQLiteException("Incomplete migration from $currentVersion to $nextVersion")
                try {
                    db.execSQL(migration.apply)
                    Log.d(TAG, "Applied migration from version $currentVersion to $nextVersion: ${migration.apply}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error while applying migration from version $currentVersion to $nextVersion: ${migration.apply}", e)
                    if (!migration.allowApplyFailure && !allowFailure)
                        throw e
                }
            }

            currentVersion = nextVersion
        }
        while (currentVersion > newVersion) {
            val nextVersion = currentVersion - 1
            val migrations = this.migrations[currentVersion].orEmpty()
            for (migration in migrations.asReversed()) {
                if (migration.revert == null && !migration.allowRevertFailure && !allowFailure)
                    throw SQLiteException("Incomplete migration from $currentVersion to $nextVersion")
                try {
                    db.execSQL(migration.revert)
                    Log.d(TAG, "Reverted migration from version $currentVersion to $nextVersion: ${migration.revert}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error while reverting migration from version $currentVersion to $nextVersion: ${migration.revert}", e)
                    if (!migration.allowRevertFailure && !allowFailure)
                        throw e
                }
            }

            currentVersion = nextVersion
        }
        Log.i(TAG, "Migrated from $oldVersion to $newVersion")
    }

    private fun SQLiteDatabase.query(table: String, columns: Array<String>, selection: String? = null, selectionArgs: Array<String>? = null) =
        query(table, columns, selection, selectionArgs, null, null, null)

    fun getCellLocation(cell: CellDetails, allowLearned: Boolean = true): Location? {
        var cursor = readableDatabase.query(TABLE_CELLS, FIELDS_CACHE_LOCATION, CELLS_SELECTION, getCellSelectionArgs(cell))
        val cellLocation = cursor.getSingleLocation(MAX_CACHE_AGE)
        if (allowLearned) {
            cursor = readableDatabase.query(TABLE_CELLS_LEARN, FIELDS_MID_LOCATION_GET_LEARN, CELLS_SELECTION, getCellSelectionArgs(cell))
            try {
                if (cursor.moveToNext()) {
                    val badTime = cursor.getLong(8)
                    val time = cursor.getLong(7)
                    if (badTime < time - LEARN_BAD_CUTOFF) {
                        cursor.getCellMidLocation()?.let {
                            if (cellLocation == null || cellLocation == NEGATIVE_CACHE_ENTRY || cellLocation.precision < it.precision) return it
                        }
                    }
                }
            } finally {
                cursor.close()
            }
        }
        if (cellLocation != null) return cellLocation
        cursor = readableDatabase.query(TABLE_CELLS_PRE, arrayOf(FIELD_TIME), CELLS_PRE_SELECTION, getCellPreSelectionArgs(cell))
        try {
            if (cursor.moveToNext()) {
                if (cursor.getLong(1) > System.currentTimeMillis() - MAX_CACHE_AGE) {
                    return NEGATIVE_CACHE_ENTRY
                }
            }
        } finally {
            cursor.close()
        }
        return null
    }

    fun getWifiLocation(wifi: WifiDetails, allowLearned: Boolean = true): Location? {
        var cursor = readableDatabase.query(TABLE_WIFIS, FIELDS_CACHE_LOCATION, getWifiSelection(wifi))
        val wifiLocation = cursor.getSingleLocation(MAX_CACHE_AGE)
        if (allowLearned) {
            cursor = readableDatabase.query(TABLE_WIFI_LEARN, FIELDS_MID_LOCATION_GET_LEARN, getWifiSelection(wifi))
            try {
                if (cursor.moveToNext()) {
                    val badTime = cursor.getLong(8)
                    val time = cursor.getLong(7)
                    if (badTime < time - LEARN_BAD_CUTOFF) {
                        cursor.getWifiMidLocation()?.let {
                            if (wifiLocation == null || wifiLocation == NEGATIVE_CACHE_ENTRY || wifiLocation.precision < it.precision) return it
                        }
                    }
                }
            } finally {
                cursor.close()
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
            FIELD_PSC to (cell.pscOrPci ?: 0)
        ).apply { putLocation(location) }
        writableDatabase.insertWithOnConflict(TABLE_CELLS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun putWifiLocation(wifi: WifiDetails, location: Location) {
        if (!wifi.isRequestable) return
        val cv = contentValuesOf(
            FIELD_MAC to wifi.macBytes
        ).apply { putLocation(location) }
        writableDatabase.insertWithOnConflict(TABLE_WIFIS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun learnCellLocation(cell: CellDetails, location: Location, import: Boolean = false): Boolean {
        if (!cell.isValid) return false
        val cursor = readableDatabase.query(TABLE_CELLS_LEARN, FIELDS_MID_LOCATION, CELLS_SELECTION, getCellSelectionArgs(cell))
        var exists = false
        var isBad = false
        var midLocation: Location? = null
        try {
            if (cursor.moveToNext()) {
                midLocation = cursor.getMidLocation()
                exists = midLocation != null
                isBad = midLocation?.let { it.distanceTo(location) > LEARN_BAD_SIZE_CELL } == true
            }
        } finally {
            cursor.close()
        }
        if (exists && isBad) {
            val values = ContentValues().apply { putLearnLocation(location, badTime = location.time, import = import) }
            writableDatabase.update(TABLE_CELLS_LEARN, values, CELLS_SELECTION, getCellSelectionArgs(cell))
        } else if (!exists) {
            val values = contentValuesOf(
                FIELD_MCC to cell.mcc,
                FIELD_MNC to cell.mnc,
                FIELD_LAC_TAC to (cell.lac ?: cell.tac ?: 0),
                FIELD_TYPE to cell.type.ordinal,
                FIELD_CID to cell.cid,
                FIELD_PSC to (cell.pscOrPci ?: 0),
            ).apply { putLearnLocation(location, badTime = 0) }
            writableDatabase.insertWithOnConflict(TABLE_CELLS_LEARN, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            val values = ContentValues().apply { putLearnLocation(location, midLocation) }
            writableDatabase.update(TABLE_CELLS_LEARN, values, CELLS_SELECTION, getCellSelectionArgs(cell))
        }
        return true
    }

    fun learnWifiLocation(wifi: WifiDetails, location: Location, import: Boolean = false): Boolean {
        if (!wifi.isRequestable) return false
        val cursor = readableDatabase.query(TABLE_WIFI_LEARN, FIELDS_MID_LOCATION, getWifiSelection(wifi))
        var exists = false
        var isBad = false
        var midLocation: Location? = null
        try {
            if (cursor.moveToNext()) {
                midLocation = cursor.getMidLocation()
                exists = midLocation != null
                isBad = midLocation?.let { it.distanceTo(location) > LEARN_BAD_SIZE_WIFI } == true
            }
        } finally {
            cursor.close()
        }
        if (exists && isBad) {
            val values = ContentValues().apply { putLearnLocation(location, badTime = location.time, import = import) }
            writableDatabase.update(TABLE_WIFI_LEARN, values, getWifiSelection(wifi), null)
        } else if (!exists) {
            val values = contentValuesOf(
                FIELD_MAC to wifi.macBytes
            ).apply { putLearnLocation(location, badTime = 0) }
            writableDatabase.insertWithOnConflict(TABLE_WIFI_LEARN, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            val values = ContentValues().apply { putLearnLocation(location, midLocation) }
            writableDatabase.update(TABLE_WIFI_LEARN, values, getWifiSelection(wifi), null)
        }
        return true
    }

    fun exportLearned(name: String): Uri? {
        try {
            val wifi = when (name) {
                NAME_WIFI -> true
                NAME_CELL -> false
                else -> throw IllegalArgumentException()
            }
            val fieldNames = if (wifi) FIELDS_WIFI else FIELDS_CELL
            val tableName = if (wifi) TABLE_WIFI_LEARN else TABLE_CELLS_LEARN
            val midLocationGetter: (Cursor) -> Location? = if (wifi) Cursor::getWifiMidLocation else Cursor::getCellMidLocation

            val exportDir = File(context.cacheDir, "location")
            exportDir.mkdir()
            val exportFile = File(exportDir, "$name-${UUID.randomUUID()}.csv.gz")
            val output = GZIPOutputStream(exportFile.outputStream()).bufferedWriter()
            output.write("${fieldNames.joinToString(",")},${FIELDS_EXPORT_DATA.joinToString(",")}\n")
            val cursor = readableDatabase.query(tableName, FIELDS_MID_LOCATION + fieldNames)
            val indices = fieldNames.map { cursor.getColumnIndexOrThrow(it) }
            while (cursor.moveToNext()) {
                val midLocation = midLocationGetter(cursor)
                if (midLocation != null) {
                    output.write(indices.joinToString(",") { index ->
                        if (cursor.getType(index) == Cursor.FIELD_TYPE_BLOB) {
                            cursor.getBlob(index).toHexString()
                        } else {
                            cursor.getStringOrNull(index) ?: ""
                        }
                    })
                    output.write(",${midLocation.latitude},${midLocation.longitude},${if (midLocation.hasAltitude()) midLocation.altitude else ""}\n")
                }
            }
            output.close()
            return FileProvider.getUriForFile(context,"${context.packageName}.microg.location.export", exportFile)
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
        return null
    }

    fun importLearned(fileUri: Uri): Int {
        var counter = 0
        try {
            val type = context.contentResolver.getType(fileUri)
            val gzip = if (type == null || type !in SUPPORTED_TYPES) {
                if (fileUri.path == null) throw IllegalArgumentException("Unsupported file extension")
                if (fileUri.path!!.endsWith(".gz")) {
                    true
                } else if (fileUri.path!!.endsWith(".csv")) {
                    false
                } else {
                    throw IllegalArgumentException("Unsupported file extension")
                }
            } else {
                type.endsWith("gzip")
            }
            val desc = context.contentResolver.openFileDescriptor(fileUri, "r") ?: throw FileNotFoundException()
            ParcelFileDescriptor.AutoCloseInputStream(desc).use { source ->
                val input = (if (gzip) GZIPInputStream(source) else source).bufferedReader()
                val headers = input.readLine().split(",")
                val name = when {
                    headers.containsAll(FIELDS_WIFI.toList()) && headers.containsAll(FIELDS_EXPORT_DATA.toList()) -> NAME_WIFI
                    headers.containsAll(FIELDS_CELL.toList()) && headers.containsAll(FIELDS_EXPORT_DATA.toList()) -> NAME_CELL
                    else -> null
                }
                if (name != null) {
                    while (true) {
                        val line = input.readLine().split(",")
                        if (line.size != headers.size) break // End of file reached
                        val location = Location(PROVIDER_CACHE)
                        location.latitude = line[headers.indexOf(FIELD_LATITUDE)].toDoubleOrNull() ?: continue
                        location.longitude = line[headers.indexOf(FIELD_LONGITUDE)].toDoubleOrNull() ?: continue
                        line[headers.indexOf(FIELD_ALTITUDE)].toDoubleOrNull()?.let { location.altitude = it }
                        if (name == NAME_WIFI) {
                            val wifi = WifiDetails(
                                macAddress = line[headers.indexOf(FIELD_MAC)]
                            )
                            if (learnWifiLocation(wifi, location)) counter++
                        } else  {
                            val cell = CellDetails(
                                type = line[headers.indexOf(FIELD_TYPE)].let {
                                    it.toIntOrNull()?.let { CellDetails.Companion.Type.entries[it] } ?:
                                    runCatching { CellDetails.Companion.Type.valueOf(it) }.getOrNull()
                                } ?: continue,
                                mcc = line[headers.indexOf(FIELD_MCC)].toIntOrNull() ?: continue,
                                mnc = line[headers.indexOf(FIELD_MNC)].toIntOrNull() ?: continue,
                                lac = line[headers.indexOf(FIELD_LAC_TAC)].toIntOrNull(),
                                tac = line[headers.indexOf(FIELD_LAC_TAC)].toIntOrNull(),
                                cid = line[headers.indexOf(FIELD_CID)].toLongOrNull() ?: continue,
                                pscOrPci = line[headers.indexOf(FIELD_PSC)].toIntOrNull(),
                            )
                            if (learnCellLocation(cell, location)) counter++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
        return counter
    }

    override fun onCreate(db: SQLiteDatabase) {
        migrate(db, 0, CURRENT_VERSION)
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
        migrate(db, oldVersion, newVersion)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        migrate(db, oldVersion, newVersion)
    }

    fun dump(writer: PrintWriter) {
        writer.println("Database: cells(cached)=${DatabaseUtils.queryNumEntries(readableDatabase, TABLE_CELLS)}, cells(learnt)=${DatabaseUtils.queryNumEntries(readableDatabase, TABLE_CELLS_LEARN)}, wifis(cached)=${DatabaseUtils.queryNumEntries(readableDatabase, TABLE_WIFIS)}, wifis(learnt)=${DatabaseUtils.queryNumEntries(readableDatabase, TABLE_WIFI_LEARN)}")
    }
}

const val EXTRA_HIGH_LOCATION = "high"
const val EXTRA_LOW_LOCATION = "low"
const val EXTRA_RECORD_COUNT = "recs"
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
private const val FIELD_ALTITUDE = "alt"
private const val FIELD_ALTITUDE_ACCURACY = "alt_acc"
private const val FIELD_TIME = "time"
private const val FIELD_PRECISION = "prec"
private const val FIELD_MAC = "mac"
private const val FIELD_SCAN_HASH = "hash"
private const val FIELD_LATITUDE_HIGH = "lath"
private const val FIELD_LATITUDE_LOW = "latl"
private const val FIELD_LONGITUDE_HIGH = "lonh"
private const val FIELD_LONGITUDE_LOW = "lonl"
private const val FIELD_ALTITUDE_HIGH = "alth"
private const val FIELD_ALTITUDE_LOW = "altl"
private const val FIELD_BAD_TIME = "btime"
private const val FIELD_LEARN_RECORD_COUNT = "recs"

private const val LEARN_BASE_ACCURACY_CELL = 10_000.0
private const val LEARN_BASE_VERTICAL_ACCURACY_CELL = 5_000.0
private const val LEARN_BASE_PRECISION_CELL = 0.5
private const val LEARN_ACCURACY_FACTOR_CELL = 0.002
private const val LEARN_VERTICAL_ACCURACY_FACTOR_CELL = 0.002
private const val LEARN_PRECISION_FACTOR_CELL = 0.01
private const val LEARN_BAD_SIZE_CELL = 10_000

private const val LEARN_BASE_ACCURACY_WIFI = 200.0
private const val LEARN_BASE_VERTICAL_ACCURACY_WIFI = 100.0
private const val LEARN_BASE_PRECISION_WIFI = 0.2
private const val LEARN_ACCURACY_FACTOR_WIFI = 0.02
private const val LEARN_VERTICAL_ACCURACY_FACTOR_WIFI = 0.02
private const val LEARN_PRECISION_FACTOR_WIFI = 0.1
private const val LEARN_BAD_SIZE_WIFI = 200

private const val LEARN_BAD_CUTOFF = 1000L * 60 * 60 * 24 * 14

private const val CELLS_SELECTION = "$FIELD_MCC = ? AND $FIELD_MNC = ? AND $FIELD_TYPE = ? AND $FIELD_LAC_TAC = ? AND $FIELD_CID = ? AND $FIELD_PSC = ?"
private const val CELLS_PRE_SELECTION = "$FIELD_MCC = ? AND $FIELD_MNC = ?"

private val FIELDS_CACHE_LOCATION = arrayOf(FIELD_LATITUDE, FIELD_LONGITUDE, FIELD_ACCURACY, FIELD_ALTITUDE, FIELD_ALTITUDE_ACCURACY, FIELD_TIME, FIELD_PRECISION)
private val FIELDS_MID_LOCATION = arrayOf(FIELD_LATITUDE_HIGH, FIELD_LATITUDE_LOW, FIELD_LONGITUDE_HIGH, FIELD_LONGITUDE_LOW, FIELD_ALTITUDE_HIGH, FIELD_ALTITUDE_LOW, FIELD_LEARN_RECORD_COUNT, FIELD_TIME)
private val FIELDS_MID_LOCATION_GET_LEARN = FIELDS_MID_LOCATION + FIELD_BAD_TIME
private val FIELDS_CELL = arrayOf(FIELD_MCC, FIELD_MNC, FIELD_TYPE, FIELD_LAC_TAC, FIELD_CID, FIELD_PSC)
private val FIELDS_WIFI = arrayOf(FIELD_MAC)
private val FIELDS_EXPORT_DATA = arrayOf(FIELD_LATITUDE, FIELD_LONGITUDE, FIELD_ALTITUDE)
private val SUPPORTED_TYPES = listOf(
    "application/vnd.microg.location.cell+csv+gzip",
    "application/vnd.microg.location.cell+csv",
    "application/vnd.microg.location.wifi+csv+gzip",
    "application/vnd.microg.location.wifi+csv",
    "application/gzip",
    "application/x-gzip",
    "text/csv",
)

private fun getCellSelectionArgs(cell: CellDetails): Array<String> {
    return arrayOf(
        cell.mcc.toString(),
        cell.mnc.toString(),
        cell.type.ordinal.toString(),
        (cell.lac ?: cell.tac ?: 0).toString(),
        cell.cid.toString(),
        (cell.pscOrPci ?: 0).toString(),
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

private fun Cursor.getSingleLocation(maxAge: Long): Location? {
    return try {
        if (moveToNext()) {
            getLocation(maxAge)
        } else {
            null
        }
    } finally {
        close()
    }
}

private fun Cursor.getLocation(maxAge: Long): Location? {
    if (getLong(5) > System.currentTimeMillis() - maxAge) {
        if (getDouble(2) == 0.0) return NEGATIVE_CACHE_ENTRY
        return Location(PROVIDER_CACHE).apply {
            latitude = getDouble(0)
            longitude = getDouble(1)
            accuracy = getDouble(2).toFloat()
            getDoubleOrNull(3)?.let { altitude = it }
            verticalAccuracy = getDoubleOrNull(4)?.toFloat()
            time = getLong(5)
            precision = getDouble(6)
        }
    }
    return null
}

private fun Cursor.getMidLocation(
    maxAge: Long = Long.MAX_VALUE,
    baseAccuracy: Double = 0.0,
    accuracyFactor: Double = 0.0,
    baseVerticalAccuracy: Double = baseAccuracy,
    verticalAccuracyFactor: Double = accuracyFactor,
    basePrecision: Double = 0.0,
    precisionFactor: Double = 0.0
): Location? {
    if (maxAge == Long.MAX_VALUE || getLong(7) > System.currentTimeMillis() - maxAge) {
        val high = Location(PROVIDER_CACHE).apply { latitude = getDouble(0); longitude = getDouble(2) }
        if (!isNull(4)) high.altitude = getDouble(4)
        val low = Location(PROVIDER_CACHE).apply { latitude = getDouble(1); longitude = getDouble(3) }
        if (!isNull(5)) low.altitude = getDouble(5)
        val count = getInt(6)
        val computedAccuracy = baseAccuracy / (1 + (accuracyFactor * (count - 1).toDouble()))
        val computedVerticalAccuracy = baseVerticalAccuracy / (1 + (verticalAccuracyFactor * (count - 1).toDouble()))
        return Location(PROVIDER_CACHE).apply {
            latitude = (high.latitude + low.latitude) / 2.0
            longitude = (high.longitude + low.longitude) / 2.0
            accuracy = max(high.distanceTo(low) / 2.0, computedAccuracy).toFloat()
            if (high.hasAltitude() && low.hasAltitude()) {
                altitude = (high.altitude + low.altitude) / 2.0
                verticalAccuracy = max((abs(high.altitude - low.altitude) / 2.0), computedVerticalAccuracy).toFloat()
            } else if (high.hasAltitude()) {
                altitude = high.altitude
                verticalAccuracy = computedVerticalAccuracy.toFloat()
            } else if (low.hasAltitude()) {
                altitude = low.altitude
                verticalAccuracy = computedVerticalAccuracy.toFloat()
            }
            precision = basePrecision * (1 + (precisionFactor * (count - 1)))
            highLocation = high
            lowLocation = low
            extras += EXTRA_RECORD_COUNT to count
        }
    }
    return null
}

private fun Cursor.getWifiMidLocation() = getMidLocation(
    MAX_LEARN_AGE,
    baseAccuracy = LEARN_BASE_ACCURACY_WIFI,
    accuracyFactor = LEARN_ACCURACY_FACTOR_WIFI,
    baseVerticalAccuracy = LEARN_BASE_VERTICAL_ACCURACY_WIFI,
    verticalAccuracyFactor = LEARN_VERTICAL_ACCURACY_FACTOR_WIFI,
    basePrecision = LEARN_BASE_PRECISION_WIFI,
    precisionFactor = LEARN_PRECISION_FACTOR_WIFI
)

private fun Cursor.getCellMidLocation() = getMidLocation(
    MAX_LEARN_AGE,
    baseAccuracy = LEARN_BASE_ACCURACY_CELL,
    accuracyFactor = LEARN_ACCURACY_FACTOR_CELL,
    baseVerticalAccuracy = LEARN_BASE_VERTICAL_ACCURACY_CELL,
    verticalAccuracyFactor = LEARN_VERTICAL_ACCURACY_FACTOR_CELL,
    basePrecision = LEARN_BASE_PRECISION_CELL,
    precisionFactor = LEARN_PRECISION_FACTOR_CELL
)

private var Location.highLocation: Location?
    get() = extras?.let { BundleCompat.getParcelable(it, EXTRA_HIGH_LOCATION, Location::class.java) }
    set(value) { extras += EXTRA_HIGH_LOCATION to value }

private val Location.highLatitude: Double
    get() = highLocation?.latitude ?: latitude
private val Location.highLongitude: Double
    get() = highLocation?.longitude ?: longitude
private val Location.highAltitude: Double?
    get() = highLocation?.takeIf { it.hasAltitude() }?.altitude ?: altitude.takeIf { hasAltitude() }

private var Location.lowLocation: Location?
    get() = extras?.let { BundleCompat.getParcelable(it, EXTRA_LOW_LOCATION, Location::class.java) }
    set(value) { extras += EXTRA_LOW_LOCATION to value }

private val Location.lowLatitude: Double
    get() = lowLocation?.latitude ?: latitude
private val Location.lowLongitude: Double
    get() = lowLocation?.longitude ?: longitude
private val Location.lowAltitude: Double?
    get() = lowLocation?.takeIf { it.hasAltitude() }?.altitude ?: altitude.takeIf { hasAltitude() }

private var Location?.recordCount: Int
    get() = this?.extras?.getInt(EXTRA_RECORD_COUNT, 0) ?: 0
    set(value) { this?.extras += EXTRA_RECORD_COUNT to value }

private fun max(first: Double, second: Double?): Double {
    if (second == null) return first
    return max(first, second)
}

private fun max(first: Long, second: Long?): Long {
    if (second == null) return first
    return max(first, second)
}

private fun min(first: Double, second: Double?): Double {
    if (second == null) return first
    return min(first, second)
}

private fun ContentValues.putLocation(location: Location) {
    if (location != NEGATIVE_CACHE_ENTRY) {
        put(FIELD_LATITUDE, location.latitude)
        put(FIELD_LONGITUDE, location.longitude)
        put(FIELD_ACCURACY, location.accuracy)
        put(FIELD_TIME, location.time)
        put(FIELD_PRECISION, location.precision)
        if (location.hasAltitude()) {
            put(FIELD_ALTITUDE, location.altitude)
            put(FIELD_ALTITUDE_ACCURACY, location.verticalAccuracy)
        }
    } else {
        put(FIELD_LATITUDE, 0.0)
        put(FIELD_LONGITUDE, 0.0)
        put(FIELD_ACCURACY, 0.0)
        put(FIELD_TIME, System.currentTimeMillis())
        put(FIELD_PRECISION, 0.0)
    }
}

private fun ContentValues.putLearnLocation(location: Location, previous: Location? = null, badTime: Long? = null, import: Boolean = false) {
    if (location != NEGATIVE_CACHE_ENTRY) {
        put(FIELD_LATITUDE_HIGH, max(location.latitude, previous?.highLatitude))
        put(FIELD_LATITUDE_LOW, min(location.latitude, previous?.lowLatitude))
        put(FIELD_LONGITUDE_HIGH, max(location.longitude, previous?.highLongitude))
        put(FIELD_LONGITUDE_LOW, min(location.longitude, previous?.lowLongitude))
        put(FIELD_TIME, max(location.time, previous?.time ?: 0))
        if (location.hasAltitude()) {
            put(FIELD_ALTITUDE_HIGH, max(location.altitude, previous?.highAltitude))
            put(FIELD_ALTITUDE_LOW, min(location.altitude, previous?.lowAltitude))
        }
        put(FIELD_LEARN_RECORD_COUNT, previous.recordCount + 1)
        if (badTime != null && !import) {
            put(FIELD_BAD_TIME, max(badTime, previous?.time))
        }
    }
}