/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.core.content.FileProvider
import com.google.android.gms.nearby.exposurenotification.*
import kotlinx.coroutines.*
import okio.ByteString
import org.microg.gms.common.PackageUtils
import java.io.File
import java.lang.Runnable
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.*
import kotlin.experimental.and

@TargetApi(21)
class ExposureDatabase private constructor(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private val createdAt: Exception = Exception("Database ${hashCode()} created")
    private var refCount = 1

    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        onUpgrade(db, 0, DB_VERSION)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Upgrading database from $oldVersion to $newVersion")
        if (oldVersion < 1) {
            Log.d(TAG, "Creating tables for version >= 1")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ADVERTISEMENTS(rpi BLOB NOT NULL, aem BLOB NOT NULL, timestamp INTEGER NOT NULL, rssi INTEGER NOT NULL, duration INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(rpi, timestamp));")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_ADVERTISEMENTS}_rpi ON $TABLE_ADVERTISEMENTS(rpi);")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_ADVERTISEMENTS}_timestamp ON $TABLE_ADVERTISEMENTS(timestamp);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_APP_LOG(package TEXT NOT NULL, timestamp INTEGER NOT NULL, method TEXT NOT NULL, args TEXT);")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_APP_LOG}_package_timestamp ON $TABLE_APP_LOG(package, timestamp);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK(keyData BLOB NOT NULL, rollingStartNumber INTEGER NOT NULL, rollingPeriod INTEGER NOT NULL);")
        }
        if (oldVersion < 3) {
            Log.d(TAG, "Creating tables for version >= 3")
            db.execSQL("CREATE TABLE $TABLE_APP_PERMS(package TEXT NOT NULL, sig TEXT NOT NULL, perm TEXT NOT NULL, timestamp INTEGER NOT NULL);")
        }
        if (oldVersion < 5) {
            Log.d(TAG, "Creating tables for version >= 5")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TOKENS(tid INTEGER PRIMARY KEY, package TEXT NOT NULL, token TEXT NOT NULL, timestamp INTEGER NOT NULL, configuration BLOB, diagnosisKeysDataMap BLOB);")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${TABLE_TOKENS}_package_token ON $TABLE_TOKENS(package, token);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK_CHECK_SINGLE(tcsid INTEGER PRIMARY KEY, keyData BLOB NOT NULL, rollingStartNumber INTEGER NOT NULL, rollingPeriod INTEGER NOT NULL, matched INTEGER);")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${TABLE_TEK_CHECK_SINGLE}_key ON $TABLE_TEK_CHECK_SINGLE(keyData, rollingStartNumber, rollingPeriod);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK_CHECK_SINGLE_TOKEN(tcsid INTEGER REFERENCES $TABLE_TEK_CHECK_SINGLE(tcsid) ON DELETE CASCADE, tid INTEGER REFERENCES $TABLE_TOKENS(tid) ON DELETE CASCADE, transmissionRiskLevel INTEGER NOT NULL, reportType INTEGER NOT NULL, daysSinceOnsetOfSymptoms INTEGER NOT NULL, UNIQUE(tcsid, tid));")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_TEK_CHECK_SINGLE_TOKEN}_tid ON $TABLE_TEK_CHECK_SINGLE_TOKEN(tid);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK_CHECK_FILE(tcfid INTEGER PRIMARY KEY, hash TEXT NOT NULL, endTimestamp INTEGER NOT NULL, keys INTEGER NOT NULL);")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${TABLE_TEK_CHECK_FILE}_hash ON $TABLE_TEK_CHECK_FILE(hash);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK_CHECK_FILE_TOKEN(tcfid INTEGER REFERENCES $TABLE_TEK_CHECK_FILE(tcfid) ON DELETE CASCADE, tid INTEGER REFERENCES $TABLE_TOKENS(tid) ON DELETE CASCADE, UNIQUE(tcfid, tid));")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_TEK_CHECK_FILE_TOKEN}_tid ON $TABLE_TEK_CHECK_FILE_TOKEN(tid);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK_CHECK_FILE_MATCH(tcfid INTEGER REFERENCES $TABLE_TEK_CHECK_FILE(tcfid) ON DELETE CASCADE, keyData BLOB NOT NULL, rollingStartNumber INTEGER NOT NULL, rollingPeriod INTEGER NOT NULL, transmissionRiskLevel INTEGER NOT NULL, reportType INTEGER NOT NULL, daysSinceOnsetOfSymptoms INTEGER NOT NULL, UNIQUE(tcfid, keyData, rollingStartNumber, rollingPeriod));")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_TEK_CHECK_FILE_MATCH}_tcfid ON $TABLE_TEK_CHECK_FILE_MATCH(tcfid);")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_TEK_CHECK_FILE_MATCH}_key ON $TABLE_TEK_CHECK_FILE_MATCH(keyData, rollingStartNumber, rollingPeriod);")
        }
        if (oldVersion < 9) {
            Log.d(TAG, "Creating tables for version >= 9")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_APP(package TEXT NOT NULL, sig TEXT NOT NULL, PRIMARY KEY (package, sig));")
        }
        if (oldVersion in 5 until 7) {
            Log.d(TAG, "Altering tables for version >= 7")
            db.execSQL("ALTER TABLE $TABLE_TOKENS ADD COLUMN diagnosisKeysDataMap BLOB;")
        }
        if (oldVersion in 5 until 11) {
            Log.d(TAG, "Altering tables for version >= 11")
            db.execSQL("ALTER TABLE $TABLE_TEK_CHECK_SINGLE_TOKEN ADD COLUMN reportType INTEGER NOT NULL DEFAULT ${ReportType.UNKNOWN};")
            db.execSQL("ALTER TABLE $TABLE_TEK_CHECK_SINGLE_TOKEN ADD COLUMN daysSinceOnsetOfSymptoms INTEGER NOT NULL DEFAULT ${TemporaryExposureKey.DAYS_SINCE_ONSET_OF_SYMPTOMS_UNKNOWN};")
            db.execSQL("ALTER TABLE $TABLE_TEK_CHECK_FILE_MATCH ADD COLUMN reportType INTEGER NOT NULL DEFAULT ${ReportType.UNKNOWN};")
            db.execSQL("ALTER TABLE $TABLE_TEK_CHECK_FILE_MATCH ADD COLUMN daysSinceOnsetOfSymptoms INTEGER NOT NULL DEFAULT ${TemporaryExposureKey.DAYS_SINCE_ONSET_OF_SYMPTOMS_UNKNOWN};")
        }
        if (oldVersion in 1 until 5) {
            Log.d(TAG, "Dropping legacy tables from version < 5")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_CONFIGURATIONS;")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_DIAGNOSIS;")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_TEK_CHECK;")
        }
        if (oldVersion in 1 until 6) {
            Log.d(TAG, "Fixing invalid rssi values from version < 6")
            // There's no bluetooth chip with a sensitivity that would result in rssi -200, so this would be invalid.
            // RSSI of -100 is already extremely low and thus is a good "default" value
            db.execSQL("UPDATE $TABLE_ADVERTISEMENTS SET rssi = -100 WHERE rssi < -200;")
        }
        if (oldVersion in 5 until 7) {
            Log.d(TAG, "Clearing non-matching tek cache from version < 7")
            // Entries might be invalid due to previously missing support for new bluetooth AEM format
            db.execSQL("DELETE FROM $TABLE_TEK_CHECK_FILE WHERE tcfid NOT IN (SELECT tcfid FROM $TABLE_TEK_CHECK_FILE_MATCH);")
        }
        if (oldVersion in 1 until 9) {
            Log.d(TAG, "Migrating authorized apps from version < 9")
            val pm = context.packageManager
            db.query(true, TABLE_APP_LOG, arrayOf("package"), null, null, null, null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val packageName = cursor.getString(0)
                    val signatureDigest = PackageUtils.firstSignatureDigest(pm, packageName)
                    if (signatureDigest != null) {
                        db.insertWithOnConflict(TABLE_APP, "NULL", ContentValues().apply {
                            put("package", packageName)
                            put("sig", signatureDigest)
                        }, CONFLICT_IGNORE)
                    }
                }
            }
        }
        if (oldVersion == 9) {
            Log.d(TAG, "Get rid of isEnabled log entries")
            db.delete(TABLE_APP_LOG, "method = ?", arrayOf("isEnabled"));
        }
        if (oldVersion == 11) {
            Log.d(TAG, "Fixing invalid rssi values from version 11 with release 0.2.23")
            // Setting the RSSI to -75. This is obviously not the correct value, but is still way better estimate
            // than 0, based on the measurements shown in https://github.com/microg/GmsCore/issues/1655
            db.execSQL("UPDATE $TABLE_ADVERTISEMENTS SET rssi = -75 WHERE rssi = 0 AND duration > 0")
        }
        Log.d(TAG, "Finished database upgrade")
    }

    fun SQLiteDatabase.delete(table: String, whereClause: String, args: LongArray): Int =
            compileStatement("DELETE FROM $table WHERE $whereClause").use {
                args.forEachIndexed { idx, l -> it.bindLong(idx + 1, l) }
                it.executeUpdateDelete()
            }

    fun dailyCleanup(): Boolean = writableDatabase.run {
        val start = System.currentTimeMillis()
        val rollingStartTime = currentDayRollingStartNumber.toLong() * ROLLING_WINDOW_LENGTH_MS - TimeUnit.DAYS.toMillis(KEEP_DAYS.toLong())
        val advertisements = delete(TABLE_ADVERTISEMENTS, "timestamp < ?", longArrayOf(rollingStartTime))
        Log.d(TAG, "Deleted on daily cleanup: $advertisements adv")
        if (start + MAX_DELETE_TIME < System.currentTimeMillis()) return@run false
        val appLogEntries = delete(TABLE_APP_LOG, "timestamp < ?", longArrayOf(rollingStartTime))
        Log.d(TAG, "Deleted on daily cleanup: $appLogEntries applogs")
        if (start + MAX_DELETE_TIME < System.currentTimeMillis()) return@run false
        val temporaryExposureKeys = delete(TABLE_TEK, "(rollingStartNumber + rollingPeriod) < ?", longArrayOf(rollingStartTime / ROLLING_WINDOW_LENGTH_MS))
        Log.d(TAG, "Deleted on daily cleanup: $temporaryExposureKeys teks")
        if (start + MAX_DELETE_TIME < System.currentTimeMillis()) return@run false
        val singleCheckedTemporaryExposureKeys = delete(TABLE_TEK_CHECK_SINGLE, "rollingStartNumber < ?", longArrayOf(rollingStartTime / ROLLING_WINDOW_LENGTH_MS - ROLLING_PERIOD))
        Log.d(TAG, "Deleted on daily cleanup: $singleCheckedTemporaryExposureKeys tcss")
        if (start + MAX_DELETE_TIME < System.currentTimeMillis()) return@run false
        val fileCheckedTemporaryExposureKeys = delete(TABLE_TEK_CHECK_FILE, "endTimestamp < ?", longArrayOf(rollingStartTime))
        Log.d(TAG, "Deleted on daily cleanup: $fileCheckedTemporaryExposureKeys tcfs")
        if (start + MAX_DELETE_TIME < System.currentTimeMillis()) return@run false
        val appPerms = delete(TABLE_APP_PERMS, "timestamp < ?", longArrayOf(System.currentTimeMillis() - CONFIRM_PERMISSION_VALIDITY))
        Log.d(TAG, "Deleted on daily cleanup: $appPerms perms")
        if (start + MAX_DELETE_TIME < System.currentTimeMillis()) return@run false
        execSQL("VACUUM;")
        Log.d(TAG, "Done vacuuming")
        return@run true
    }

    fun grantPermission(packageName: String, signatureDigest: String, permission: String, timestamp: Long = System.currentTimeMillis()) = writableDatabase.run {
        insert(TABLE_APP_PERMS, "NULL", ContentValues().apply {
            put("package", packageName)
            put("sig", signatureDigest)
            put("perm", permission)
            put("timestamp", timestamp)
        })
    }

    fun hasPermission(packageName: String, signatureDigest: String, permission: String, maxAge: Long = CONFIRM_PERMISSION_VALIDITY) = readableDatabase.run {
        query(TABLE_APP_PERMS, arrayOf("MAX(timestamp)"), "package = ? AND sig = ? and perm = ?", arrayOf(packageName, signatureDigest, permission), null, null, null).use { cursor ->
            cursor.moveToNext() && cursor.getLong(0) + maxAge > System.currentTimeMillis()
        }
    }

    fun noteAdvertisement(rpi: ByteArray, aem: ByteArray, rssi: Int, timestamp: Long = Date().time) = writableDatabase.run {
        val update = compileStatement("UPDATE $TABLE_ADVERTISEMENTS SET rssi = IFNULL(((rssi * duration) + (? * MAX(0, ? - timestamp - duration))) / MAX(duration, ? - timestamp), -100), duration = MAX(duration, ? - timestamp) WHERE rpi = ? AND timestamp > ? AND timestamp < ?").run {
            bindLong(1, rssi.toLong())
            bindLong(2, timestamp)
            bindLong(3, timestamp)
            bindLong(4, timestamp)
            bindBlob(5, rpi)
            bindLong(6, timestamp - ALLOWED_KEY_OFFSET_MS)
            bindLong(7, timestamp + ALLOWED_KEY_OFFSET_MS)
            executeUpdateDelete()
        }
        if (update <= 0) {
            insert(TABLE_ADVERTISEMENTS, "NULL", ContentValues().apply {
                put("rpi", rpi)
                put("aem", aem)
                put("timestamp", timestamp)
                put("rssi", rssi)
                put("duration", MINIMUM_EXPOSURE_DURATION_MS)
            })
        }
    }

    fun deleteAllCollectedAdvertisements() = writableDatabase.run {
        delete(TABLE_ADVERTISEMENTS, null, null)
        delete(TABLE_TEK_CHECK_FILE_MATCH, null, null)
        update(TABLE_TEK_CHECK_SINGLE, ContentValues().apply {
            put("matched", 0)
        }, null, null)
    }

    fun authorizeApp(packageName: String?, signatureDigest: String? = PackageUtils.firstSignatureDigest(context, packageName)) = writableDatabase.run {
        if (packageName == null || signatureDigest == null) return@run
        insertWithOnConflict(TABLE_APP, "NULL", ContentValues().apply {
            put("package", packageName)
            put("sig", signatureDigest)
        }, CONFLICT_IGNORE)
    }

    fun isAppAuthorized(packageName: String?, signatureDigest: String? = PackageUtils.firstSignatureDigest(context, packageName)): Boolean = readableDatabase.run {
        if (packageName == null || signatureDigest == null) return@run false
        query(TABLE_APP, arrayOf("package"), "package = ? AND sig = ?", arrayOf(packageName, signatureDigest), null, null, null).use { cursor ->
            return@use cursor.moveToNext()
        }
    }

    fun noteAppAction(packageName: String, method: String, args: String? = null, timestamp: Long = Date().time) = writableDatabase.run {
        insert(TABLE_APP_LOG, "NULL", ContentValues().apply {
            put("package", packageName)
            put("timestamp", timestamp)
            put("method", method)
            put("args", args)
        })
    }

    private fun storeOwnKey(key: TemporaryExposureKey, database: SQLiteDatabase = writableDatabase) = database.run {
        insert(TABLE_TEK, "NULL", ContentValues().apply {
            put("keyData", key.keyData)
            put("rollingStartNumber", key.rollingStartIntervalNumber)
            put("rollingPeriod", key.rollingPeriod)
        })
    }

    private fun getTekCheckSingleId(key: TemporaryExposureKey, mayInsert: Boolean = false, database: SQLiteDatabase = if (mayInsert) writableDatabase else readableDatabase): Long? = database.run {
        if (mayInsert) {
            insertWithOnConflict(TABLE_TEK_CHECK_SINGLE, "NULL", ContentValues().apply {
                put("keyData", key.keyData)
                put("rollingStartNumber", key.rollingStartIntervalNumber)
                put("rollingPeriod", key.rollingPeriod)
            }, CONFLICT_IGNORE)
        }
        compileStatement("SELECT tcsid FROM $TABLE_TEK_CHECK_SINGLE WHERE keyData = ? AND rollingStartNumber = ? AND rollingPeriod = ?").use {
            it.bindBlob(1, key.keyData)
            it.bindLong(2, key.rollingStartIntervalNumber.toLong())
            it.bindLong(3, key.rollingPeriod.toLong())
            it.simpleQueryForLong()
        }
    }

    fun getTokenId(packageName: String, token: String, database: SQLiteDatabase = readableDatabase) = database.run {
        query(TABLE_TOKENS, arrayOf("tid"), "package = ? AND token = ?", arrayOf(packageName, token), null, null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getLong(0)
            } else {
                null
            }
        }
    }

    fun getOrCreateTokenId(packageName: String, token: String, database: SQLiteDatabase = writableDatabase) = database.run {
        val tid = getTokenId(packageName, token, this)
        if (tid != null) {
            tid
        } else {
            insert(TABLE_TOKENS, "NULL", ContentValues().apply {
                put("package", packageName)
                put("token", token)
                put("timestamp", System.currentTimeMillis())
            })
            getTokenId(packageName, token, this)
        }
    }

    private fun storeSingleDiagnosisKey(tid: Long, key: TemporaryExposureKey, database: SQLiteDatabase = writableDatabase) = database.run {
        val tcsid = getTekCheckSingleId(key, true, database)
        insert(TABLE_TEK_CHECK_SINGLE_TOKEN, "NULL", ContentValues().apply {
            put("tid", tid)
            put("tcsid", tcsid)
            put("transmissionRiskLevel", key.transmissionRiskLevel)
            put("reportType", key.reportType)
            put("daysSinceOnsetOfSymptoms", key.daysSinceOnsetOfSymptoms)
        })
    }

    fun batchStoreSingleDiagnosisKey(tid: Long, keys: List<TemporaryExposureKey>, database: SQLiteDatabase = writableDatabase) = database.run {
        beginTransactionNonExclusive()
        try {
            keys.forEach { storeSingleDiagnosisKey(tid, it, database) }
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    fun getDiagnosisFileId(hash: ByteArray, database: SQLiteDatabase = readableDatabase) = database.run {
        val hexHash = ByteString.of(*hash).hex()
        query(TABLE_TEK_CHECK_FILE, arrayOf("tcfid"), "hash = ?", arrayOf(hexHash), null, null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getLong(0)
            } else {
                null
            }
        }
    }

    fun storeDiagnosisFileUsed(tid: Long, tcfid: Long, database: SQLiteDatabase = writableDatabase) = database.run {
        insert(TABLE_TEK_CHECK_FILE_TOKEN, "NULL", ContentValues().apply {
            put("tid", tid)
            put("tcfid", tcfid)
        })
    }

    fun storeDiagnosisFileUsed(tid: Long, hash: ByteArray, database: SQLiteDatabase = writableDatabase) = database.run {
        val hexHash = ByteString.of(*hash).hex()
        query(TABLE_TEK_CHECK_FILE, arrayOf("tcfid", "keys"), "hash = ?", arrayOf(hexHash), null, null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                insertWithOnConflict(TABLE_TEK_CHECK_FILE_TOKEN, "NULL", ContentValues().apply {
                    put("tid", tid)
                    put("tcfid", cursor.getLong(0))
                }, CONFLICT_IGNORE)
                cursor.getLong(1)
            } else {
                null
            }
        }
    }

    private fun listSingleDiagnosisKeysPendingSearch(tid: Long, database: SQLiteDatabase = readableDatabase) = database.run {
        rawQuery("""
            SELECT $TABLE_TEK_CHECK_SINGLE.keyData, $TABLE_TEK_CHECK_SINGLE.rollingStartNumber, $TABLE_TEK_CHECK_SINGLE.rollingPeriod
            FROM $TABLE_TEK_CHECK_SINGLE_TOKEN
            LEFT JOIN $TABLE_TEK_CHECK_SINGLE ON $TABLE_TEK_CHECK_SINGLE.tcsid = $TABLE_TEK_CHECK_SINGLE_TOKEN.tcsid
            WHERE 
                $TABLE_TEK_CHECK_SINGLE_TOKEN.tid = ? AND
                $TABLE_TEK_CHECK_SINGLE.matched IS NULL
                """, arrayOf(tid.toString())).use { cursor ->
            val list = arrayListOf<TemporaryExposureKey>()
            while (cursor.moveToNext()) {
                list.add(TemporaryExposureKey.TemporaryExposureKeyBuilder()
                        .setKeyData(cursor.getBlob(0))
                        .setRollingStartIntervalNumber(cursor.getLong(1).toInt())
                        .setRollingPeriod(cursor.getLong(2).toInt())
                        .build())
            }
            list
        }
    }

    private fun applySingleDiagnosisKeySearchResult(key: TemporaryExposureKey, matched: Boolean, database: SQLiteDatabase = writableDatabase) = database.run {
        compileStatement("UPDATE $TABLE_TEK_CHECK_SINGLE SET matched = ? WHERE keyData = ? AND rollingStartNumber = ? AND rollingPeriod = ?;").use {
            it.bindLong(1, if (matched) 1 else 0)
            it.bindBlob(2, key.keyData)
            it.bindLong(3, key.rollingStartIntervalNumber.toLong())
            it.bindLong(4, key.rollingPeriod.toLong())
            it.executeUpdateDelete()
        }
    }

    private fun applyDiagnosisFileKeySearchResult(tcfid: Long, key: TemporaryExposureKey, database: SQLiteDatabase = writableDatabase) = database.run {
        insert(TABLE_TEK_CHECK_FILE_MATCH, "NULL", ContentValues().apply {
            put("tcfid", tcfid)
            put("keyData", key.keyData)
            put("rollingStartNumber", key.rollingStartIntervalNumber)
            put("rollingPeriod", key.rollingPeriod)
            put("transmissionRiskLevel", key.transmissionRiskLevel)
            put("reportType", key.reportType)
            put("daysSinceOnsetOfSymptoms", key.daysSinceOnsetOfSymptoms)
        })
    }

    private fun listMatchedSingleDiagnosisKeys(tid: Long, database: SQLiteDatabase = readableDatabase) = database.run {
        rawQuery("""
            SELECT $TABLE_TEK_CHECK_SINGLE.keyData, $TABLE_TEK_CHECK_SINGLE.rollingStartNumber, $TABLE_TEK_CHECK_SINGLE.rollingPeriod, $TABLE_TEK_CHECK_SINGLE_TOKEN.transmissionRiskLevel, $TABLE_TEK_CHECK_SINGLE_TOKEN.reportType, $TABLE_TEK_CHECK_SINGLE_TOKEN.daysSinceOnsetOfSymptoms
            FROM $TABLE_TEK_CHECK_SINGLE_TOKEN
            JOIN $TABLE_TEK_CHECK_SINGLE ON $TABLE_TEK_CHECK_SINGLE.tcsid = $TABLE_TEK_CHECK_SINGLE_TOKEN.tcsid
            WHERE 
                $TABLE_TEK_CHECK_SINGLE_TOKEN.tid = ? AND
                $TABLE_TEK_CHECK_SINGLE.matched = 1
                """, arrayOf(tid.toString())).use { cursor ->
            val list = arrayListOf<TemporaryExposureKey>()
            while (cursor.moveToNext()) {
                list.add(TemporaryExposureKey.TemporaryExposureKeyBuilder()
                        .setKeyData(cursor.getBlob(0))
                        .setRollingStartIntervalNumber(cursor.getLong(1).toInt())
                        .setRollingPeriod(cursor.getLong(2).toInt())
                        .setTransmissionRiskLevel(cursor.getLong(3).toInt())
                        .setReportType(cursor.getLong(4).toInt())
                        .setDaysSinceOnsetOfSymptoms(cursor.getLong(5).toInt())
                        .build())
            }
            list
        }
    }

    private fun listMatchedFileDiagnosisKeys(tid: Long, database: SQLiteDatabase = readableDatabase) = database.run {
        rawQuery("""
            SELECT $TABLE_TEK_CHECK_FILE_MATCH.keyData, $TABLE_TEK_CHECK_FILE_MATCH.rollingStartNumber, $TABLE_TEK_CHECK_FILE_MATCH.rollingPeriod, $TABLE_TEK_CHECK_FILE_MATCH.transmissionRiskLevel, $TABLE_TEK_CHECK_FILE_MATCH.reportType, $TABLE_TEK_CHECK_FILE_MATCH.daysSinceOnsetOfSymptoms
            FROM $TABLE_TEK_CHECK_FILE_TOKEN
            JOIN $TABLE_TEK_CHECK_FILE_MATCH ON $TABLE_TEK_CHECK_FILE_MATCH.tcfid = $TABLE_TEK_CHECK_FILE_TOKEN.tcfid
            WHERE 
                $TABLE_TEK_CHECK_FILE_TOKEN.tid = ?
                """, arrayOf(tid.toString())).use { cursor ->
            val list = arrayListOf<TemporaryExposureKey>()
            while (cursor.moveToNext()) {
                list.add(TemporaryExposureKey.TemporaryExposureKeyBuilder()
                        .setKeyData(cursor.getBlob(0))
                        .setRollingStartIntervalNumber(cursor.getLong(1).toInt())
                        .setRollingPeriod(cursor.getLong(2).toInt())
                        .setTransmissionRiskLevel(cursor.getLong(3).toInt())
                        .setReportType(cursor.getLong(4).toInt())
                        .setDaysSinceOnsetOfSymptoms(cursor.getLong(5).toInt())
                        .build())
            }
            list
        }
    }

    fun finishSingleMatching(tid: Long, database: SQLiteDatabase = writableDatabase): Int {
        val workQueue = LinkedBlockingQueue<Runnable>()
        val poolSize = Runtime.getRuntime().availableProcessors()
        val executor = ThreadPoolExecutor(poolSize, poolSize, 1, TimeUnit.SECONDS, workQueue)
        val futures = arrayListOf<Future<*>>()
        val keys = listSingleDiagnosisKeysPendingSearch(tid, database)
        val oldestRpi = oldestRpi
        for (key in keys) {
            if ((key.rollingStartIntervalNumber + key.rollingPeriod).toLong() * ROLLING_WINDOW_LENGTH_MS + ALLOWED_KEY_OFFSET_MS < oldestRpi) {
                // Early ignore because key is older than since we started scanning.
                applySingleDiagnosisKeySearchResult(key, false, database)
            } else {
                futures.add(executor.submit {
                    applySingleDiagnosisKeySearchResult(key, findMeasuredExposures(key).isNotEmpty(), database)
                })
            }
        }
        for (future in futures) {
            future.get()
        }
        executor.shutdown()
        return keys.size
    }

    fun finishFileMatching(tid: Long, hash: ByteArray, endTimestamp: Long, keys: List<TemporaryExposureKey>, updates: List<TemporaryExposureKey>, database: SQLiteDatabase = writableDatabase) = database.run {
        beginTransactionNonExclusive()
        try {
            insert(TABLE_TEK_CHECK_FILE, "NULL", ContentValues().apply {
                put("hash", ByteString.of(*hash).hex())
                put("endTimestamp", endTimestamp)
                put("keys", keys.size + updates.size)
            })
            val tcfid = getDiagnosisFileId(hash, this) ?: return
            val workQueue = LinkedBlockingQueue<Runnable>()
            val poolSize = Runtime.getRuntime().availableProcessors()
            val executor = ThreadPoolExecutor(poolSize, poolSize, 1, TimeUnit.SECONDS, workQueue)
            val futures = arrayListOf<Future<TemporaryExposureKey?>>()
            val oldestRpi = oldestRpi
            var ignored = 0
            var processed = 0
            var found = 0
            var riskLogged = -1
            var startLogged = -1
            for (key in keys) {
                if (key.transmissionRiskLevel > riskLogged || key.rollingStartIntervalNumber > startLogged) {
                    riskLogged = key.transmissionRiskLevel
                    startLogged = key.rollingStartIntervalNumber
                    Log.d(TAG, "First key with risk ${key.transmissionRiskLevel}: ${ByteString.of(*key.keyData).hex()} starts ${key.rollingStartIntervalNumber}")
                }
                if ((key.rollingStartIntervalNumber + key.rollingPeriod).toLong() * ROLLING_WINDOW_LENGTH_MS + ALLOWED_KEY_OFFSET_MS < oldestRpi) {
                    // Early ignore because key is older than since we started scanning.
                    ignored++;
                } else {
                    futures.add(executor.submit(Callable {
                        processed++
                        if (findMeasuredExposures(key).isNotEmpty()) {
                            key
                        } else {
                            null
                        }
                    }))
                }
            }
            for (future in futures) {
                future.get()?.let {
                    applyDiagnosisFileKeySearchResult(tcfid, it, this)
                    found++
                }
            }
            Log.d(TAG, "Processed $processed keys, found $found matches, ignored $ignored keys that are older than our scanning efforts ($oldestRpi)")
            executor.shutdown()
            for (update in updates) {
                val matched = compileStatement("SELECT COUNT(tcsid) FROM $TABLE_TEK_CHECK_FILE_MATCH WHERE keyData = ? AND rollingStartNumber = ? AND rollingPeriod = ?").use {
                    it.bindBlob(1, update.keyData)
                    it.bindLong(2, update.rollingStartIntervalNumber.toLong())
                    it.bindLong(3, update.rollingPeriod.toLong())
                    it.simpleQueryForLong()
                }
                if (matched > 0) {
                    applyDiagnosisFileKeySearchResult(tcfid, update, this)
                }
            }
            insert(TABLE_TEK_CHECK_FILE_TOKEN, "NULL", ContentValues().apply {
                put("tid", tid)
                put("tcfid", tcfid)
            })
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private fun findAllSingleMeasuredExposures(tid: Long, database: SQLiteDatabase = readableDatabase): List<MeasuredExposure> {
        return listMatchedSingleDiagnosisKeys(tid, database).flatMap { findMeasuredExposures(it, database) }
    }

    private fun findAllFileMeasuredExposures(tid: Long, database: SQLiteDatabase = readableDatabase): List<MeasuredExposure> {
        return listMatchedFileDiagnosisKeys(tid, database).flatMap { findMeasuredExposures(it, database) }
    }

    fun findAllMeasuredExposures(tid: Long, database: SQLiteDatabase = readableDatabase) = findAllSingleMeasuredExposures(tid, database) + findAllFileMeasuredExposures(tid, database)

    private fun findMeasuredExposures(key: TemporaryExposureKey, database: SQLiteDatabase = readableDatabase): List<MeasuredExposure> {
        val allRpis = key.generateAllRpiIds()
        val rpis = (0 until key.rollingPeriod).map { i ->
            val pos = i * 16
            allRpis.sliceArray(pos until (pos + 16))
        }
        val measures = findExposures(rpis, key.rollingStartIntervalNumber.toLong() * ROLLING_WINDOW_LENGTH_MS - ALLOWED_KEY_OFFSET_MS, (key.rollingStartIntervalNumber.toLong() + key.rollingPeriod) * ROLLING_WINDOW_LENGTH_MS + ALLOWED_KEY_OFFSET_MS, database)
        return measures.filter {
            val index = rpis.indexOfFirst { rpi -> rpi.contentEquals(it.rpi) }
            val targetTimestamp = (key.rollingStartIntervalNumber + index).toLong() * ROLLING_WINDOW_LENGTH_MS
            it.timestamp >= targetTimestamp - ALLOWED_KEY_OFFSET_MS && it.timestamp <= targetTimestamp + ROLLING_WINDOW_LENGTH_MS + ALLOWED_KEY_OFFSET_MS
        }.mapNotNull {
            val decrypted = key.cryptAem(it.rpi, it.aem)
            val version = (decrypted[0] and 0xf0.toByte())
            val txPower = if (decrypted.size >= 4 && version >= VERSION_1_0) decrypted[1].toInt() else averageDeviceInfo.txPowerCorrection.toInt()
            val confidence = if (decrypted.size >= 4 && version >= VERSION_1_1) ((decrypted[0] and 0xc) / 4) else (averageDeviceInfo.confidence)
            if (version > VERSION_1_1) {
                Log.w(TAG, "Unknown AEM version: 0x${version.toString(16)}")
            }
            MeasuredExposure(it.timestamp, it.duration, it.rssi, txPower, confidence, key)
        }
    }

    private fun findExposures(rpis: List<ByteArray>, minTime: Long, maxTime: Long, database: SQLiteDatabase = readableDatabase): List<PlainExposure> = database.run {
        if (rpis.isEmpty()) return emptyList()
        val qs = rpis.map { "?" }.joinToString(",")
        queryWithFactory({ _, cursorDriver, editTable, query ->
            query.bindLong(1, minTime)
            query.bindLong(2, maxTime)
            rpis.forEachIndexed { index, rpi ->
                query.bindBlob(index + 3, rpi)
            }
            SQLiteCursor(cursorDriver, editTable, query)
        }, false, TABLE_ADVERTISEMENTS, arrayOf("rpi", "aem", "timestamp", "duration", "rssi"), "timestamp > ? AND timestamp < ? AND rpi IN ($qs)", null, null, null, null, null).use { cursor ->
            val list = arrayListOf<PlainExposure>()
            while (cursor.moveToNext()) {
                list.add(PlainExposure(cursor.getBlob(0), cursor.getBlob(1), cursor.getLong(2), cursor.getLong(3), cursor.getInt(4)))
            }
            list
        }
    }

    fun findExposure(rpi: ByteArray, minTime: Long, maxTime: Long): PlainExposure? = readableDatabase.run {
        queryWithFactory({ _, cursorDriver, editTable, query ->
            query.bindBlob(1, rpi)
            query.bindLong(2, minTime)
            query.bindLong(3, maxTime)
            SQLiteCursor(cursorDriver, editTable, query)
        }, false, TABLE_ADVERTISEMENTS, arrayOf("aem", "timestamp", "duration", "rssi"), "rpi = ? AND timestamp > ? AND timestamp < ?", null, null, null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                PlainExposure(rpi, cursor.getBlob(0), cursor.getLong(1), cursor.getLong(2), cursor.getInt(3))
            } else {
                null
            }
        }
    }

    private fun findOwnKeyAt(intervalNumber: Int, database: SQLiteDatabase = readableDatabase): TemporaryExposureKey? = database.run {
        val dayRollingStartNumber = getDayRollingStartNumber(intervalNumber)
        query(TABLE_TEK, arrayOf("keyData", "rollingStartNumber", "rollingPeriod"), "rollingStartNumber >= ? AND (rollingStartNumber + rollingPeriod) < ?", arrayOf(dayRollingStartNumber.toString(), intervalNumber.toString()), null, null, "rollingStartNumber DESC").use { cursor ->
            if (cursor.moveToNext()) {
                TemporaryExposureKey.TemporaryExposureKeyBuilder()
                        .setKeyData(cursor.getBlob(0))
                        .setRollingStartIntervalNumber(cursor.getLong(1).toInt())
                        .setRollingPeriod(cursor.getLong(2).toInt())
                        .build()
            } else {
                null
            }
        }
    }

    fun Parcelable.marshall(): ByteArray {
        val parcel = Parcel.obtain()
        writeToParcel(parcel, 0)
        val bytes = parcel.marshall()
        parcel.recycle()
        return bytes
    }

    fun <T> Parcelable.Creator<T>.unmarshall(data: ByteArray): T {
        val parcel = Parcel.obtain()
        parcel.unmarshall(data, 0, data.size)
        parcel.setDataPosition(0)
        val res = createFromParcel(parcel)
        parcel.recycle()
        return res
    }

    fun storeConfiguration(packageName: String, token: String, configuration: ExposureConfiguration, database: SQLiteDatabase = writableDatabase) = database.run {
        val update = update(TABLE_TOKENS, ContentValues().apply { put("configuration", configuration.marshall()) }, "package = ? AND token = ?", arrayOf(packageName, token))
        if (update <= 0) {
            insert(TABLE_TOKENS, "NULL", ContentValues().apply {
                put("package", packageName)
                put("token", token)
                put("timestamp", System.currentTimeMillis())
                put("configuration", configuration.marshall())
            })
        }
        getTokenId(packageName, token, database)
    }

    fun storeConfiguration(packageName: String, token: String, mapping: DiagnosisKeysDataMapping, database: SQLiteDatabase = writableDatabase) = database.run {
        val update = update(TABLE_TOKENS, ContentValues().apply { put("diagnosisKeysDataMap", mapping.marshall()) }, "package = ? AND token = ?", arrayOf(packageName, token))
        if (update <= 0) {
            insert(TABLE_TOKENS, "NULL", ContentValues().apply {
                put("package", packageName)
                put("token", token)
                put("timestamp", System.currentTimeMillis())
                put("diagnosisKeysDataMap", mapping.marshall())
            })
        }
        getTokenId(packageName, token, database)
    }

    fun loadConfiguration(packageName: String, token: String, database: SQLiteDatabase = readableDatabase): Triple<Long, ExposureConfiguration?, DiagnosisKeysDataMapping?>? = database.run {
        query(TABLE_TOKENS, arrayOf("tid", "configuration", "diagnosisKeysDataMap"), "package = ? AND token = ?", arrayOf(packageName, token), null, null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                val configuration = try {ExposureConfiguration.CREATOR.unmarshall(cursor.getBlob(1)) } catch (e: Exception) { null }
                val diagnosisKeysDataMapping = try { DiagnosisKeysDataMapping.CREATOR.unmarshall(cursor.getBlob(2)) }  catch (e: Exception) { null }
                Triple(cursor.getLong(0), configuration, diagnosisKeysDataMapping)
            } else {
                null
            }
        }
    }

    fun exportKeys(database: SQLiteDatabase = writableDatabase): List<TemporaryExposureKey> = database.run {
        database.beginTransactionNonExclusive()
        try {
            val intervalNumber = currentIntervalNumber
            val key = findOwnKeyAt(intervalNumber, database)
            if (key != null && intervalNumber != key.rollingStartIntervalNumber) {
                // Rotate key
                update(TABLE_TEK, ContentValues().apply {
                    put("rollingPeriod", intervalNumber - key.rollingStartIntervalNumber)
                }, "rollingStartNumber = ?", arrayOf(key.rollingStartIntervalNumber.toString()))
                storeOwnKey(generateCurrentDayTemporaryExposureKey(), database)
            }
            database.setTransactionSuccessful()
            val startRollingNumber = (getDayRollingStartNumber(intervalNumber) - 14 * ROLLING_PERIOD)
            query(TABLE_TEK, arrayOf("keyData", "rollingStartNumber", "rollingPeriod"), "rollingStartNumber >= ? AND (rollingStartNumber + rollingPeriod) <= ?", arrayOf(startRollingNumber.toString(), intervalNumber.toString()), null, null, null).use { cursor ->
                val list = arrayListOf<TemporaryExposureKey>()
                while (cursor.moveToNext()) {
                    list.add(TemporaryExposureKey.TemporaryExposureKeyBuilder()
                            .setKeyData(cursor.getBlob(0))
                            .setRollingStartIntervalNumber(cursor.getLong(1).toInt())
                            .setRollingPeriod(cursor.getLong(2).toInt())
                            .build())
                }
                list
            }
        } finally {
            database.endTransaction()
        }
    }

    val rpiHourHistogram: Set<ExposureScanSummary>
        get() = readableDatabase.run {
            rawQuery("SELECT round(timestamp/(60*60*1000))*60*60*1000, COUNT(*), COUNT(*) FROM $TABLE_ADVERTISEMENTS WHERE timestamp > ? GROUP BY round(timestamp/(60*60*1000)) ORDER BY timestamp ASC;", arrayOf((System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000L)).toString())).use { cursor ->
                val set = hashSetOf<ExposureScanSummary>()
                while (cursor.moveToNext()) {
                    set.add(ExposureScanSummary(cursor.getLong(0), cursor.getInt(1), cursor.getInt(2)))
                }
                set
            }
        }

    val hourRpiCount: Long
        get() = readableDatabase.run {
            rawQuery("SELECT COUNT(*) FROM $TABLE_ADVERTISEMENTS WHERE timestamp > ?;", arrayOf((Date().time - (60 * 60 * 1000)).toString())).use { cursor ->
                if (cursor.moveToNext()) {
                    cursor.getLong(0)
                } else {
                    0L
                }
            }
        }

    val oldestRpi: Long
        get() = readableDatabase.run {
            query(TABLE_ADVERTISEMENTS, arrayOf("MIN(timestamp)"), null, null, null, null, null).use { cursor ->
                if (cursor.moveToNext()) {
                    cursor.getLong(0).let { if (it == 0L) System.currentTimeMillis() else it }
                } else {
                    System.currentTimeMillis()
                }
            }
        }

    val appList: List<String>
        get() = readableDatabase.run {
            query(true, TABLE_APP_LOG, arrayOf("package"), null, null, null, null, "timestamp DESC", null).use { cursor ->
                val list = arrayListOf<String>()
                while (cursor.moveToNext()) {
                    list.add(cursor.getString(0))
                }
                list
            }
        }

    fun countMethodCalls(packageName: String, method: String): Int = readableDatabase.run {
        query(TABLE_APP_LOG, arrayOf("COUNT(*)"), "package = ? AND method = ? AND timestamp > ?", arrayOf(packageName, method, (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(KEEP_DAYS.toLong())).toString()), null, null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getInt(0)
            } else {
                0
            }
        }
    }

    fun lastMethodCall(packageName: String, method: String): Long? = readableDatabase.run {
        query(TABLE_APP_LOG, arrayOf("MAX(timestamp)"), "package = ? AND method = ?", arrayOf(packageName, method), null, null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getLong(0)
            } else {
                null
            }
        }
    }

    fun lastMethodCallArgs(packageName: String, method: String): String? = readableDatabase.run {
        query(TABLE_APP_LOG, arrayOf("args"), "package = ? AND method = ?", arrayOf(packageName, method), null, null, "timestamp DESC", "1").use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getString(0)
            } else {
                null
            }
        }
    }

    fun countDiagnosisKeysInvolved(tid: Long): Long = readableDatabase.run {
        val fromFile = rawQuery("SELECT SUM($TABLE_TEK_CHECK_FILE.keys) AS keys FROM $TABLE_TEK_CHECK_FILE_TOKEN JOIN $TABLE_TEK_CHECK_FILE ON $TABLE_TEK_CHECK_FILE_TOKEN.tcfid = $TABLE_TEK_CHECK_FILE.tcfid WHERE $TABLE_TEK_CHECK_FILE_TOKEN.tid = $tid;", null).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getLong(0)
            } else {
                0
            }
        }
        val single = rawQuery("SELECT COUNT(*) as keys FROM $TABLE_TEK_CHECK_SINGLE_TOKEN WHERE $TABLE_TEK_CHECK_SINGLE_TOKEN.tid = $tid;", null).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getLong(0)
            } else {
                0
            }
        }
        return fromFile + single
    }

    fun methodUsageHistogram(packageName: String): List<Pair<String, Int>> = readableDatabase.run {
        val list = arrayListOf<Pair<String, Int>>()
        rawQuery("SELECT method, COUNT(*) AS count FROM $TABLE_APP_LOG WHERE package = ? GROUP BY method;", arrayOf(packageName)).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0) to cursor.getInt(1))
            }
        }
        list.sortedByDescending { it.second }
    }

    private fun ensureTemporaryExposureKey(): TemporaryExposureKey = writableDatabase.let { database ->
        database.beginTransactionNonExclusive()
        try {
            var key = findOwnKeyAt(currentIntervalNumber, database)
            if (key == null) {
                key = generateCurrentDayTemporaryExposureKey()
                storeOwnKey(key, database)
            }
            database.setTransactionSuccessful()
            key
        } finally {
            database.endTransaction()
        }
    }

    val currentRpiId: UUID?
        get() {
            val key = findOwnKeyAt(currentIntervalNumber) ?: return null
            val buffer = ByteBuffer.wrap(key.generateRpiId(currentIntervalNumber))
            return UUID(buffer.long, buffer.long)
        }

    fun generateCurrentPayload(metadata: ByteArray) = ensureTemporaryExposureKey().generatePayload(currentIntervalNumber, metadata)

    override fun getWritableDatabase(): SQLiteDatabase {
        requirePrimary(this)
        return super.getWritableDatabase()
    }

    @Synchronized
    fun ref(): ExposureDatabase {
        refCount++
        return this
    }

    @Synchronized
    fun unref() {
        refCount--
        if (refCount == 0) {
            clearInstance(this)
            close()
        } else if (refCount < 0) {
            throw IllegalStateException("ref/unref mismatch")
        }
    }

    companion object {
        private const val DB_NAME = "exposure.db"
        private const val DB_VERSION = 12
        private const val DB_SIZE_TOO_LARGE = 256L * 1024 * 1024
        private const val MAX_DELETE_TIME = 5000L
        private const val TABLE_ADVERTISEMENTS = "advertisements"
        private const val TABLE_APP = "app"
        private const val TABLE_APP_LOG = "app_log"
        private const val TABLE_TEK = "tek"
        private const val TABLE_APP_PERMS = "app_perms"
        private const val TABLE_TOKENS = "tokens"
        private const val TABLE_TEK_CHECK_SINGLE = "tek_check_single"
        private const val TABLE_TEK_CHECK_SINGLE_TOKEN = "tek_check_single_token"
        private const val TABLE_TEK_CHECK_FILE = "tek_check_file"
        private const val TABLE_TEK_CHECK_FILE_TOKEN = "tek_check_file_token"
        private const val TABLE_TEK_CHECK_FILE_MATCH = "tek_check_file_match"

        @Deprecated(message = "No longer supported")
        private const val TABLE_TEK_CHECK = "tek_check"

        @Deprecated(message = "No longer supported")
        private const val TABLE_DIAGNOSIS = "diagnosis"

        @Deprecated(message = "No longer supported")
        private const val TABLE_CONFIGURATIONS = "configurations"

        private var deferredInstance: Deferred<ExposureDatabase>? = null
        private var deferredRefCount: Int = 0
        private var instance: ExposureDatabase? = null

        @Synchronized
        private fun requirePrimary(database: ExposureDatabase) {
            if (database != instance) {
                throw IllegalStateException("Operation requires ${database.hashCode()} to be a primary database instance, but ${instance?.hashCode()} is primary", database.createdAt)
            }
        }

        @Synchronized
        private fun clearInstance(database: ExposureDatabase, errorOnNull: Boolean = true) {
            if (database == instance) {
                if (deferredRefCount == 0) {
                    deferredInstance = null
                    instance = null
                }
            } else if (errorOnNull || instance != null) {
                throw IllegalStateException("Tried to remove database instance ${database.hashCode()}, but ${instance?.hashCode()} is primary", database.createdAt)
            }
        }

        @Synchronized
        private fun getDeferredInstance(): Pair<Deferred<ExposureDatabase>, Boolean> {
            val deferredInstance = deferredInstance
            deferredRefCount++
            return when {
                deferredInstance != null -> deferredInstance to false
                instance != null -> throw IllegalStateException("No deferred database instance, but instance ${instance?.hashCode()} is primary", instance?.createdAt)
                else -> {
                    val newInstance = CompletableDeferred<ExposureDatabase>()
                    this.deferredInstance = newInstance
                    newInstance to true
                }
            }
        }

        @Synchronized
        private fun unrefDeferredInstance() {
            deferredRefCount--;
        }

        @Synchronized
        private fun completeInstance(database: ExposureDatabase) {
            if (instance != null) {
                throw IllegalStateException("Tried to make ${database.hashCode()} the primary, but ${instance?.hashCode()} is currently primary", instance?.createdAt)
            }
            instance = database
        }

        private fun prepareDatabaseMigration(context: Context): Pair<File, File> {
            val dbFile = context.getDatabasePath(DB_NAME)
            val dbWalFile = context.getDatabasePath("$DB_NAME-wal")
            val dbMigrateFile = context.getDatabasePath("$DB_NAME-migrate")
            val dbMigrateWalFile = context.getDatabasePath("$DB_NAME-migrate-wal")
            if (dbFile.length() + dbWalFile.length() > DB_SIZE_TOO_LARGE) {
                Log.d(TAG, "Database file is larger than $DB_SIZE_TOO_LARGE, force clean up")
                if (dbFile.exists()) dbFile.renameTo(dbMigrateFile)
                if (dbWalFile.exists()) dbWalFile.renameTo(dbMigrateWalFile)
            }
            return dbMigrateFile to dbMigrateWalFile
        }

        private fun finishDatabaseMigration(database: ExposureDatabase, dbMigrateFile: File, dbMigrateWalFile: File) {
            if (dbMigrateFile.exists()) {
                val writableDatabase = database.writableDatabase
                writableDatabase.execSQL("ATTACH DATABASE '${dbMigrateFile.absolutePath}' AS old;")
                writableDatabase.beginTransaction()
                try {
                    Log.d(TAG, "Migrating advertisements and TEKs from old database file")
                    writableDatabase.execSQL("INSERT INTO $TABLE_ADVERTISEMENTS SELECT * FROM old.$TABLE_ADVERTISEMENTS;")
                    writableDatabase.execSQL("INSERT INTO $TABLE_TEK SELECT * FROM old.$TABLE_TEK;")
                    Log.d(TAG, "Migration finished successfully")
                    writableDatabase.setTransactionSuccessful()
                } finally {
                    writableDatabase.endTransaction()
                    writableDatabase.execSQL("DETACH DATABASE old;")
                }
            }
            dbMigrateFile.delete()
            dbMigrateWalFile.delete()
        }

        suspend fun ref(context: Context): ExposureDatabase {
            val (instance, new) = getDeferredInstance()
            try {
                if (new) {
                    val newInstance = instance as CompletableDeferred
                    try {
                        val (dbMigrateFile, dbMigrateWalFile) = prepareDatabaseMigration(context)
                        val database = ExposureDatabase(context.applicationContext)
                        try {
                            completeInstance(database)
                            finishDatabaseMigration(database, dbMigrateFile, dbMigrateWalFile)
                            newInstance.complete(database)
                            return database
                        } catch (e: Exception) {
                            clearInstance(database, false)
                            database.close()
                            throw e
                        }
                    } catch (e: Exception) {
                        newInstance.completeExceptionally(e)
                        throw e;
                    }
                } else {
                    return instance.await().ref()
                }
            } finally {
                unrefDeferredInstance()
            }
        }

        fun export(context: Context) {
            // FileProvider cannot directly access /databases, so we have to copy the database first
            // In addition, we filter out only the Advertisements table to not export sensitive TEKs

            // Create a new database which will store only the Advertisements table
            val exportDir = File(context.getCacheDir(), "exposureDatabase")
            exportDir.mkdir()
            val exportFile = File(exportDir, "exposure.db")
            if (exportFile.delete()) {
                Log.d("EN-DB-Exporter", "Deleted old export database.")
            }
            val db = SQLiteDatabase.openOrCreateDatabase(exportFile, null)

            // Attach the full database to the new empty db
            val databaseFile = context.getDatabasePath(DB_NAME);
            db.execSQL("ATTACH '$databaseFile' AS fulldb;")

            // copy TABLE_ADVERTISEMENTS over
            db.execSQL("CREATE TABLE $TABLE_ADVERTISEMENTS AS SELECT * FROM fulldb.$TABLE_ADVERTISEMENTS;")

            // Detach original db, close new db
            db.execSQL("DETACH DATABASE fulldb;")
            db.close()

            // Use the FileProvider to get a content URI for the new DB
            val fileUri: Uri? = try {
                FileProvider.getUriForFile(context,"${context.packageName}.microg.exposure.export", exportFile)
            } catch (e: IllegalArgumentException) {
                Log.e("EN-DB-Exporter", "The database file can't be shared: $exportFile $e")
                null
            }

            // Open a sharesheet
            if (fileUri != null) {
                // Grant temporary read permission to the content URI
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    type = "application/vnd.microg.exposure+sqlite3"
                }

                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }
        }

        @Deprecated(message = "Sync database access is slow", replaceWith = ReplaceWith("with(context, call)"))
        fun <T> withSync(context: Context, call: (ExposureDatabase) -> T): T {
            val it = runBlocking { ref(context) }
            try {
                return call(it)
            } finally {
                it.unref()
            }
        }

        suspend fun <T> with(context: Context, call: suspend (ExposureDatabase) -> T): T = withContext(Dispatchers.IO) {
            val it = ref(context)
            try {
                call(it)
            } finally {
                it.unref()
            }
        }
    }
}
