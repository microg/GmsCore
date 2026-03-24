/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_ACTIVITY
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_DELETED
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_MEMORY
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_PATH
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_PERIOD_SUMMARY
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_VISIT
import com.google.android.gms.semanticlocationhistory.utils.SegmentConverter
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.semanticlocationhistory.LocationHistorySegmentProto
import java.io.Closeable
import java.io.File
import java.util.Random

class OdlhStorageManager(context: Context) : Closeable {

    companion object {
        private const val TAG = "OdlhStorageManager"

        private const val DATABASE_NAME = "odlh-storage.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_SEMANTIC_SEGMENT = "semantic_segment_table"
        private const val TABLE_EDITED_SEGMENT = "edited_segment_table"
        private const val TABLE_GELLER_METADATA = "geller_metadata"
        private const val TABLE_GELLER_SYNC_STATUS = "geller_sync_status"

        private const val COLUMN_ID = "_id"
        private const val COLUMN_OBFUSCATED_GAIA_ID = "obfuscated_gaia_id"
        private const val COLUMN_SEGMENT_ID = "segment_id"
        private const val COLUMN_SEGMENT_TYPE = "segment_type"
        private const val COLUMN_SEMANTIC_SEGMENT = "semantic_segment"
        private const val COLUMN_START_TIMESTAMP = "start_timestamp_seconds"
        private const val COLUMN_END_TIMESTAMP = "end_timestamp_seconds"
        private const val COLUMN_HIERARCHY_LEVEL = "hierarchy_level"
        private const val COLUMN_FPRINT = "fprint"
        private const val COLUMN_SHOWN_IN_TIMELINE = "shown_in_timeline"
        private const val COLUMN_TIMESTAMP_MILLIS = "timestamp_millis"
        private const val COLUMN_DATABASE_ID = "database_id"
        private const val COLUMN_ORIGIN_ID = "origin_id"
        private const val COLUMN_IS_FINALIZED = "is_finalized"

        private const val COLUMN_BLOCK_START_TIMESTAMP = "block_start_timestamp_seconds"
        private const val COLUMN_BLOCK_END_TIMESTAMP = "block_end_timestamp_seconds"
        private const val COLUMN_IS_EDIT_UPLOADED = "is_edit_uploaded"

        private const val COLUMN_KEY = "metadata_key"
        private const val COLUMN_VALUE = "metadata_value"

        private const val COLUMN_DATA_TYPE = "data_type"
        private const val COLUMN_SYNC_TOKEN = "sync_token"
        private const val COLUMN_LAST_SYNC_TIME = "last_sync_time"

        @Volatile
        private var instance: OdlhStorageManager? = null

        fun getInstance(context: Context): OdlhStorageManager =
            instance ?: synchronized(this) {
                instance ?: OdlhStorageManager(context.applicationContext).also { instance = it }
            }
    }

    private val dbHelper = OdlhDatabaseHelper(context)
    private var database: SQLiteDatabase? = null
    private val lock = Any()

    private fun openDatabase(): SQLiteDatabase = synchronized(lock) {
        database?.takeIf { it.isOpen } ?: dbHelper.writableDatabase.also { database = it }
    }

    fun getDatabaseSize(): Long = runCatching {
        File(dbHelper.readableDatabase.path).length()
    }.getOrElse {
        Log.e(TAG, "Failed to get database size", it)
        -1L
    }

    override fun close() {
        synchronized(lock) {
            database?.close()
            database = null
        }
        dbHelper.close()
    }

    private inline fun <T> withTransaction(crossinline block: (SQLiteDatabase) -> T): T {
        val db = openDatabase()
        db.beginTransaction()
        return try {
            block(db).also { db.setTransactionSuccessful() }
        } finally {
            db.endTransaction()
        }
    }

    fun <T> runInTransaction(block: () -> T): T {
        val db = openDatabase()
        db.beginTransaction()
        return try {
            block().also { db.setTransactionSuccessful() }
        } finally {
            db.endTransaction()
        }
    }

    private inline fun <T> SQLiteDatabase.querySafe(
        table: String,
        columns: Array<String>,
        selection: String,
        selectionArgs: Array<String>,
        orderBy: String? = null,
        crossinline transform: (Cursor) -> T
    ): T = query(table, columns, selection, selectionArgs, null, null, orderBy).use(transform)

    @Volatile
    private var cachedDatabaseId: Long? = null

    fun getDatabaseId(): Long {
        cachedDatabaseId?.let { return it }

        synchronized(lock) {
            cachedDatabaseId?.let { return it }

            val db = openDatabase()

            val existing = db.query(
                TABLE_GELLER_METADATA,
                arrayOf(COLUMN_VALUE),
                "$COLUMN_KEY=?",
                arrayOf("database_id"),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.toLongOrNull() else null
            }

            if (existing != null) {
                cachedDatabaseId = existing
                return existing
            }

            val newId = db.rawQuery(
                "SELECT CAST((julianday('now') - 2440587.5)*86400000000000 AS INTEGER)",
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else System.currentTimeMillis() * 1000000
            }

            db.insertWithOnConflict(
                TABLE_GELLER_METADATA,
                null,
                ContentValues().apply {
                    put(COLUMN_KEY, "database_id")
                    put(COLUMN_VALUE, newId.toString())
                },
                SQLiteDatabase.CONFLICT_REPLACE
            )

            cachedDatabaseId = newId
            Log.d(TAG, "Generated new database_id: $newId")
            return newId
        }
    }

    fun getDeviceTag(accountName: String): Int {
        val key = "deviceTag_$accountName"
        getMetadata(key)?.toIntOrNull()?.let { return it }
        val tag = Random().nextInt(Int.MAX_VALUE)
        setMetadata(key, tag.toString())
        Log.d(TAG, "getDeviceTag: generated new deviceTag=$tag for account=$accountName")
        return tag
    }

    fun getMetadata(key: String): String? = runCatching {
        openDatabase().query(
            TABLE_GELLER_METADATA,
            arrayOf(COLUMN_VALUE),
            "$COLUMN_KEY=?",
            arrayOf(key),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrElse {
        Log.e(TAG, "Failed to get metadata for key=$key", it)
        null
    }

    fun setMetadata(key: String, value: String): Boolean = runCatching {
        openDatabase().insertWithOnConflict(
            TABLE_GELLER_METADATA,
            null,
            ContentValues().apply {
                put(COLUMN_KEY, key)
                put(COLUMN_VALUE, value)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        ) >= 0
    }.getOrElse {
        Log.e(TAG, "Failed to set metadata for key=$key", it)
        false
    }

    fun getSyncToken(dataType: Int): String? = runCatching {
        openDatabase().query(
            TABLE_GELLER_SYNC_STATUS,
            arrayOf(COLUMN_SYNC_TOKEN),
            "$COLUMN_DATA_TYPE=?",
            arrayOf(dataType.toString()),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrElse {
        Log.e(TAG, "Failed to get sync token for dataType=$dataType", it)
        null
    }

    fun saveSyncStatus(dataType: Int, syncToken: String, lastSyncTime: Long = System.currentTimeMillis()): Boolean = runCatching {
        openDatabase().insertWithOnConflict(
            TABLE_GELLER_SYNC_STATUS,
            null,
            ContentValues().apply {
                put(COLUMN_DATA_TYPE, dataType)
                put(COLUMN_SYNC_TOKEN, syncToken)
                put(COLUMN_LAST_SYNC_TIME, lastSyncTime)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        ) >= 0
    }.getOrElse {
        Log.e(TAG, "Failed to save sync status for dataType=$dataType", it)
        false
    }

    fun clearSyncToken(dataType: Int): Boolean = runCatching {
        openDatabase().delete(
            TABLE_GELLER_SYNC_STATUS,
            "$COLUMN_DATA_TYPE=?",
            arrayOf(dataType.toString())
        ) >= 0
    }.getOrElse {
        Log.e(TAG, "Failed to clear sync token for dataType=$dataType", it)
        false
    }

    fun insertSegment(gaiaId: String, segment: SemanticSegment): Long {
        if (gaiaId.isEmpty()) return -1
        return runCatching {
            openDatabase().insertOrThrow(TABLE_SEMANTIC_SEGMENT, null, segment.toContentValues(gaiaId))
        }.getOrElse {
            Log.e(TAG, "Failed to insert segment", it)
            -1
        }
    }

    fun insertOrUpdateSegment(gaiaId: String, segment: SemanticSegment): Long {
        if (gaiaId.isEmpty() || segment.segmentId == null) return -1
        return runCatching {
            openDatabase().insertWithOnConflict(
                TABLE_SEMANTIC_SEGMENT,
                null,
                segment.toContentValues(gaiaId),
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }.getOrElse {
            Log.e(TAG, "Failed to insert or update segment", it)
            -1
        }
    }

    fun insertEditedSegment(gaiaId: String, segment: SemanticSegment): Long {
        if (gaiaId.isEmpty() || segment.segmentId == null) return -1
        return runCatching {
            openDatabase().insertOrThrow(TABLE_EDITED_SEGMENT, null, segment.toEditedContentValues(gaiaId))
        }.getOrElse {
            Log.e(TAG, "Failed to insert edited segment", it)
            -1
        }
    }

    private fun SemanticSegment.toEditedContentValues(gaiaId: String) = ContentValues().apply {
        put(COLUMN_OBFUSCATED_GAIA_ID, gaiaId)
        put(COLUMN_SEGMENT_ID, segmentId)
        put(COLUMN_SEGMENT_TYPE, segmentType)
        put(COLUMN_SEMANTIC_SEGMENT, data)
        put(COLUMN_START_TIMESTAMP, startTimestamp)
        put(COLUMN_END_TIMESTAMP, endTimestamp)
        put(COLUMN_BLOCK_START_TIMESTAMP, blockStartTimestamp ?: startTimestamp)
        put(COLUMN_BLOCK_END_TIMESTAMP, blockEndTimestamp ?: endTimestamp)
        put(COLUMN_HIERARCHY_LEVEL, hierarchyLevel)
        put(COLUMN_IS_EDIT_UPLOADED, if (isEditUploaded) 1 else 0)
    }

    private fun SemanticSegment.toContentValues(gaiaId: String) = ContentValues().apply {
        put(COLUMN_OBFUSCATED_GAIA_ID, gaiaId)
        put(COLUMN_SEGMENT_ID, segmentId)
        put(COLUMN_SEGMENT_TYPE, segmentType)
        put(COLUMN_SEMANTIC_SEGMENT, data)
        put(COLUMN_START_TIMESTAMP, startTimestamp)
        put(COLUMN_END_TIMESTAMP, endTimestamp)
        put(COLUMN_HIERARCHY_LEVEL, hierarchyLevel)
        fprint?.let { put(COLUMN_FPRINT, it) }
        put(COLUMN_SHOWN_IN_TIMELINE, if (shownInTimeline) 1 else 0)
        put(COLUMN_TIMESTAMP_MILLIS, timestampMillis)
        put(COLUMN_DATABASE_ID, databaseId)
        put(COLUMN_ORIGIN_ID, originId)
        put(COLUMN_IS_FINALIZED, if (isFinalized) 1 else 0)
    }

    private val segmentColumns = arrayOf(
        COLUMN_ID, COLUMN_SEGMENT_ID, COLUMN_SEGMENT_TYPE, COLUMN_SEMANTIC_SEGMENT,
        COLUMN_START_TIMESTAMP, COLUMN_END_TIMESTAMP, COLUMN_HIERARCHY_LEVEL,
        COLUMN_FPRINT, COLUMN_SHOWN_IN_TIMELINE, COLUMN_TIMESTAMP_MILLIS,
        COLUMN_DATABASE_ID, COLUMN_ORIGIN_ID, COLUMN_IS_FINALIZED
    )

    private val editedSegmentColumns = arrayOf(
        COLUMN_ID, COLUMN_SEGMENT_ID, COLUMN_SEGMENT_TYPE, COLUMN_SEMANTIC_SEGMENT,
        COLUMN_START_TIMESTAMP, COLUMN_END_TIMESTAMP, COLUMN_HIERARCHY_LEVEL,
        COLUMN_BLOCK_START_TIMESTAMP, COLUMN_BLOCK_END_TIMESTAMP, COLUMN_IS_EDIT_UPLOADED
    )

    private val defaultOrderBy = "$COLUMN_START_TIMESTAMP, CASE $COLUMN_SEGMENT_TYPE WHEN 1 THEN $COLUMN_HIERARCHY_LEVEL ELSE 10 END"

    fun querySegments(gaiaId: String, startTime: Long, endTime: Long, segmentTypes: IntArray? = null, placeFprint: Long? = null, pathMemoryEndTime: Long? = null): List<SemanticSegment> {
        if (gaiaId.isEmpty()) return emptyList()

        return runCatching {
            val visitTypes = intArrayOf(SEGMENT_TYPE_VISIT, SEGMENT_TYPE_ACTIVITY)
            val needVisit = segmentTypes == null || segmentTypes.any { it in visitTypes }
            val needPath = segmentTypes == null || segmentTypes.contains(SEGMENT_TYPE_PATH)
            val needMemory = segmentTypes == null || segmentTypes.contains(SEGMENT_TYPE_MEMORY)
            val needPeriodSummary = segmentTypes == null || segmentTypes.contains(SEGMENT_TYPE_PERIOD_SUMMARY)

            val results = mutableListOf<SemanticSegment>()

            if (needVisit) {
                results.addAll(queryVisitWithEdits(gaiaId, startTime, endTime, placeFprint))
            }

            if (needPath) {
                val pathEnd = pathMemoryEndTime ?: endTime
                results.addAll(queryByTypes(gaiaId, startTime, pathEnd, intArrayOf(SEGMENT_TYPE_PATH)))
            }

            if (needMemory) {
                val memoryEnd = pathMemoryEndTime ?: endTime
                results.addAll(queryByTypes(gaiaId, startTime, memoryEnd, intArrayOf(SEGMENT_TYPE_MEMORY)))
            }

            if (needPeriodSummary) {
                results.addAll(queryByTypes(gaiaId, startTime, endTime, intArrayOf(SEGMENT_TYPE_PERIOD_SUMMARY)))
            }

            results.sortWith(compareBy<SemanticSegment> { it.startTimestamp }
                .thenBy { if (it.segmentType == SEGMENT_TYPE_VISIT) it.hierarchyLevel else 10 })
            results
        }.getOrElse {
            Log.e(TAG, "Failed to query segments", it)
            emptyList()
        }
    }

    private fun queryVisitWithEdits(gaiaId: String, startTime: Long, endTime: Long, placeFprint: Long? = null): List<SemanticSegment> {
        val editedSegments = queryEditedSegments(gaiaId, startTime, endTime)

        var adjustedStart = startTime
        var adjustedEnd = endTime
        if (editedSegments.isNotEmpty()) {
            adjustedStart = minOf(startTime, editedSegments.minOf { it.startTimestamp })
            adjustedEnd = maxOf(endTime, editedSegments.maxOf { it.endTimestamp })
        }

        val visitTypes = intArrayOf(SEGMENT_TYPE_VISIT, SEGMENT_TYPE_ACTIVITY)
        val semanticSegments = queryByTypes(gaiaId, adjustedStart, adjustedEnd, visitTypes, placeFprint)

        return mergeWithEdits(semanticSegments, editedSegments)
    }

    private fun popWithSubVisits(queue: ArrayDeque<SemanticSegment>): List<SemanticSegment> {
        if (queue.isEmpty()) return emptyList()
        val group = mutableListOf(queue.removeFirst())
        while (queue.isNotEmpty() && queue.first().isSubVisit) {
            group.add(queue.removeFirst())
        }
        return group
    }

    private fun popContinuousEditBlock(editedQueue: ArrayDeque<SemanticSegment>): List<SemanticSegment> {
        if (editedQueue.isEmpty()) return emptyList()

        val block = mutableListOf<SemanticSegment>()
        val firstGroup = popWithSubVisits(editedQueue)
        block.addAll(firstGroup)

        var lastNonSubVisitEnd = firstGroup.first().endTimestamp

        while (editedQueue.isNotEmpty()) {
            val next = editedQueue.first()
            if (next.startTimestamp == lastNonSubVisitEnd) {
                val nextGroup = popWithSubVisits(editedQueue)
                block.addAll(nextGroup)
                lastNonSubVisitEnd = nextGroup.first().endTimestamp
            } else {
                break
            }
        }

        return block
    }

    private fun mergeWithEdits(
        semanticSegments: List<SemanticSegment>,
        editedSegments: List<SemanticSegment>
    ): List<SemanticSegment> {
        if (editedSegments.isEmpty()) return semanticSegments
        if (semanticSegments.isEmpty()) return editedSegments.filter { it.segmentType != SEGMENT_TYPE_DELETED }

        val result = mutableListOf<SemanticSegment>()
        val semanticQueue = ArrayDeque(semanticSegments)
        val editedQueue = ArrayDeque(editedSegments)

        while (semanticQueue.isNotEmpty() || editedQueue.isNotEmpty()) {
            if (semanticQueue.isEmpty()) {
                while (editedQueue.isNotEmpty()) {
                    val block = popContinuousEditBlock(editedQueue)
                    result.addAll(block.filter { it.segmentType != SEGMENT_TYPE_DELETED })
                }
                break
            }
            if (editedQueue.isEmpty()) {
                while (semanticQueue.isNotEmpty()) {
                    result.addAll(popWithSubVisits(semanticQueue))
                }
                break
            }

            val semantic = semanticQueue.first()
            val edited = editedQueue.first()

            if (semantic.endTimestamp <= edited.startTimestamp) {
                result.addAll(popWithSubVisits(semanticQueue))
            } else {
                val editBlock = popContinuousEditBlock(editedQueue)
                val editBlockStart = editBlock.first().startTimestamp
                val editBlockEnd = editBlock.last { !it.isSubVisit }.endTimestamp

                var coveredStart = editBlockStart
                var coveredEnd = editBlockEnd
                while (semanticQueue.isNotEmpty()) {
                    val next = semanticQueue.first()
                    if (next.startTimestamp < editBlockEnd && next.endTimestamp > editBlockStart) {
                        val coveredGroup = popWithSubVisits(semanticQueue)
                        val groupMain = coveredGroup.first()
                        coveredStart = minOf(coveredStart, groupMain.startTimestamp)
                        coveredEnd = maxOf(coveredEnd, groupMain.endTimestamp)
                    } else {
                        break
                    }
                }

                val nextEditStart = editedQueue.firstOrNull()?.startTimestamp
                if (nextEditStart != null && coveredEnd > nextEditStart) {
                    coveredEnd = nextEditStart
                }

                val expandedBlock = editBlock.toMutableList()
                if (coveredStart < editBlockStart) {
                    expandedBlock[0] = SegmentConverter.updateSegmentTimestamps(
                        expandedBlock[0], coveredStart, expandedBlock[0].endTimestamp
                    )
                }

                val lastNonSubIdx = expandedBlock.indexOfLast { !it.isSubVisit }
                if (lastNonSubIdx >= 0 && coveredEnd > editBlockEnd) {
                    expandedBlock[lastNonSubIdx] = SegmentConverter.updateSegmentTimestamps(
                        expandedBlock[lastNonSubIdx],
                        expandedBlock[lastNonSubIdx].startTimestamp,
                        coveredEnd
                    )
                }

                result.addAll(expandedBlock.filter { it.segmentType != SEGMENT_TYPE_DELETED })
            }
        }

        return result
    }

    fun mergeSegments(existing: List<SemanticSegment>, newEdits: List<SemanticSegment>): List<SemanticSegment> =
        mergeWithEdits(existing, newEdits)

    private fun queryByTypes(gaiaId: String, startTime: Long, endTime: Long, types: IntArray, placeFprint: Long? = null): List<SemanticSegment> {
        val (selection, args) = buildSegmentQuery(gaiaId, startTime, endTime, types, placeFprint)
        return openDatabase().querySafe(TABLE_SEMANTIC_SEGMENT, segmentColumns, selection, args, defaultOrderBy) { cursor ->
            cursor.toSegmentList { it.segmentType != SEGMENT_TYPE_DELETED }
        }
    }

    private fun buildSegmentQuery(gaiaId: String, startTime: Long, endTime: Long, segmentTypes: IntArray? = null, placeFprint: Long? = null): Pair<String, Array<String>> {
        val conditions = mutableListOf(
            "$COLUMN_OBFUSCATED_GAIA_ID=?",
            "$COLUMN_END_TIMESTAMP>? AND $COLUMN_START_TIMESTAMP<?"
        )
        val args = mutableListOf(gaiaId, startTime.toString(), endTime.toString())

        segmentTypes?.takeIf { it.isNotEmpty() }?.let { types ->
            conditions += "(${types.joinToString(" OR ") { "$COLUMN_SEGMENT_TYPE=?" }})"
            args += types.map { it.toString() }
        }

        placeFprint?.let {
            conditions += "$COLUMN_FPRINT=?"
            args += it.toString()
        }

        return conditions.joinToString(" AND ") to args.toTypedArray()
    }

    fun queryEditedSegments(gaiaId: String, startTime: Long, endTime: Long): List<SemanticSegment> {
        if (gaiaId.isEmpty()) return emptyList()

        return runCatching {
            val selection = "$COLUMN_OBFUSCATED_GAIA_ID=? AND $COLUMN_BLOCK_END_TIMESTAMP>? AND $COLUMN_BLOCK_START_TIMESTAMP<?"
            val args = arrayOf(gaiaId, startTime.toString(), endTime.toString())
            openDatabase().querySafe(TABLE_EDITED_SEGMENT, editedSegmentColumns, selection, args, defaultOrderBy) { cursor ->
                cursor.toSegmentList()
            }
        }.getOrElse {
            Log.e(TAG, "Failed to query edited segments", it)
            emptyList()
        }
    }

    fun queryEditedBlockRanges(gaiaId: String): List<Pair<Long, Long>> {
        if (gaiaId.isEmpty()) return emptyList()
        return runCatching {
            openDatabase().rawQuery(
                """
                SELECT DISTINCT $COLUMN_BLOCK_START_TIMESTAMP, $COLUMN_BLOCK_END_TIMESTAMP
                FROM $TABLE_EDITED_SEGMENT
                WHERE $COLUMN_OBFUSCATED_GAIA_ID = ?
                ORDER BY $COLUMN_BLOCK_START_TIMESTAMP
                """.trimIndent(),
                arrayOf(gaiaId)
            ).use { cursor ->
                val results = mutableListOf<Pair<Long, Long>>()
                while (cursor.moveToNext()) {
                    results.add(cursor.getLong(0) to cursor.getLong(1))
                }
                results
            }
        }.getOrElse {
            Log.e(TAG, "Failed to query edited block ranges", it)
            emptyList()
        }
    }

    fun flushEditsToMainTable(gaiaId: String): Int {
        val blockRanges = queryEditedBlockRanges(gaiaId)
        if (blockRanges.isEmpty()) return 0

        val databaseId = getDatabaseId()
        var flushedCount = 0
        for ((blockStart, blockEnd) in blockRanges) {
            runCatching {
                flushSingleEditBlock(gaiaId, blockStart, blockEnd, databaseId)
                flushedCount++
            }.onFailure {
                Log.e(TAG, "Failed to flush edit block [$blockStart, $blockEnd]", it)
            }
        }
        return flushedCount
    }

    private fun flushSingleEditBlock(gaiaId: String, blockStart: Long, blockEnd: Long, databaseId: Long) {
        runInTransaction {
            val editedSegments = queryEditedSegments(gaiaId, blockStart, blockEnd)
            if (editedSegments.isEmpty()) return@runInTransaction

            var adjustedStart = blockStart
            var adjustedEnd = blockEnd
            if (editedSegments.isNotEmpty()) {
                adjustedStart = minOf(blockStart, editedSegments.minOf { it.startTimestamp })
                adjustedEnd = maxOf(blockEnd, editedSegments.maxOf { it.endTimestamp })
            }

            val visitTypes = intArrayOf(SEGMENT_TYPE_VISIT, SEGMENT_TYPE_ACTIVITY)
            val originals = queryByTypes(gaiaId, adjustedStart, adjustedEnd, visitTypes)

            val merged = mergeWithEdits(originals, editedSegments)

            deleteVisitActivityByTimeRange(gaiaId, adjustedStart, adjustedEnd)

            for (segment in merged) {
                val fprint = extractFprintFromProto(segment.data, segment.segmentType)
                val enriched = segment.copy(
                    fprint = fprint ?: segment.fprint,
                    databaseId = databaseId,
                    isFinalized = true,
                    shownInTimeline = true
                )
                insertOrUpdateSegment(gaiaId, enriched)
            }

            deleteEditedSegmentsByTimeRange(gaiaId, blockStart, blockEnd)
        }
    }

    private fun deleteVisitActivityByTimeRange(gaiaId: String, startTime: Long, endTime: Long): Int {
        if (gaiaId.isEmpty()) return -1
        return deleteSafe(
            TABLE_SEMANTIC_SEGMENT,
            "$COLUMN_OBFUSCATED_GAIA_ID=? AND $COLUMN_END_TIMESTAMP>? AND $COLUMN_START_TIMESTAMP<? AND ($COLUMN_SEGMENT_TYPE=? OR $COLUMN_SEGMENT_TYPE=?)",
            arrayOf(gaiaId, startTime.toString(), endTime.toString(), SEGMENT_TYPE_VISIT.toString(), SEGMENT_TYPE_ACTIVITY.toString())
        )
    }

    private fun extractFprintFromProto(data: ByteArray, segmentType: Int): Long? {
        if (segmentType != SEGMENT_TYPE_VISIT) return null
        return runCatching {
            LocationHistorySegmentProto.ADAPTER.decode(data).segment_data?.visit?.place?.feature_id?.low
        }.getOrNull()
    }

    fun getSegmentCount(gaiaId: String? = null): Int = getCount(TABLE_SEMANTIC_SEGMENT, gaiaId)

    fun getLastSyncTime(dataType: Int): Long? = runCatching {
        openDatabase().query(
            TABLE_GELLER_SYNC_STATUS,
            arrayOf(COLUMN_LAST_SYNC_TIME),
            "$COLUMN_DATA_TYPE=?",
            arrayOf(dataType.toString()),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }
    }.getOrElse {
        Log.e(TAG, "Failed to get last sync time for dataType=$dataType", it)
        null
    }

    private fun getCount(table: String, gaiaId: String?): Int = runCatching {
        val (where, args) = if (gaiaId != null) {
            " WHERE $COLUMN_OBFUSCATED_GAIA_ID=?" to arrayOf(gaiaId)
        } else {
            "" to emptyArray()
        }
        openDatabase().rawQuery("SELECT COUNT(*) FROM $table$where", args).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }.getOrElse {
        Log.e(TAG, "Failed to get count for $table", it)
        -1
    }

    private class ColumnIndices(cursor: Cursor) {
        val id = cursor.getColumnIndex(COLUMN_ID)
        val segmentId = cursor.getColumnIndexOrThrow(COLUMN_SEGMENT_ID)
        val segmentType = cursor.getColumnIndexOrThrow(COLUMN_SEGMENT_TYPE)
        val semanticSegment = cursor.getColumnIndexOrThrow(COLUMN_SEMANTIC_SEGMENT)
        val startTimestamp = cursor.getColumnIndexOrThrow(COLUMN_START_TIMESTAMP)
        val endTimestamp = cursor.getColumnIndexOrThrow(COLUMN_END_TIMESTAMP)
        val hierarchyLevel = cursor.getColumnIndex(COLUMN_HIERARCHY_LEVEL)
        val fprint = cursor.getColumnIndex(COLUMN_FPRINT)
        val shownInTimeline = cursor.getColumnIndex(COLUMN_SHOWN_IN_TIMELINE)
        val timestampMillis = cursor.getColumnIndex(COLUMN_TIMESTAMP_MILLIS)
        val databaseId = cursor.getColumnIndex(COLUMN_DATABASE_ID)
        val originId = cursor.getColumnIndex(COLUMN_ORIGIN_ID)
        val isFinalized = cursor.getColumnIndex(COLUMN_IS_FINALIZED)
        val blockStartTimestamp = cursor.getColumnIndex(COLUMN_BLOCK_START_TIMESTAMP)
        val blockEndTimestamp = cursor.getColumnIndex(COLUMN_BLOCK_END_TIMESTAMP)
        val isEditUploaded = cursor.getColumnIndex(COLUMN_IS_EDIT_UPLOADED)
    }

    private inline fun Cursor.toSegmentList(filter: (SemanticSegment) -> Boolean = { true }): List<SemanticSegment> {
        if (!moveToFirst()) return emptyList()
        val cols = ColumnIndices(this)
        val results = mutableListOf<SemanticSegment>()
        do {
            runCatching { cursorToSegment(this, cols) }
                .onSuccess { if (filter(it)) results.add(it) }
                .onFailure { Log.w(TAG, "Failed to parse segment", it) }
        } while (moveToNext())
        return results
    }

    private fun cursorToSegment(cursor: Cursor, cols: ColumnIndices) = SemanticSegment(
        id = cols.id.takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getLong(it) },
        segmentId = cursor.getString(cols.segmentId),
        segmentType = cursor.getInt(cols.segmentType),
        data = cursor.getBlob(cols.semanticSegment),
        startTimestamp = cursor.getLong(cols.startTimestamp),
        endTimestamp = cursor.getLong(cols.endTimestamp),
        hierarchyLevel = cols.hierarchyLevel.takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getInt(it) } ?: 0,
        fprint = cols.fprint.takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getLong(it) },
        shownInTimeline = (cols.shownInTimeline.takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getInt(it) } ?: 1) == 1,
        timestampMillis = cols.timestampMillis.takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getLong(it) } ?: 0L,
        databaseId = cols.databaseId.takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getLong(it) } ?: 0L,
        originId = cols.originId.takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getLong(it) },
        isFinalized = (cols.isFinalized.takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getInt(it) } ?: 0) == 1,
        blockStartTimestamp = cols.blockStartTimestamp.takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getLong(it) },
        blockEndTimestamp = cols.blockEndTimestamp.takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getLong(it) },
        isEditUploaded = (cols.isEditUploaded.takeIf { it >= 0 && !cursor.isNull(it) }?.let { cursor.getInt(it) } ?: 0) == 1
    )

    fun purgeDeletedEdits(gaiaId: String, beforeTimestamp: Long): Int {
        if (gaiaId.isEmpty()) return -1
        return deleteSafe(
            TABLE_EDITED_SEGMENT,
            "$COLUMN_OBFUSCATED_GAIA_ID=? AND $COLUMN_START_TIMESTAMP<=? AND $COLUMN_SEGMENT_TYPE=?",
            arrayOf(gaiaId, beforeTimestamp.toString(), SEGMENT_TYPE_DELETED.toString())
        )
    }

    fun deleteBySegmentId(gaiaId: String, segmentId: String): Int {
        if (gaiaId.isEmpty()) return -1
        return deleteSafe(TABLE_SEMANTIC_SEGMENT, "$COLUMN_OBFUSCATED_GAIA_ID=? AND $COLUMN_SEGMENT_ID=?", arrayOf(gaiaId, segmentId))
    }

    fun deleteSegmentsByTimeRange(gaiaId: String, startTime: Long, endTime: Long): Int {
        if (gaiaId.isEmpty()) return -1
        return deleteSafe(
            TABLE_SEMANTIC_SEGMENT,
            "$COLUMN_OBFUSCATED_GAIA_ID=? AND $COLUMN_END_TIMESTAMP>? AND $COLUMN_START_TIMESTAMP<?",
            arrayOf(gaiaId, startTime.toString(), endTime.toString())
        )
    }

    fun deleteEditedSegmentsByTimeRange(gaiaId: String, startTime: Long, endTime: Long, excludeDeleted: Boolean = false): Int {
        if (gaiaId.isEmpty()) return -1
        val where = buildString {
            append("$COLUMN_OBFUSCATED_GAIA_ID=? AND $COLUMN_BLOCK_END_TIMESTAMP>? AND $COLUMN_BLOCK_START_TIMESTAMP<?")
            if (excludeDeleted) append(" AND $COLUMN_SEGMENT_TYPE != ${SEGMENT_TYPE_DELETED}")
        }
        return deleteSafe(TABLE_EDITED_SEGMENT, where, arrayOf(gaiaId, startTime.toString(), endTime.toString()))
    }

    private fun deleteSafe(table: String, whereClause: String, whereArgs: Array<String>): Int = runCatching {
        openDatabase().delete(table, whereClause, whereArgs)
    }.getOrElse {
        Log.e(TAG, "Failed to delete from $table", it)
        -1
    }

    fun queryShardStates(gaiaId: String, databaseId: Long): Map<Long, ShardState> {
        if (gaiaId.isEmpty()) return emptyMap()
        return runCatching {
            openDatabase().rawQuery(
                """
                SELECT $COLUMN_ID >> 10 AS shard_index,
                       COUNT($COLUMN_ID) AS row_count,
                       MAX($COLUMN_TIMESTAMP_MILLIS) AS max_timestamp_millis
                FROM $TABLE_SEMANTIC_SEGMENT
                WHERE $COLUMN_OBFUSCATED_GAIA_ID = ? AND $COLUMN_DATABASE_ID = ?
                GROUP BY $COLUMN_ID >> 10
                """.trimIndent(),
                arrayOf(gaiaId, databaseId.toString())
            ).use { cursor ->
                val result = mutableMapOf<Long, ShardState>()
                while (cursor.moveToNext()) {
                    val shardIndex = cursor.getLong(0)
                    result[shardIndex] = ShardState(
                        shardIndex = shardIndex,
                        rowCount = cursor.getInt(1),
                        maxTimestampMillis = cursor.getLong(2)
                    )
                }
                result
            }
        }.getOrElse {
            Log.e(TAG, "Failed to query shard states", it)
            emptyMap()
        }
    }

    fun queryRawSegmentRowsForShard(gaiaId: String, databaseId: Long, minId: Long, maxId: Long): List<RawSegmentRow> {
        if (gaiaId.isEmpty()) return emptyList()
        return runCatching {
            val columns = arrayOf(
                COLUMN_ID, COLUMN_TIMESTAMP_MILLIS, COLUMN_DATABASE_ID, COLUMN_ORIGIN_ID,
                COLUMN_SEGMENT_TYPE, COLUMN_SEMANTIC_SEGMENT, COLUMN_START_TIMESTAMP,
                COLUMN_END_TIMESTAMP, COLUMN_HIERARCHY_LEVEL, COLUMN_SHOWN_IN_TIMELINE,
                COLUMN_SEGMENT_ID, COLUMN_IS_FINALIZED
            )
            openDatabase().querySafe(
                TABLE_SEMANTIC_SEGMENT,
                columns,
                "$COLUMN_OBFUSCATED_GAIA_ID=? AND $COLUMN_DATABASE_ID=? AND $COLUMN_ID BETWEEN ? AND ?",
                arrayOf(gaiaId, databaseId.toString(), minId.toString(), maxId.toString()),
                COLUMN_ID
            ) { cursor ->
                val results = mutableListOf<RawSegmentRow>()
                while (cursor.moveToNext()) {
                    runCatching {
                        RawSegmentRow(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            timestampMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP_MILLIS)),
                            databaseId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATABASE_ID)),
                            originId = cursor.getLongOrNull(COLUMN_ORIGIN_ID),
                            segmentType = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SEGMENT_TYPE)),
                            semanticSegment = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_SEMANTIC_SEGMENT)),
                            startTimestampSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START_TIMESTAMP)),
                            endTimestampSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_END_TIMESTAMP)),
                            hierarchyLevel = cursor.getIntOrNull(COLUMN_HIERARCHY_LEVEL),
                            shownInTimeline = (cursor.getIntOrNull(COLUMN_SHOWN_IN_TIMELINE) ?: 1) == 1,
                            segmentId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SEGMENT_ID)),
                            isFinalized = (cursor.getIntOrNull(COLUMN_IS_FINALIZED) ?: 0) == 1
                        )
                    }.onSuccess { results.add(it) }
                        .onFailure { Log.w(TAG, "Failed to parse raw segment row", it) }
                }
                results
            }
        }.getOrElse {
            Log.e(TAG, "Failed to query raw segment rows for shard [$minId, $maxId]", it)
            emptyList()
        }
    }

    fun saveShardSyncSnapshot(gaiaId: String, databaseId: Long, shards: Collection<ShardState>) {
        val key = "shard_sync_${gaiaId}_${databaseId}"
        val jsonArray = JSONArray()
        for (shard in shards) {
            jsonArray.put(JSONObject().apply {
                put("si", shard.shardIndex)
                put("rc", shard.rowCount)
                put("mt", shard.maxTimestampMillis)
            })
        }
        setMetadata(key, jsonArray.toString())
    }

    fun getShardSyncSnapshot(gaiaId: String, databaseId: Long): Map<Long, ShardState> {
        val key = "shard_sync_${gaiaId}_${databaseId}"
        val json = getMetadata(key) ?: return emptyMap()
        return runCatching {
            val array = JSONArray(json)
            val result = mutableMapOf<Long, ShardState>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val shardIndex = obj.getLong("si")
                result[shardIndex] = ShardState(
                    shardIndex = shardIndex,
                    rowCount = obj.getInt("rc"),
                    maxTimestampMillis = obj.getLong("mt")
                )
            }
            result
        }.getOrElse {
            Log.e(TAG, "Failed to parse shard sync snapshot", it)
            emptyMap()
        }
    }

    fun queryRawSegmentRows(gaiaId: String, databaseId: Long): List<RawSegmentRow> {
        if (gaiaId.isEmpty()) return emptyList()
        return runCatching {
            val columns = arrayOf(
                COLUMN_ID, COLUMN_TIMESTAMP_MILLIS, COLUMN_DATABASE_ID, COLUMN_ORIGIN_ID,
                COLUMN_SEGMENT_TYPE, COLUMN_SEMANTIC_SEGMENT, COLUMN_START_TIMESTAMP,
                COLUMN_END_TIMESTAMP, COLUMN_HIERARCHY_LEVEL, COLUMN_SHOWN_IN_TIMELINE,
                COLUMN_SEGMENT_ID, COLUMN_IS_FINALIZED
            )
            openDatabase().querySafe(
                TABLE_SEMANTIC_SEGMENT,
                columns,
                "$COLUMN_OBFUSCATED_GAIA_ID=? AND $COLUMN_DATABASE_ID=?",
                arrayOf(gaiaId, databaseId.toString()),
                COLUMN_ID
            ) { cursor ->
                val results = mutableListOf<RawSegmentRow>()
                while (cursor.moveToNext()) {
                    runCatching {
                        RawSegmentRow(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            timestampMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP_MILLIS)),
                            databaseId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATABASE_ID)),
                            originId = cursor.getLongOrNull(COLUMN_ORIGIN_ID),
                            segmentType = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SEGMENT_TYPE)),
                            semanticSegment = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_SEMANTIC_SEGMENT)),
                            startTimestampSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START_TIMESTAMP)),
                            endTimestampSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_END_TIMESTAMP)),
                            hierarchyLevel = cursor.getIntOrNull(COLUMN_HIERARCHY_LEVEL),
                            shownInTimeline = (cursor.getIntOrNull(COLUMN_SHOWN_IN_TIMELINE) ?: 1) == 1,
                            segmentId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SEGMENT_ID)),
                            isFinalized = (cursor.getIntOrNull(COLUMN_IS_FINALIZED) ?: 0) == 1
                        )
                    }.onSuccess { results.add(it) }
                        .onFailure { Log.w(TAG, "Failed to parse raw segment row", it) }
                }
                results
            }
        }.getOrElse {
            Log.e(TAG, "Failed to query raw segment rows", it)
            emptyList()
        }
    }

    data class Tombstone(
        val createdMillis: Long,
        val startTimeSec: Long,
        val endTimeSec: Long
    )

    private fun tombstoneMetadataKey(gaiaId: String) = "tombstones_$gaiaId"

    fun getTombstones(gaiaId: String): List<Tombstone> {
        val json = getMetadata(tombstoneMetadataKey(gaiaId)) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Tombstone(
                    createdMillis = obj.getLong("created"),
                    startTimeSec = obj.getLong("start"),
                    endTimeSec = obj.getLong("end")
                )
            }
        }.getOrElse {
            Log.e(TAG, "Failed to parse tombstones", it)
            emptyList()
        }
    }

    fun addTombstone(gaiaId: String, tombstone: Tombstone) {
        val existing = getTombstones(gaiaId)
        val updated = existing + tombstone
        saveTombstones(gaiaId, updated)
    }

    fun removeTombstonesOverlapping(gaiaId: String, startSec: Long, endSec: Long) {
        val existing = getTombstones(gaiaId)
        if (existing.isEmpty()) return
        val remaining = existing.filter { ts ->
            ts.endTimeSec <= startSec || ts.startTimeSec >= endSec
        }
        val removed = existing.size - remaining.size
        if (removed > 0) {
            saveTombstones(gaiaId, remaining)
            Log.d(TAG, "Removed $removed overlapping tombstones in [$startSec, $endSec), kept ${remaining.size}")
        }
    }

    fun purgeTombstones(gaiaId: String, ttlDays: Int = 90) {
        val cutoff = System.currentTimeMillis() - ttlDays * 86400_000L
        val existing = getTombstones(gaiaId)
        val remaining = existing.filter { it.createdMillis > cutoff }
        if (remaining.size < existing.size) {
            saveTombstones(gaiaId, remaining)
            Log.d(TAG, "Purged ${existing.size - remaining.size} expired tombstones")
        }
    }

    private fun saveTombstones(gaiaId: String, tombstones: List<Tombstone>) {
        val array = JSONArray()
        tombstones.forEach { ts ->
            array.put(JSONObject().apply {
                put("created", ts.createdMillis)
                put("start", ts.startTimeSec)
                put("end", ts.endTimeSec)
            })
        }
        setMetadata(tombstoneMetadataKey(gaiaId), array.toString())
    }

    private fun Cursor.getLongOrNull(columnName: String): Long? =
        getColumnIndex(columnName).takeIf { it >= 0 && !isNull(it) }?.let { getLong(it) }

    private fun Cursor.getIntOrNull(columnName: String): Int? =
        getColumnIndex(columnName).takeIf { it >= 0 && !isNull(it) }?.let { getInt(it) }

    private class OdlhDatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            Log.i(TAG, "Creating ODLH database")
            createGellerMetadataTable(db)
            createGellerSyncStatusTable(db)
            createSemanticSegmentTable(db)
            createEditedSegmentTable(db)
        }

        private fun createGellerMetadataTable(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_GELLER_METADATA (
                    $COLUMN_KEY TEXT PRIMARY KEY,
                    $COLUMN_VALUE TEXT
                )
            """.trimIndent()
            )
        }

        private fun createGellerSyncStatusTable(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_GELLER_SYNC_STATUS (
                    $COLUMN_DATA_TYPE INTEGER PRIMARY KEY,
                    $COLUMN_SYNC_TOKEN TEXT,
                    $COLUMN_LAST_SYNC_TIME INTEGER
                )
            """.trimIndent()
            )
        }

        private fun createSemanticSegmentTable(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_SEMANTIC_SEGMENT (
                    $COLUMN_ID INTEGER PRIMARY KEY,
                    $COLUMN_OBFUSCATED_GAIA_ID TEXT NOT NULL,
                    $COLUMN_SEGMENT_ID TEXT NOT NULL UNIQUE,
                    $COLUMN_SEGMENT_TYPE INTEGER NOT NULL,
                    $COLUMN_SEMANTIC_SEGMENT BLOB NOT NULL,
                    $COLUMN_START_TIMESTAMP INTEGER NOT NULL,
                    $COLUMN_END_TIMESTAMP INTEGER NOT NULL,
                    $COLUMN_HIERARCHY_LEVEL INTEGER,
                    $COLUMN_FPRINT INTEGER,
                    $COLUMN_SHOWN_IN_TIMELINE INTEGER DEFAULT 1,
                    $COLUMN_TIMESTAMP_MILLIS INTEGER NOT NULL DEFAULT 0,
                    $COLUMN_DATABASE_ID INTEGER NOT NULL,
                    $COLUMN_ORIGIN_ID INTEGER,
                    $COLUMN_IS_FINALIZED INTEGER
                )
            """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_semantic_gaia_time
                ON $TABLE_SEMANTIC_SEGMENT ($COLUMN_OBFUSCATED_GAIA_ID, $COLUMN_END_TIMESTAMP, $COLUMN_START_TIMESTAMP)
            """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_semantic_gaia_type
                ON $TABLE_SEMANTIC_SEGMENT ($COLUMN_OBFUSCATED_GAIA_ID, $COLUMN_SEGMENT_TYPE, $COLUMN_END_TIMESTAMP)
            """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_semantic_database_origin
                ON $TABLE_SEMANTIC_SEGMENT ($COLUMN_OBFUSCATED_GAIA_ID, $COLUMN_DATABASE_ID)
            """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_semantic_fprint
                ON $TABLE_SEMANTIC_SEGMENT ($COLUMN_OBFUSCATED_GAIA_ID, $COLUMN_FPRINT)
            """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS tr_semantic_insert
                AFTER INSERT ON $TABLE_SEMANTIC_SEGMENT
                BEGIN
                    UPDATE $TABLE_SEMANTIC_SEGMENT
                    SET $COLUMN_ORIGIN_ID = $COLUMN_ID,
                        $COLUMN_TIMESTAMP_MILLIS = CAST(ROUND((julianday('now') - 2440587.5)*86400000) as integer)
                    WHERE $COLUMN_ID = NEW.$COLUMN_ID AND $COLUMN_TIMESTAMP_MILLIS = 0;
                END
            """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS tr_semantic_update
                AFTER UPDATE ON $TABLE_SEMANTIC_SEGMENT
                BEGIN
                    UPDATE $TABLE_SEMANTIC_SEGMENT
                    SET $COLUMN_TIMESTAMP_MILLIS = CAST(ROUND((julianday('now') - 2440587.5)*86400000) as integer)
                    WHERE $COLUMN_ID = NEW.$COLUMN_ID;
                END
            """.trimIndent()
            )
        }

        private fun createEditedSegmentTable(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_EDITED_SEGMENT (
                    $COLUMN_ID INTEGER PRIMARY KEY,
                    $COLUMN_OBFUSCATED_GAIA_ID TEXT NOT NULL,
                    $COLUMN_SEGMENT_ID TEXT NOT NULL UNIQUE,
                    $COLUMN_SEGMENT_TYPE INTEGER NOT NULL,
                    $COLUMN_SEMANTIC_SEGMENT BLOB NOT NULL,
                    $COLUMN_START_TIMESTAMP INTEGER NOT NULL,
                    $COLUMN_END_TIMESTAMP INTEGER NOT NULL,
                    $COLUMN_BLOCK_START_TIMESTAMP INTEGER NOT NULL,
                    $COLUMN_BLOCK_END_TIMESTAMP INTEGER NOT NULL,
                    $COLUMN_HIERARCHY_LEVEL INTEGER,
                    $COLUMN_IS_EDIT_UPLOADED INTEGER
                )
            """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_edited_gaia_block_time
                ON $TABLE_EDITED_SEGMENT ($COLUMN_OBFUSCATED_GAIA_ID, $COLUMN_BLOCK_END_TIMESTAMP, $COLUMN_BLOCK_START_TIMESTAMP)
            """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_edited_gaia_purge
                ON $TABLE_EDITED_SEGMENT ($COLUMN_OBFUSCATED_GAIA_ID, $COLUMN_START_TIMESTAMP, $COLUMN_SEGMENT_TYPE)
            """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.i(TAG, "Upgrading ODLH database from $oldVersion to $newVersion")
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.i(TAG, "Downgrading ODLH database from $oldVersion to $newVersion")
        }
    }
}

data class ShardState(
    val shardIndex: Long,
    val rowCount: Int,
    val maxTimestampMillis: Long
)

data class RawSegmentRow(
    val id: Long,
    val timestampMillis: Long,
    val databaseId: Long,
    val originId: Long?,
    val segmentType: Int,
    val semanticSegment: ByteArray,
    val startTimestampSeconds: Long,
    val endTimestampSeconds: Long,
    val hierarchyLevel: Int?,
    val shownInTimeline: Boolean,
    val segmentId: String,
    val isFinalized: Boolean
)

data class SemanticSegment(
    val id: Long? = null,
    val segmentId: String,
    val segmentType: Int,
    val data: ByteArray,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val hierarchyLevel: Int = 0,
    val fprint: Long? = null,
    val shownInTimeline: Boolean = true,
    val timestampMillis: Long = 0L,
    val databaseId: Long = 0L,
    val originId: Long? = null,
    val isFinalized: Boolean = false,
    val blockStartTimestamp: Long? = null,
    val blockEndTimestamp: Long? = null,
    val isEditUploaded: Boolean = false
) {
    val isSubVisit: Boolean
        get() = segmentType == SEGMENT_TYPE_VISIT && hierarchyLevel != 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SemanticSegment) return false
        return id == other.id && segmentId == other.segmentId
    }

    override fun hashCode(): Int = 31 * (id?.hashCode() ?: 0) + segmentId.hashCode()
}
