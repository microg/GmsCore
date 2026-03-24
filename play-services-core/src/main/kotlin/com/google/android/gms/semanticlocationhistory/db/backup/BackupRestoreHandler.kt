/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.db.backup

import android.content.Context
import android.util.Log
import com.google.android.gms.geller.ElementTimestamp
import com.google.android.gms.geller.GellerAny
import com.google.android.gms.geller.GellerDataType
import com.google.android.gms.geller.GellerE2eeElement
import com.google.android.gms.geller.GellerElement
import com.google.android.gms.geller.externaldb.ExternalDbSnapshot
import com.google.android.gms.geller.externaldb.ExternalDbSync
import com.google.android.gms.semanticlocationhistory.E2EE_TYPE_URL
import com.google.android.gms.semanticlocationhistory.OdlhBackupSummary
import com.google.android.gms.semanticlocationhistory.TAG
import com.google.android.gms.semanticlocationhistory.api.GellerSyncClient
import com.google.android.gms.semanticlocationhistory.db.OdlhStorageManager

object BackupRestoreHandler {

    @Volatile
    private var cachedAllMutations: List<GellerElement>? = null

    suspend fun fetchBackupSummaries(
        context: Context,
        accountName: String,
        storageManager: OdlhStorageManager
    ): List<OdlhBackupSummary> {
        val response = GellerSyncClient.queryOdlh(
            context = context,
            accountName = accountName,
            syncToken = null
        )
        if (response == null) {
            Log.w(TAG, "fetchBackupSummaries: queryOdlh returned null")
            return emptyList()
        }

        val localDatabaseId = storageManager.getDatabaseId()
        val allMutations = response.items
            .filter { it.dataType == GellerDataType.ENCRYPTED_ONDEVICE_LOCATION_HISTORY }
            .mapNotNull { it.syncResult }
            .flatMap { it.mutations }

        cachedAllMutations = allMutations

        if (allMutations.isEmpty()) {
            Log.d(TAG, "fetchBackupSummaries: no mutations in response, returning empty list")
            return emptyList()
        }

        Log.d(TAG, "fetchBackupSummaries_allMutations: $allMutations")

        val deviceMap = mutableMapOf<Long, BackupSnapshotData>()
        var decryptedCount = 0
        var fallbackCount = 0

        for (entry in allMutations) {
            val key = entry.elementId ?: continue
            val databaseIdFromKey = parseDatabaseIdFromKey(key) ?: continue

            val parsed = tryParseSnapshot(context, accountName, entry)
            if (parsed != null) decryptedCount++ else fallbackCount++
            val snapshot = parsed?.first
            val serializedSize = parsed?.second ?: (entry.payload?.value_?.size ?: 0)

            val databaseId = snapshot?.databaseId ?: databaseIdFromKey
            val entryTimestamp = entry.timestamp?.timestampMicros ?: 0L

            val existing = deviceMap[databaseId]
            if (existing == null) {
                deviceMap[databaseId] = createBackupSnapshotData(snapshot, entryTimestamp, serializedSize, key)
            } else {
                mergeBackupSnapshotData(existing, snapshot, entryTimestamp, serializedSize, key)
            }
        }

        if (deviceMap.isEmpty()) {
            Log.d(TAG, "fetchBackupSummaries: no valid backup data found")
            return emptyList()
        }

        Log.d(TAG, "fetchBackupSummaries: found ${deviceMap.size} devices (decrypted=$decryptedCount, fallback=$fallbackCount), localDatabaseId=$localDatabaseId")

        return deviceMap.map { (databaseId, info) ->
            Log.d(TAG, "fetchBackupSummaries_info: $info")
            OdlhBackupSummary(
                databaseId,
                info.deviceModel,
                databaseId == localDatabaseId,
                info.latestTimestamp,
                info.keys.toList(),
                info.deviceName,
                info.serializedSize.takeIf { it > 0 },
                info.rowCount.takeIf { it > 0 },
                info.earliestTimestamp.takeIf { it != Long.MAX_VALUE }
            )
        }
    }

    private fun parseDatabaseIdFromKey(key: String): Long? {
        val parts = key.split(";")
        if (parts.size != 4) return null
        return parts[0].toLongOrNull()
    }

    private fun createBackupSnapshotData(
        snapshot: ExternalDbSnapshot?,
        entryTimestamp: Long,
        serializedSize: Int,
        key: String
    ): BackupSnapshotData {
        return BackupSnapshotData(
            deviceModel = snapshot?.deviceModel ?: "",
            deviceName = snapshot?.deviceIdentifier ?: "",
            latestTimestamp = snapshot?.timestamp?.timestampMicros ?: entryTimestamp,
            rowCount = snapshot?.rows?.size ?: 0,
            serializedSize = serializedSize,
            earliestTimestamp = snapshot?.let { computeEarliestTimestamp(it) } ?: Long.MAX_VALUE,
            keys = mutableSetOf(key)
        )
    }

    private fun mergeBackupSnapshotData(
        existing: BackupSnapshotData,
        snapshot: ExternalDbSnapshot?,
        entryTimestamp: Long,
        serializedSize: Int,
        key: String
    ) {
        val newTs = snapshot?.timestamp?.timestampMicros ?: entryTimestamp
        if (newTs > existing.latestTimestamp) {
            snapshot?.deviceModel?.takeIf { it.isNotEmpty() }?.let { existing.deviceModel = it }
            snapshot?.deviceIdentifier?.takeIf { it.isNotEmpty() }?.let { existing.deviceName = it }
            existing.latestTimestamp = newTs
        }
        existing.rowCount += snapshot?.rows?.size ?: 0
        existing.serializedSize += serializedSize
        val newEarliest = snapshot?.let { computeEarliestTimestamp(it) } ?: Long.MAX_VALUE
        if (newEarliest < existing.earliestTimestamp) {
            existing.earliestTimestamp = newEarliest
        }
        existing.keys.add(key)
    }

    private fun tryParseSnapshot(context: Context, accountName: String, entry: GellerElement): Pair<ExternalDbSnapshot, Int>? {
        val typedValue = entry.payload ?: return null
        val typeUrl = typedValue.typeUrl
        val valueBytes = typedValue.value_.toByteArray()

        return try {
            val externalDbSyncBytes: ByteArray

            if (typeUrl == E2EE_TYPE_URL) {
                val e2eeElement = GellerE2eeElement.ADAPTER.decode(bytes = valueBytes)
                val decryptedBytes = OdlhSyncProcessor.decryptE2eeElement(context, accountName, e2eeElement)
                    ?: return null

                val innerTypedValue = GellerAny.ADAPTER.decode(decryptedBytes)
                if (!innerTypedValue.typeUrl.contains("ExternalDbSync")) return null
                externalDbSyncBytes = innerTypedValue.value_.toByteArray()
            } else if (typeUrl.contains("ExternalDbSync")) {
                externalDbSyncBytes = valueBytes
            } else {
                return null
            }

            val dbSync = ExternalDbSync.ADAPTER.decode(externalDbSyncBytes)
            val snapshot = dbSync.snapshot ?: return null
            Pair(snapshot, externalDbSyncBytes.size)
        } catch (e: Exception) {
            Log.w(TAG, "tryParseSnapshot: failed for key=${entry.elementId}: ${e.message}")
            null
        }
    }

    private fun computeEarliestTimestamp(tableSync: ExternalDbSnapshot): Long {
        val colIndex = tableSync.columnNames.indexOf("start_timestamp_seconds")
        if (colIndex < 0) return Long.MAX_VALUE

        var earliest = Long.MAX_VALUE
        for (row in tableSync.rows) {
            if (colIndex >= row.values.size) continue
            val ts = row.values[colIndex].intValue ?: continue
            if (ts > 0 && ts < earliest) {
                earliest = ts
            }
        }
        return earliest
    }

    data class RestoreResult(
        val restoredCount: Int,
        val minStartSec: Long = Long.MAX_VALUE,
        val maxEndSec: Long = Long.MIN_VALUE
    ) {
        val hasData get() = restoredCount > 0 && minStartSec != Long.MAX_VALUE
    }

    fun restoreBackups(
        context: Context,
        accountName: String,
        gaiaId: String,
        databaseIds: List<Long>,
        storageManager: OdlhStorageManager
    ): RestoreResult {
        val mutations = cachedAllMutations
        if (mutations.isNullOrEmpty()) {
            Log.w(TAG, "restoreBackups: no cached mutations available")
            return RestoreResult(0)
        }

        val localDatabaseId = storageManager.getDatabaseId()
        val databaseIdSet = databaseIds.toSet()

        var restoredCount = 0
        var failedCount = 0
        var minStart = Long.MAX_VALUE
        var maxEnd = Long.MIN_VALUE

        for (entry in mutations) {
            val key = entry.elementId ?: continue
            val entryDatabaseId = parseDatabaseIdFromKey(key) ?: continue

            if (entryDatabaseId !in databaseIdSet) continue

            try {
                val segments = OdlhSyncProcessor.processMutation(context, accountName, entry, localDatabaseId)
                for (segment in segments) {
                    Log.d(
                        TAG, "restoreBackups: segment segmentId=${segment.segmentId}, type=${segment.segmentType}, " +
                                "start=${segment.startTimestamp}, end=${segment.endTimestamp}, " +
                                "hierarchy=${segment.hierarchyLevel}, dataSize=${segment.data.size}"
                    )
                    val rowId = storageManager.insertOrUpdateSegment(gaiaId, segment)
                    if (rowId >= 0) {
                        restoredCount++
                        minStart = minOf(minStart, segment.startTimestamp)
                        maxEnd = maxOf(maxEnd, segment.endTimestamp)
                    } else {
                        failedCount++
                        Log.w(TAG, "restoreBackups: insertOrUpdateSegment returned $rowId for segmentId=${segment.segmentId}")
                    }
                }
            } catch (e: Exception) {
                failedCount++
                Log.e(TAG, "restoreBackups: failed to process entry key=$key", e)
            }
        }

        if (restoredCount > 0 && minStart != Long.MAX_VALUE) {
            val clearedEdits = storageManager.deleteEditedSegmentsByTimeRange(gaiaId, minStart, maxEnd)
            if (clearedEdits > 0) {
                Log.d(TAG, "restoreBackups: cleared $clearedEdits stale edits in restored time range [$minStart, $maxEnd]")
            }
        }

        cachedAllMutations = null

        Log.d(
            TAG, "restoreBackups: restored=$restoredCount, failed=$failedCount, " +
                    "timeRange=[$minStart, $maxEnd] for databaseIds=$databaseIds"
        )
        return RestoreResult(restoredCount, minStart, maxEnd)
    }

    suspend fun deleteBackups(
        context: Context,
        accountName: String,
        databaseIds: List<Long>,
        storageManager: OdlhStorageManager
    ) {
        val response = GellerSyncClient.queryOdlh(
            context = context,
            accountName = accountName,
            syncToken = null
        )
        if (response == null) {
            Log.w(TAG, "deleteBackups: queryOdlh returned null")
            return
        }

        val databaseIdSet = databaseIds.toSet()
        var syncToken: String? = null
        val latestEntryByKey = mutableMapOf<String, GellerElement>()

        for (item in response.items) {
            if (item.dataType != GellerDataType.ENCRYPTED_ONDEVICE_LOCATION_HISTORY) continue
            val syncResult = item.syncResult ?: continue

            if (!syncResult.syncToken.isNullOrEmpty()) {
                syncToken = syncResult.syncToken
            }

            val entries = syncResult.mutations + syncResult.results
            for (entry in entries) {
                val key = entry.elementId ?: continue
                val databaseId = parseDatabaseIdFromKey(key) ?: continue
                if (databaseId !in databaseIdSet) continue

                val ts = entry.timestamp?.timestampMicros ?: 0L
                val existing = latestEntryByKey[key]
                if (existing == null || ts > (existing.timestamp?.timestampMicros ?: 0L)) {
                    latestEntryByKey[key] = GellerElement(elementId = key, timestamp = ElementTimestamp(timestampMicros = ts))
                }
            }
        }

        Log.d(TAG, "deleteBackups: found ${latestEntryByKey.size} unique keys to delete for databaseIds=$databaseIds, syncToken=${syncToken?.take(16)}...")

        if (latestEntryByKey.isEmpty()) return

        val deleteResponse = GellerSyncClient.deleteOdlh(
            context = context,
            accountName = accountName,
            syncToken = syncToken,
            deletions = latestEntryByKey.values.toList()
        )

        Log.d(TAG, "deleteBackups: deleteOdlh response=$deleteResponse")

        if (storageManager.getDatabaseId() in databaseIdSet) {
            storageManager.clearSyncToken(GellerDataType.ENCRYPTED_ONDEVICE_LOCATION_HISTORY.value)
            Log.d(TAG, "deleteBackups: cleared local syncToken for this device")
        }
    }

    private data class BackupSnapshotData(
        var deviceModel: String = "",
        var deviceName: String = "",
        var latestTimestamp: Long = 0L,
        var rowCount: Int = 0,
        var serializedSize: Int = 0,
        var earliestTimestamp: Long = Long.MAX_VALUE,
        val keys: MutableSet<String> = mutableSetOf()
    )
}