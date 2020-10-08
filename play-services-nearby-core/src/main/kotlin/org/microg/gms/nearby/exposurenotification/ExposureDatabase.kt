/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
import android.database.sqlite.SQLiteOpenHelper
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import okio.ByteString
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.*
import kotlin.math.roundToInt

@TargetApi(21)
class ExposureDatabase private constructor(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private var refCount = 0

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
            Log.d(TAG, "Dropping legacy tables")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_CONFIGURATIONS;")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_DIAGNOSIS;")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_TEK_CHECK;")
            Log.d(TAG, "Creating tables for version >= 3")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TOKENS(tid INTEGER PRIMARY KEY, package TEXT NOT NULL, token TEXT NOT NULL, timestamp INTEGER NOT NULL, configuration BLOB);")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${TABLE_TOKENS}_package_token ON $TABLE_TOKENS(package, token);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK_CHECK_SINGLE(tcsid INTEGER PRIMARY KEY, keyData BLOB NOT NULL, rollingStartNumber INTEGER NOT NULL, rollingPeriod INTEGER NOT NULL, matched INTEGER);")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${TABLE_TEK_CHECK_SINGLE}_key ON $TABLE_TEK_CHECK_SINGLE(keyData, rollingStartNumber, rollingPeriod);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK_CHECK_SINGLE_TOKEN(tcsid INTEGER REFERENCES $TABLE_TEK_CHECK_SINGLE(tcsid) ON DELETE CASCADE, tid INTEGER REFERENCES $TABLE_TOKENS(tid) ON DELETE CASCADE, transmissionRiskLevel INTEGER NOT NULL, UNIQUE(tcsid, tid));")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_TEK_CHECK_SINGLE_TOKEN}_tid ON $TABLE_TEK_CHECK_SINGLE_TOKEN(tid);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK_CHECK_FILE(tcfid INTEGER PRIMARY KEY, hash TEXT NOT NULL, endTimestamp INTEGER NOT NULL, keys INTEGER NOT NULL);")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${TABLE_TEK_CHECK_FILE}_hash ON $TABLE_TEK_CHECK_FILE(hash);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK_CHECK_FILE_TOKEN(tcfid INTEGER REFERENCES $TABLE_TEK_CHECK_FILE(tcfid) ON DELETE CASCADE, tid INTEGER REFERENCES $TABLE_TOKENS(tid) ON DELETE CASCADE, UNIQUE(tcfid, tid));")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_TEK_CHECK_FILE_TOKEN}_tid ON $TABLE_TEK_CHECK_FILE_TOKEN(tid);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK_CHECK_FILE_MATCH(tcfid INTEGER REFERENCES $TABLE_TEK_CHECK_FILE(tcfid) ON DELETE CASCADE, keyData BLOB NOT NULL, rollingStartNumber INTEGER NOT NULL, rollingPeriod INTEGER NOT NULL, transmissionRiskLevel INTEGER NOT NULL, UNIQUE(tcfid, keyData, rollingStartNumber, rollingPeriod));")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_TEK_CHECK_FILE_MATCH}_tcfid ON $TABLE_TEK_CHECK_FILE_MATCH(tcfid);")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_TEK_CHECK_FILE_MATCH}_key ON $TABLE_TEK_CHECK_FILE_MATCH(keyData, rollingStartNumber, rollingPeriod);")
        }
        Log.d(TAG, "Finished database upgrade")
    }

    fun SQLiteDatabase.delete(table: String, whereClause: String, args: LongArray): Int =
            compileStatement("DELETE FROM $table WHERE $whereClause").use {
                args.forEachIndexed { idx, l -> it.bindLong(idx + 1, l) }
                it.executeUpdateDelete()
            }

    fun dailyCleanup() = writableDatabase.run {
        val rollingStartTime = currentRollingStartNumber * ROLLING_WINDOW_LENGTH * 1000 - TimeUnit.DAYS.toMillis(KEEP_DAYS.toLong())
        val advertisements = delete(TABLE_ADVERTISEMENTS, "timestamp < ?", longArrayOf(rollingStartTime))
        Log.d(TAG, "Deleted on daily cleanup: $advertisements adv")
        val appLogEntries = delete(TABLE_APP_LOG, "timestamp < ?", longArrayOf(rollingStartTime))
        Log.d(TAG, "Deleted on daily cleanup: $appLogEntries applogs")
        val temporaryExposureKeys = delete(TABLE_TEK, "(rollingStartNumber + rollingPeriod) < ?", longArrayOf(rollingStartTime / ROLLING_WINDOW_LENGTH_MS))
        Log.d(TAG, "Deleted on daily cleanup: $temporaryExposureKeys teks")
        val singleCheckedTemporaryExposureKeys = delete(TABLE_TEK_CHECK_SINGLE, "rollingStartNumber < ?", longArrayOf(rollingStartTime / ROLLING_WINDOW_LENGTH_MS - ROLLING_PERIOD))
        Log.d(TAG, "Deleted on daily cleanup: $singleCheckedTemporaryExposureKeys tcss")
        val fileCheckedTemporaryExposureKeys = delete(TABLE_TEK_CHECK_FILE, "endTimestamp < ?", longArrayOf(rollingStartTime))
        Log.d(TAG, "Deleted on daily cleanup: $fileCheckedTemporaryExposureKeys tcfs")
        val appPerms = delete(TABLE_APP_PERMS, "timestamp < ?", longArrayOf(System.currentTimeMillis() - CONFIRM_PERMISSION_VALIDITY))
        Log.d(TAG, "Deleted on daily cleanup: $appPerms perms")
        execSQL("VACUUM;")
        Log.d(TAG, "Done vacuuming")
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
        val update = compileStatement("UPDATE $TABLE_ADVERTISEMENTS SET rssi = ((rssi * duration) + (? * (? - timestamp - duration)) / (? - timestamp)), duration = (? - timestamp) WHERE rpi = ? AND timestamp > ? AND timestamp < ?").run {
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
        update(TABLE_TEK_CHECK_FILE, ContentValues().apply {
            put("matched", 0)
        }, null, null)
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

    private fun storeSingleDiagnosisKey(tid: Long, key: TemporaryExposureKey, database: SQLiteDatabase = writableDatabase) = database.run {
        val tcsid = getTekCheckSingleId(key, true, database)
        insert(TABLE_TEK_CHECK_SINGLE_TOKEN, "NULL", ContentValues().apply {
            put("tid", tid)
            put("tcsid", tcsid)
            put("transmissionRiskLevel", key.transmissionRiskLevel)
        })
    }

    fun batchStoreSingleDiagnosisKey(tid: Long, keys: List<TemporaryExposureKey>, database: SQLiteDatabase = writableDatabase) = database.run {
        beginTransaction()
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
                insert(TABLE_TEK_CHECK_FILE_TOKEN, "NULL", ContentValues().apply {
                    put("tid", tid)
                    put("tcfid", cursor.getLong(0))
                })
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
        })
    }

    private fun listMatchedSingleDiagnosisKeys(tid: Long, database: SQLiteDatabase = readableDatabase) = database.run {
        rawQuery("""
            SELECT $TABLE_TEK_CHECK_SINGLE.keyData, $TABLE_TEK_CHECK_SINGLE.rollingStartNumber, $TABLE_TEK_CHECK_SINGLE.rollingPeriod, $TABLE_TEK_CHECK_SINGLE_TOKEN.transmissionRiskLevel
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
                        .build())
            }
            list
        }
    }

    private fun listMatchedFileDiagnosisKeys(tid: Long, database: SQLiteDatabase = readableDatabase) = database.run {
        rawQuery("""
            SELECT $TABLE_TEK_CHECK_FILE_MATCH.keyData, $TABLE_TEK_CHECK_FILE_MATCH.rollingStartNumber, $TABLE_TEK_CHECK_FILE_MATCH.rollingPeriod, $TABLE_TEK_CHECK_FILE_MATCH.transmissionRiskLevel
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
        beginTransaction()
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
            val futures = arrayListOf<Future<*>>()
            val oldestRpi = oldestRpi
            var ignored = 0
            var processed = 0
            var found = 0
            for (key in keys) {
                if ((key.rollingStartIntervalNumber + key.rollingPeriod).toLong() * ROLLING_WINDOW_LENGTH_MS + ALLOWED_KEY_OFFSET_MS < oldestRpi) {
                    // Early ignore because key is older than since we started scanning.
                    ignored++;
                } else {
                    futures.add(executor.submit {
                        if (findMeasuredExposures(key).isNotEmpty()) {
                            applyDiagnosisFileKeySearchResult(tcfid, key, this)
                            found++;
                        }
                        processed++
                    })
                }
            }
            for (future in futures) {
                future.get()
            }
            Log.d(TAG,  "Processed $processed keys, found $found matches, ignored $ignored keys that are older than our scanning efforts ($oldestRpi)")
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

    fun findAllSingleMeasuredExposures(tid: Long, database: SQLiteDatabase = readableDatabase): List<MeasuredExposure> {
        return listMatchedSingleDiagnosisKeys(tid, database).flatMap { findMeasuredExposures(it, database) }
    }

    fun findAllFileMeasuredExposures(tid: Long, database: SQLiteDatabase = readableDatabase): List<MeasuredExposure> {
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
            if (decrypted[0] == 0x40.toByte() || decrypted[0] == 0x50.toByte()) {
                val txPower = decrypted[1]
                MeasuredExposure(it.timestamp, it.duration, it.rssi, txPower.toInt(), key)
            } else {
                Log.w(TAG, "Unknown AEM version ${decrypted[0]}, ignoring")
                null
            }
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

    private fun findOwnKeyAt(rollingStartNumber: Int, database: SQLiteDatabase = readableDatabase): TemporaryExposureKey? = database.run {
        query(TABLE_TEK, arrayOf("keyData", "rollingStartNumber", "rollingPeriod"), "rollingStartNumber = ?", arrayOf(rollingStartNumber.toString()), null, null, null).use { cursor ->
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

    fun loadConfiguration(packageName: String, token: String, database: SQLiteDatabase = readableDatabase): Pair<Long, ExposureConfiguration>? = database.run {
        query(TABLE_TOKENS, arrayOf("tid", "configuration"), "package = ? AND token = ?", arrayOf(packageName, token), null, null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                cursor.getLong(0) to ExposureConfiguration.CREATOR.unmarshall(cursor.getBlob(1))
            } else {
                null
            }
        }
    }

    val allKeys: List<TemporaryExposureKey> = readableDatabase.run {
        val startRollingNumber = (currentRollingStartNumber - 14 * ROLLING_PERIOD)
        query(TABLE_TEK, arrayOf("keyData", "rollingStartNumber", "rollingPeriod"), "rollingStartNumber >= ? AND rollingStartNumber < ?", arrayOf(startRollingNumber.toString(), currentIntervalNumber.toString()), null, null, null).use { cursor ->
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

    val rpiHistogram: Map<Long, Long>
        get() = readableDatabase.run {
            rawQuery("SELECT round(timestamp/(24*60*60*1000)), COUNT(*) FROM $TABLE_ADVERTISEMENTS WHERE timestamp > ? GROUP BY round(timestamp/(24*60*60*1000)) ORDER BY timestamp ASC;", arrayOf((Date().time - (14 * 24 * 60 * 60 * 1000)).toString())).use { cursor ->
                val map = linkedMapOf<Long, Long>()
                while (cursor.moveToNext()) {
                    map[cursor.getLong(0)] = cursor.getLong(1)
                }
                map
            }
        }

    val totalRpiCount: Long
        get() = readableDatabase.run {
            rawQuery("SELECT COUNT(*) FROM $TABLE_ADVERTISEMENTS WHERE timestamp > ?;", arrayOf((Date().time - (14 * 24 * 60 * 60 * 1000)).toString())).use { cursor ->
                if (cursor.moveToNext()) {
                    cursor.getLong(0)
                } else {
                    0L
                }
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
                    cursor.getLong(0)
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

    private fun ensureTemporaryExposureKey(): TemporaryExposureKey = writableDatabase.let { database ->
        database.beginTransaction()
        try {
            var key = findOwnKeyAt(currentRollingStartNumber.toInt(), database)
            if (key == null) {
                key = generateCurrentTemporaryExposureKey()
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
            val key = findOwnKeyAt(currentRollingStartNumber.toInt()) ?: return null
            val buffer = ByteBuffer.wrap(key.generateRpiId(currentIntervalNumber.toInt()))
            return UUID(buffer.long, buffer.long)
        }

    fun generateCurrentPayload(metadata: ByteArray) = ensureTemporaryExposureKey().generatePayload(currentIntervalNumber.toInt(), metadata)

    override fun getWritableDatabase(): SQLiteDatabase {
        if (this != instance) {
            throw IllegalStateException("Tried to open writable database from secondary instance. We are ${hashCode()} but primary is ${instance?.hashCode()}")
        }
        return super.getWritableDatabase()
    }

    override fun close() {
        synchronized(Companion) {
            super.close()
            instance = null
        }
    }

    fun ref(): ExposureDatabase = synchronized(Companion) {
        refCount++
        return this
    }

    fun unref() = synchronized(Companion) {
        refCount--
        if (refCount == 0) {
            close()
        } else if (refCount < 0) {
            throw IllegalStateException("ref/unref mismatch")
        }
    }

    companion object {
        private const val DB_NAME = "exposure.db"
        private const val DB_VERSION = 5
        private const val TABLE_ADVERTISEMENTS = "advertisements"
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

        private var instance: ExposureDatabase? = null
        fun ref(context: Context): ExposureDatabase = synchronized(this) {
            if (instance == null) {
                instance = ExposureDatabase(context.applicationContext)
                Log.d(TAG, "Created instance ${instance?.hashCode()} of database for ${context.javaClass.name}")
            }
            instance!!.ref()
        }

        fun <T> with(context: Context, call: (ExposureDatabase) -> T): T {
            val it = ref(context)
            try {
                return call(it)
            } finally {
                it.unref()
            }
        }
    }
}
