/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.db.backup

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.geller.ElementTimestamp
import com.google.android.gms.geller.GellerAny
import com.google.android.gms.geller.GellerE2eeElement
import com.google.android.gms.geller.GellerElement
import com.google.android.gms.geller.externaldb.ExternalDbSnapshot
import com.google.android.gms.geller.externaldb.ExternalDbSync
import com.google.android.gms.geller.externaldb.SnapshotRow
import com.google.android.gms.geller.externaldb.SnapshotValue
import com.google.android.gms.geller.externaldb.TimestampMicros
import com.google.android.gms.semanticlocationhistory.AES_GCM_IV_SIZE
import com.google.android.gms.semanticlocationhistory.AES_GCM_TAG_BITS
import com.google.android.gms.semanticlocationhistory.E2EE_TYPE_URL
import com.google.android.gms.semanticlocationhistory.api.GellerSyncClient
import com.google.android.gms.semanticlocationhistory.db.OdlhStorageManager
import com.google.android.gms.semanticlocationhistory.db.RawSegmentRow
import com.google.android.gms.semanticlocationhistory.getObfuscatedGaiaId
import com.google.android.gms.semanticlocationhistory.loadKeyMaterials
import okio.ByteString.Companion.toByteString
import org.microg.gms.common.Constants
import org.microg.gms.feature.GoogleFeaturePreferences
import java.security.SecureRandom
import java.util.TimeZone
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val EXTERNAL_DB_SYNC_TYPE_URL = "type.googleapis.com/geller.externaldb.ExternalDbSync"
private const val BACKUP_SYNC_TOKEN_KEY = "backup_sync_token"
private const val SHARD_SHIFT = 10
private const val SHARD_SIZE = 1 shl SHARD_SHIFT  // 1024
private const val MAX_BATCH_SIZE = 3_670_016
private val BACKUP_COLUMNS = listOf(
    "_id", "timestamp_millis", "database_id", "origin_id", "segment_id",
    "semantic_segment", "obfuscated_gaia_id", "shown_in_timeline", "is_finalized",
    "start_timestamp_seconds", "end_timestamp_seconds", "segment_type", "hierarchy_level"
)

data class BackupResult(
    val success: Boolean,
    val message: String? = null,
    val uploadedShards: Int = 0
)

private const val TAG = "OdlhBackupProcessor"

object OdlhBackupProcessor {

    /**
     * Performs a full backup of all location history data.
     */
    suspend fun performBackup(context: Context, accountName: String): BackupResult {
        val allowedUpload = GoogleFeaturePreferences.allowedMapsTimelineUpload(context)
        if (!allowedUpload) {
            Log.w(TAG, "<performBackup> user not allowed report")
            return BackupResult(false, "Upload Not allowed!")
        }
        Log.d(TAG, "performBackup: starting for account=$accountName")
        val gaiaId = context.getObfuscatedGaiaId(accountName)
        val storageManager = OdlhStorageManager.getInstance(context)
        val databaseId = storageManager.getDatabaseId()
        Log.d(TAG, "performBackup: gaiaId=${gaiaId.take(8)}..., databaseId=$databaseId")

        val flushed = storageManager.flushEditsToMainTable(gaiaId)
        Log.d(TAG, "performBackup: flushed $flushed edit blocks before backup")

        val keyMaterials = loadKeyMaterials(context, accountName)
        if (keyMaterials.isEmpty()) {
            Log.e(TAG, "performIncrementalBackup: no encryption keys found")
            return BackupResult(false, "No encryption keys available")
        }
        val (keyMaterial, keyVersion) = keyMaterials.first()

        val shards = readAndShardData(storageManager, gaiaId, databaseId)
        if (shards.isEmpty()) {
            Log.d(TAG, "performBackup: no data to backup")
            return BackupResult(true, "No data to backup", 0)
        }

        Log.d(TAG, "performBackup: ${shards.size} shards to upload")

        val allEntries = buildShardEntries(context, accountName, gaiaId, databaseId, shards, keyMaterial, keyVersion)

        val backupSyncToken = storageManager.getMetadata(BACKUP_SYNC_TOKEN_KEY)
        var totalUploaded = 0
        var lastSyncToken = backupSyncToken

        val batches = splitIntoBatches(allEntries)
        Log.d(TAG, "performBackup: ${batches.size} batch(es) to upload")

        for (batch in batches) {
            val response = GellerSyncClient.uploadOdlh(
                context = context,
                accountName = accountName,
                syncToken = lastSyncToken,
                mutations = batch
            )

            if (response == null) {
                Log.e(TAG, "performBackup: upload failed for batch")
                return BackupResult(false, "Upload failed", totalUploaded)
            }

            totalUploaded += batch.size

            for (item in response.items) {
                item.syncResult?.syncToken?.let { token ->
                    lastSyncToken = token
                    storageManager.setMetadata(BACKUP_SYNC_TOKEN_KEY, token)
                    Log.d(TAG, "performBackup: saved new backup syncToken")
                }
            }
        }

        Log.d(TAG, "performBackup: completed, uploaded $totalUploaded shards")
        return BackupResult(true, uploadedShards = totalUploaded)
    }

    private fun buildShardEntries(
        context: Context,
        accountName: String,
        gaiaId: String,
        databaseId: Long,
        shards: Map<Long, List<RawSegmentRow>>,
        keyMaterial: ByteArray,
        keyVersion: Int
    ): List<GellerElement> {
        return shards.map { (shardIndex, rows) ->
            buildShardDataEntry(context, accountName, gaiaId, databaseId, shardIndex, rows, keyMaterial, keyVersion)
        }
    }

    /**
     * Incremental sync backup - follows GMS three-step delta comparison.
     *
     * Flow:
     * 1. Read previous sync snapshot (getShardSyncSnapshot)
     * 2. Read current SQLite shard states (queryShardStates: shardIndex, rowCount, maxTimestampMillis)
     * 3. Delta comparison: rowCount or maxTimestampMillis changed -> modified shard; previously existed but now missing -> deleted shard
     * 4. Upload changed shards, delete removed shards
     * 5. Update shard sync snapshot + sync_token
     */
    suspend fun performIncrementalBackup(context: Context, accountName: String): BackupResult {
        Log.d(TAG, "performIncrementalBackup: starting for account=$accountName")
        val allowedUpload = GoogleFeaturePreferences.allowedMapsTimelineUpload(context)
        if (!allowedUpload) {
            Log.w(TAG, "<performIncrementalBackup> user not allowed report")
            return BackupResult(false, "Upload Not allowed!")
        }

        val gaiaId = context.getObfuscatedGaiaId(accountName)
        val storageManager = OdlhStorageManager.getInstance(context)
        val databaseId = storageManager.getDatabaseId()
        Log.d(TAG, "performIncrementalBackup: gaiaId=${gaiaId.take(8)}..., databaseId=$databaseId")

        val flushed = storageManager.flushEditsToMainTable(gaiaId)
        Log.d(TAG, "performIncrementalBackup: flushed $flushed edit blocks before backup")

        val keyMaterials = loadKeyMaterials(context, accountName)
        if (keyMaterials.isEmpty()) {
            Log.e(TAG, "performIncrementalBackup: no encryption keys found")
            return BackupResult(false, "No encryption keys available")
        }
        val (keyMaterial, keyVersion) = keyMaterials.first()

        // Step 1: Read previous sync snapshot
        val previousShards = storageManager.getShardSyncSnapshot(gaiaId, databaseId).toMutableMap()
        Log.d(TAG, "performIncrementalBackup: ${previousShards.size} previously synced shards")

        // Step 2: Read current SQLite shard states
        val currentShards = storageManager.queryShardStates(gaiaId, databaseId)
        Log.d(TAG, "performIncrementalBackup: ${currentShards.size} current shards")

        // Step 3: Delta comparison
        val changedShardIndices = mutableListOf<Long>()
        for ((shardIndex, currentState) in currentShards) {
            val prevState = previousShards.remove(shardIndex)
            if (prevState == null ||
                prevState.rowCount != currentState.rowCount ||
                prevState.maxTimestampMillis != currentState.maxTimestampMillis
            ) {
                changedShardIndices.add(shardIndex)
            }
        }
        // Remaining entries in previousShards = deleted shards
        val deletedShardIndices = previousShards.keys.toList()

        Log.d(TAG, "performIncrementalBackup: ${changedShardIndices.size} changed, ${deletedShardIndices.size} deleted")

        if (changedShardIndices.isEmpty() && deletedShardIndices.isEmpty()) {
            Log.d(TAG, "performIncrementalBackup: no changes detected")
            return BackupResult(true, "No changes to sync", 0)
        }

        val backupSyncToken = storageManager.getMetadata(BACKUP_SYNC_TOKEN_KEY)
        var lastSyncToken = backupSyncToken
        var totalUploaded = 0

        // Upload changed shards
        if (changedShardIndices.isNotEmpty()) {
            val entries = buildChangedShardEntries(
                storageManager, changedShardIndices, gaiaId, databaseId,
                context, accountName, keyMaterial, keyVersion
            )

            if (entries.isNotEmpty()) {
                val batches = splitIntoBatches(entries)
                Log.d(TAG, "performIncrementalBackup: uploading ${entries.size} changed shards in ${batches.size} batch(es)")

                for (batch in batches) {
                    val response = GellerSyncClient.uploadOdlh(
                        context = context,
                        accountName = accountName,
                        syncToken = lastSyncToken,
                        mutations = batch
                    )

                    if (response == null) {
                        Log.e(TAG, "performIncrementalBackup: upload failed")
                        return BackupResult(false, "Upload failed", totalUploaded)
                    }

                    totalUploaded += batch.size

                    for (item in response.items) {
                        item.syncResult?.syncToken?.let { token ->
                            lastSyncToken = token
                        }
                    }
                }
            }
        }

        // Delete removed shards
        if (deletedShardIndices.isNotEmpty()) {
            val deletions = buildShardDeletions(databaseId, deletedShardIndices)
            Log.d(TAG, "performIncrementalBackup: deleting ${deletions.size} removed shards")

            val response = GellerSyncClient.deleteOdlh(
                context = context,
                accountName = accountName,
                syncToken = lastSyncToken,
                deletions = deletions
            )

            if (response != null) {
                for (item in response.items) {
                    item.syncResult?.syncToken?.let { token ->
                        lastSyncToken = token
                    }
                }
            }
        }

        // Save sync_token for upload direction (independent from download sync_token)
        val finalSyncToken = lastSyncToken
        if (finalSyncToken != null && finalSyncToken != backupSyncToken) {
            storageManager.setMetadata(BACKUP_SYNC_TOKEN_KEY, finalSyncToken)
            Log.d(TAG, "performIncrementalBackup: saved new backup syncToken")
        }

        // Save updated shard sync snapshot
        storageManager.saveShardSyncSnapshot(gaiaId, databaseId, currentShards.values)

        Log.d(TAG, "performIncrementalBackup: completed, $totalUploaded uploaded + ${deletedShardIndices.size} deleted")
        return BackupResult(true, uploadedShards = totalUploaded)
    }

    private fun buildChangedShardEntries(
        storageManager: OdlhStorageManager,
        changedShardIndices: List<Long>,
        gaiaId: String,
        databaseId: Long,
        context: Context,
        accountName: String,
        keyMaterial: ByteArray,
        keyVersion: Int
    ): List<GellerElement> {
        return changedShardIndices.mapNotNull { shardIndex ->
            val minId = shardIndex * SHARD_SIZE
            val maxId = minId + SHARD_SIZE - 1
            val rows = storageManager.queryRawSegmentRowsForShard(gaiaId, databaseId, minId, maxId)
            if (rows.isNotEmpty()) {
                buildShardDataEntry(context, accountName, gaiaId, databaseId, shardIndex, rows, keyMaterial, keyVersion)
            } else {
                null
            }
        }
    }

    private fun buildShardDeletions(databaseId: Long, deletedShardIndices: List<Long>): List<GellerElement> {
        return deletedShardIndices.map { shardIndex ->
            val minId = shardIndex * SHARD_SIZE
            val maxId = minId + SHARD_SIZE - 1
            val gellerKey = "$databaseId;semantic_segment_table;$minId;$maxId"
            GellerElement(elementId = gellerKey)
        }
    }

    private fun readAndShardData(storageManager: OdlhStorageManager, gaiaId: String, databaseId: Long): Map<Long, List<RawSegmentRow>> {
        val rows = storageManager.queryRawSegmentRows(gaiaId, databaseId)
        Log.d(TAG, "readAndShardData: ${rows.size} rows total")
        return rows.groupBy { it.id shr SHARD_SHIFT }
    }

    private fun buildSnapshotRow(row: RawSegmentRow, gaiaId: String): SnapshotRow {
        return SnapshotRow(
            values = listOf(
                SnapshotValue(intValue = row.id),
                SnapshotValue(intValue = row.timestampMillis),
                SnapshotValue(intValue = row.databaseId),
                if (row.originId != null) SnapshotValue(intValue = row.originId) else SnapshotValue(),
                SnapshotValue(stringValue = row.segmentId),
                SnapshotValue(bytesValue = row.semanticSegment.toByteString()),
                SnapshotValue(stringValue = gaiaId),
                SnapshotValue(boolValue = row.shownInTimeline),
                SnapshotValue(boolValue = row.isFinalized),
                SnapshotValue(intValue = row.startTimestampSeconds),
                SnapshotValue(intValue = row.endTimestampSeconds),
                SnapshotValue(intValue = row.segmentType.toLong()),
                if (row.hierarchyLevel != null) SnapshotValue(boolValue = row.hierarchyLevel > 0) else SnapshotValue()
            )
        )
    }

    private fun buildExternalDbSync(
        context: Context,
        accountName: String,
        gaiaId: String,
        databaseId: Long,
        rows: List<RawSegmentRow>
    ): ExternalDbSync {
        val minId = rows.minOf { it.id }
        val maxId = rows.maxOf { it.id }
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val timezoneOffsetMinutes = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000

        val protoRows = rows.map { buildSnapshotRow(it, gaiaId) }

        val deviceTag = OdlhStorageManager.getInstance(context).getDeviceTag(accountName)
        val tableSync = ExternalDbSnapshot(
            tableName = "semantic_segment_table",
            startId = minId,
            endId = maxId,
            columnNames = BACKUP_COLUMNS,
            rows = protoRows,
            databaseId = databaseId,
            deviceModel = Build.MODEL ?: "",
            timestamp = TimestampMicros(
                timestampMicros = currentTimeSeconds,
                timezoneOffsetMinutes = timezoneOffsetMinutes
            ),
            deviceIdentifier = deviceTag.toString()
        )

        return ExternalDbSync(
            syncType = 1,
            gmsVersion = Constants.GMS_VERSION_CODE.toString(),
            snapshot = tableSync
        )
    }

    private fun encryptExternalDbSync(externalDbSync: ExternalDbSync, key: ByteArray, keyVersion: Int): GellerE2eeElement {
        val dbSyncBytes = ExternalDbSync.ADAPTER.encode(externalDbSync)
        val typedValue = GellerAny(typeUrl = EXTERNAL_DB_SYNC_TYPE_URL, value_ = dbSyncBytes.toByteString())
        val plaintext = GellerAny.ADAPTER.encode(typedValue)

        val iv = ByteArray(AES_GCM_IV_SIZE).apply { SecureRandom().nextBytes(this) }
        val ciphertextAndTag = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(AES_GCM_TAG_BITS, iv))
        }.doFinal(plaintext)

        return GellerE2eeElement(
            encryptedData = (iv + ciphertextAndTag).toByteString(),
            encryptionVersion = keyVersion
        )
    }

    private fun buildShardDataEntry(
        context: Context,
        accountName: String,
        gaiaId: String,
        databaseId: Long,
        shardIndex: Long,
        rows: List<RawSegmentRow>,
        key: ByteArray,
        keyVersion: Int
    ): GellerElement {
        val minId = shardIndex * SHARD_SIZE
        val maxId = minId + SHARD_SIZE - 1
        val gellerKey = "$databaseId;semantic_segment_table;$minId;$maxId"

        val externalDbSync = buildExternalDbSync(context, accountName, gaiaId, databaseId, rows)
        val e2eeElement = encryptExternalDbSync(externalDbSync, key, keyVersion)
        val e2eeBytes = GellerE2eeElement.ADAPTER.encode(e2eeElement)
        val typedValue = GellerAny(typeUrl = E2EE_TYPE_URL, value_ = e2eeBytes.toByteString())

        return GellerElement(
            elementId = gellerKey,
            payload = typedValue,
            timestamp = ElementTimestamp(timestampMicros = System.currentTimeMillis() * 1000)
        )
    }

    private fun splitIntoBatches(entries: List<GellerElement>): List<List<GellerElement>> {
        if (entries.isEmpty()) return emptyList()

        val batches = mutableListOf<List<GellerElement>>()
        var currentBatch = mutableListOf<GellerElement>()
        var currentSize = 0

        for (entry in entries) {
            val entrySize = GellerElement.ADAPTER.encodedSize(entry)
            if (currentSize + entrySize > MAX_BATCH_SIZE) {
                if (currentBatch.isNotEmpty()) {
                    batches.add(currentBatch)
                    currentBatch = mutableListOf()
                    currentSize = 0
                }
            }
            currentBatch.add(entry)
            currentSize += entrySize
        }

        if (currentBatch.isNotEmpty()) {
            batches.add(currentBatch)
        }

        return batches
    }
}
