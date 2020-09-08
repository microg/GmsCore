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
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
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
        if (oldVersion < 1) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_ADVERTISEMENTS(rpi BLOB NOT NULL, aem BLOB NOT NULL, timestamp INTEGER NOT NULL, rssi INTEGER NOT NULL, duration INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(rpi, timestamp));")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_ADVERTISEMENTS}_rpi ON $TABLE_ADVERTISEMENTS(rpi);")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_ADVERTISEMENTS}_timestamp ON $TABLE_ADVERTISEMENTS(timestamp);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_APP_LOG(package TEXT NOT NULL, timestamp INTEGER NOT NULL, method TEXT NOT NULL, args TEXT);")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_APP_LOG}_package_timestamp ON $TABLE_APP_LOG(package, timestamp);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK(keyData BLOB NOT NULL, rollingStartNumber INTEGER NOT NULL, rollingPeriod INTEGER NOT NULL);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_CONFIGURATIONS(package TEXT NOT NULL, token TEXT NOT NULL, configuration BLOB, PRIMARY KEY(package, token))")
        }
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_TEK_CHECK;")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_DIAGNOSIS;")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_TEK_CHECK(tcid INTEGER PRIMARY KEY, keyData BLOB NOT NULL, rollingStartNumber INTEGER NOT NULL, rollingPeriod INTEGER NOT NULL, matched INTEGER, UNIQUE(keyData, rollingStartNumber, rollingPeriod));")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_DIAGNOSIS(package TEXT NOT NULL, token TEXT NOT NULL, tcid INTEGER REFERENCES $TABLE_TEK_CHECK(tcid) ON DELETE CASCADE, transmissionRiskLevel INTEGER NOT NULL);")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_DIAGNOSIS}_package_token ON $TABLE_DIAGNOSIS(package, token);")
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE $TABLE_APP_PERMS(package TEXT NOT NULL, sig TEXT NOT NULL, perm TEXT NOT NULL, timestamp INTEGER NOT NULL);")
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${TABLE_DIAGNOSIS}_tcid ON $TABLE_DIAGNOSIS(tcid);")
        }
    }

    fun SQLiteDatabase.delete(table: String, whereClause: String, args: LongArray): Int =
            compileStatement("DELETE FROM $table WHERE $whereClause").use {
                args.forEachIndexed { idx, l -> it.bindLong(idx + 1, l) }
                it.executeUpdateDelete()
            }

    fun dailyCleanup() = writableDatabase.run {
        beginTransaction()
        try {
            val rollingStartTime = currentRollingStartNumber * ROLLING_WINDOW_LENGTH * 1000 - TimeUnit.DAYS.toMillis(KEEP_DAYS.toLong())
            val advertisements = delete(TABLE_ADVERTISEMENTS, "timestamp < ?", longArrayOf(rollingStartTime))
            Log.d(TAG, "Deleted on daily cleanup: $advertisements adv")
            val appLogEntries = delete(TABLE_APP_LOG, "timestamp < ?", longArrayOf(rollingStartTime))
            Log.d(TAG, "Deleted on daily cleanup: $appLogEntries applogs")
            val temporaryExposureKeys = delete(TABLE_TEK, "(rollingStartNumber + rollingPeriod) < ?", longArrayOf(rollingStartTime / ROLLING_WINDOW_LENGTH_MS))
            Log.d(TAG, "Deleted on daily cleanup: $temporaryExposureKeys teks")
            val checkedTemporaryExposureKeys = delete(TABLE_TEK_CHECK, "rollingStartNumber < ?", longArrayOf(rollingStartTime / ROLLING_WINDOW_LENGTH_MS - ROLLING_PERIOD))
            Log.d(TAG, "Deleted on daily cleanup: $checkedTemporaryExposureKeys cteks")
            val appPerms = delete(TABLE_APP_PERMS, "timestamp < ?", longArrayOf(System.currentTimeMillis() - CONFIRM_PERMISSION_VALIDITY))
            Log.d(TAG, "Deleted on daily cleanup: $appPerms perms")
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
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
        update(TABLE_TEK_CHECK, ContentValues().apply {
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

    private fun getTekCheckId(key: TemporaryExposureKey, mayInsert: Boolean = false, database: SQLiteDatabase = if (mayInsert) writableDatabase else readableDatabase): Long? = database.run {
        if (mayInsert) {
            insertWithOnConflict(TABLE_TEK_CHECK, "NULL", ContentValues().apply {
                put("keyData", key.keyData)
                put("rollingStartNumber", key.rollingStartIntervalNumber)
                put("rollingPeriod", key.rollingPeriod)
            }, CONFLICT_IGNORE)
        }
        compileStatement("SELECT tcid FROM $TABLE_TEK_CHECK WHERE keyData = ? AND rollingStartNumber = ? AND rollingPeriod = ?").use {
            it.bindBlob(1, key.keyData)
            it.bindLong(2, key.rollingStartIntervalNumber.toLong())
            it.bindLong(3, key.rollingPeriod.toLong())
            it.simpleQueryForLong()
        }
    }

    fun storeDiagnosisKey(packageName: String, token: String, key: TemporaryExposureKey, database: SQLiteDatabase = writableDatabase) = database.run {
        val tcid = getTekCheckId(key, true, database)
        insert(TABLE_DIAGNOSIS, "NULL", ContentValues().apply {
            put("package", packageName)
            put("token", token)
            put("tcid", tcid)
            put("transmissionRiskLevel", key.transmissionRiskLevel)
        })
    }

    fun batchStoreDiagnosisKey(packageName: String, token: String, keys: List<TemporaryExposureKey>, database: SQLiteDatabase = writableDatabase) = database.run {
        beginTransaction()
        try {
            keys.forEach { storeDiagnosisKey(packageName, token, it, database) }
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    fun updateDiagnosisKey(packageName: String, token: String, key: TemporaryExposureKey, database: SQLiteDatabase = writableDatabase) = database.run {
        val tcid = getTekCheckId(key, false, database) ?: return 0
        compileStatement("UPDATE $TABLE_DIAGNOSIS SET transmissionRiskLevel = ? WHERE package = ? AND token = ? AND tcid = ?;").use {
            it.bindLong(1, key.transmissionRiskLevel.toLong())
            it.bindString(2, packageName)
            it.bindString(3, token)
            it.bindLong(4, tcid)
            it.executeUpdateDelete()
        }
    }

    fun batchUpdateDiagnosisKey(packageName: String, token: String, keys: List<TemporaryExposureKey>, database: SQLiteDatabase = writableDatabase) = database.run {
        beginTransaction()
        try {
            keys.forEach { updateDiagnosisKey(packageName, token, it, database) }
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private fun listDiagnosisKeysPendingSearch(packageName: String, token: String, database: SQLiteDatabase = readableDatabase) = database.run {
        rawQuery("""
            SELECT $TABLE_TEK_CHECK.keyData, $TABLE_TEK_CHECK.rollingStartNumber, $TABLE_TEK_CHECK.rollingPeriod
            FROM $TABLE_DIAGNOSIS
            LEFT JOIN $TABLE_TEK_CHECK ON $TABLE_DIAGNOSIS.tcid = $TABLE_TEK_CHECK.tcid
            WHERE 
                $TABLE_DIAGNOSIS.package = ? AND 
                $TABLE_DIAGNOSIS.token = ? AND 
                $TABLE_TEK_CHECK.matched IS NULL
                """, arrayOf(packageName, token)).use { cursor ->
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

    private fun applyDiagnosisKeySearchResult(key: TemporaryExposureKey, matched: Boolean, database: SQLiteDatabase = writableDatabase) = database.run {
        compileStatement("UPDATE $TABLE_TEK_CHECK SET matched = ? WHERE keyData = ? AND rollingStartNumber = ? AND rollingPeriod = ?;").use {
            it.bindLong(1, if (matched) 1 else 0)
            it.bindBlob(2, key.keyData)
            it.bindLong(3, key.rollingStartIntervalNumber.toLong())
            it.bindLong(4, key.rollingPeriod.toLong())
            it.executeUpdateDelete()
        }
    }

    private fun listMatchedDiagnosisKeys(packageName: String, token: String, database: SQLiteDatabase = readableDatabase) = database.run {
        rawQuery("""
            SELECT $TABLE_TEK_CHECK.keyData, $TABLE_TEK_CHECK.rollingStartNumber, $TABLE_TEK_CHECK.rollingPeriod, $TABLE_DIAGNOSIS.transmissionRiskLevel
            FROM $TABLE_DIAGNOSIS
            LEFT JOIN $TABLE_TEK_CHECK ON $TABLE_DIAGNOSIS.tcid = $TABLE_TEK_CHECK.tcid
            WHERE 
                $TABLE_DIAGNOSIS.package = ? AND 
                $TABLE_DIAGNOSIS.token = ? AND 
                $TABLE_TEK_CHECK.matched = 1
                """, arrayOf(packageName, token)).use { cursor ->
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

    fun finishMatching(packageName: String, token: String, database: SQLiteDatabase = writableDatabase) {
        val start = System.currentTimeMillis()
        val workQueue = LinkedBlockingQueue<Runnable>()
        val poolSize = Runtime.getRuntime().availableProcessors()
        val executor = ThreadPoolExecutor(poolSize, poolSize, 1, TimeUnit.SECONDS, workQueue)
        val futures = arrayListOf<Future<*>>()
        val keys = listDiagnosisKeysPendingSearch(packageName, token, database)
        val oldestRpi = oldestRpi
        for (key in keys) {
            if (oldestRpi == null || (key.rollingStartIntervalNumber + key.rollingPeriod) * ROLLING_WINDOW_LENGTH_MS + ALLOWED_KEY_OFFSET_MS < oldestRpi) {
                // Early ignore because key is older than since we started scanning.
                applyDiagnosisKeySearchResult(key, false, database)
            } else {
                futures.add(executor.submit {
                    applyDiagnosisKeySearchResult(key, findMeasuredExposures(key).isNotEmpty(), database)
                })
            }
        }
        for (future in futures) {
            future.get()
        }
        val time = (System.currentTimeMillis() - start).toDouble() / 1000.0
        executor.shutdown()
        Log.d(TAG, "Processed ${keys.size} new keys in ${time}s -> ${(keys.size.toDouble() / time * 1000).roundToInt().toDouble() / 1000.0} keys/s")
    }

    fun findAllMeasuredExposures(packageName: String, token: String, database: SQLiteDatabase = readableDatabase): List<MeasuredExposure> {
        return listMatchedDiagnosisKeys(packageName, token, database).flatMap { findMeasuredExposures(it, database) }
    }

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

    fun storeConfiguration(packageName: String, token: String, configuration: ExposureConfiguration) = writableDatabase.run {
        val update = update(TABLE_CONFIGURATIONS, ContentValues().apply { put("configuration", configuration.marshall()) }, "package = ? AND token = ?", arrayOf(packageName, token))
        if (update <= 0) {
            insert(TABLE_CONFIGURATIONS, "NULL", ContentValues().apply {
                put("package", packageName)
                put("token", token)
                put("configuration", configuration.marshall())
            })
        }
    }

    fun loadConfiguration(packageName: String, token: String): ExposureConfiguration? = readableDatabase.run {
        query(TABLE_CONFIGURATIONS, arrayOf("configuration"), "package = ? AND token = ?", arrayOf(packageName, token), null, null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                ExposureConfiguration.CREATOR.unmarshall(cursor.getBlob(0))
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

    val oldestRpi: Long?
        get() = readableDatabase.run {
            query(TABLE_ADVERTISEMENTS, arrayOf("MIN(timestamp)"), null, null, null, null, null).use { cursor ->
                if (cursor.moveToNext()) {
                    cursor.getLong(0)
                } else {
                    null
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
            throw IllegalStateException("Tried to open writable database from secondary instance")
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
        private const val DB_VERSION = 4
        private const val TABLE_ADVERTISEMENTS = "advertisements"
        private const val TABLE_APP_LOG = "app_log"
        private const val TABLE_TEK = "tek"
        private const val TABLE_TEK_CHECK = "tek_check"
        private const val TABLE_DIAGNOSIS = "diagnosis"
        private const val TABLE_CONFIGURATIONS = "configurations"
        private const val TABLE_APP_PERMS = "app_perms"

        private var instance: ExposureDatabase? = null
        fun ref(context: Context): ExposureDatabase = synchronized(this) {
            if (instance == null) {
                instance = ExposureDatabase(context.applicationContext)
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
