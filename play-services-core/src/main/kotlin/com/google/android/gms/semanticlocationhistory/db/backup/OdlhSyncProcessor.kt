/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.db.backup

import android.content.Context
import android.util.Log
import com.google.android.gms.geller.GellerAny
import com.google.android.gms.geller.GellerE2eeElement
import com.google.android.gms.geller.GellerElement
import com.google.android.gms.geller.externaldb.ExternalDbSnapshot
import com.google.android.gms.geller.externaldb.ExternalDbSync
import com.google.android.gms.semanticlocationhistory.AES_GCM_IV_SIZE
import com.google.android.gms.semanticlocationhistory.AES_GCM_TAG_BITS
import com.google.android.gms.semanticlocationhistory.AES_KEY_SIZE
import com.google.android.gms.semanticlocationhistory.E2EE_TYPE_URL
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_ACTIVITY
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_MEMORY
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_PATH
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_PERIOD_SUMMARY
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_UNKNOWN
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_VISIT
import com.google.android.gms.semanticlocationhistory.db.SemanticSegment
import com.google.android.gms.semanticlocationhistory.loadKeyMaterials
import org.microg.gms.semanticlocationhistory.FinalizationStatus
import org.microg.gms.semanticlocationhistory.LocationHistorySegmentProto
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.runCatching

private const val TAG = "OdlhSyncProcessor"

object OdlhSyncProcessor {
    internal fun processMutation(
        context: Context,
        accountName: String,
        mutation: GellerElement,
        databaseId: Long
    ): List<SemanticSegment> {
        val typedValue = mutation.payload
        if (typedValue == null) {
            Log.w(TAG, "DataEntry has no value: key=${mutation.elementId}, timestamp=${mutation.timestamp}")
            return emptyList()
        }
        val typeUrl = typedValue.typeUrl
        val valueBytes = typedValue.value_.toByteArray()

        if (typeUrl == E2EE_TYPE_URL) {
            val e2eeElement = runCatching { GellerE2eeElement.ADAPTER.decode(bytes = valueBytes) }.getOrNull() ?: return emptyList()
            val decryptedBytes = decryptE2eeElement(context, accountName, e2eeElement) ?: return emptyList()
            return parseDecryptedData(decryptedBytes, mutation.elementId, databaseId)
        }

        return parseSegmentData(typeUrl, valueBytes, mutation.elementId, databaseId)
    }

    private fun parseSegmentData(typeUrl: String, data: ByteArray, key: String?, databaseId: Long): List<SemanticSegment> {
        return when {
            typeUrl.contains("ExternalDbSync") -> {
                runCatching {
                    val externalDbSync = ExternalDbSync.ADAPTER.decode(data)
                    val snapshot = externalDbSync.snapshot
                    if (snapshot != null) {
                        parseSnapshot(snapshot, databaseId)
                    } else {
                        emptyList()
                    }
                }.getOrElse { e ->
                    Log.e(TAG, "Failed to parse ExternalDbSync: ${e.message}", e)
                    emptyList()
                }
            }

            typeUrl.contains("LocationHistorySegmentProto") -> {
                parseLocationHistorySegment(data, key, databaseId)?.let { listOf(it) } ?: emptyList()
            }

            else -> {
                Log.w(TAG, "Unknown type_url: $typeUrl")
                emptyList()
            }
        }
    }

    private fun parseDecryptedData(decryptedBytes: ByteArray, key: String?, databaseId: Long): List<SemanticSegment> {
        return runCatching {
            val typedValue = GellerAny.ADAPTER.decode(decryptedBytes)
            val innerTypeUrl = typedValue.typeUrl
            val innerValue = typedValue.value_.toByteArray()

            if (innerValue.isNotEmpty()) {
                parseSegmentData(innerTypeUrl, innerValue, key, databaseId)
            } else {
                ExternalDbSync.ADAPTER.decode(decryptedBytes).snapshot?.let { parseSnapshot(it, databaseId) }
                    ?: run {
                        Log.w(TAG, "parseDecryptedData: no valid data found")
                        emptyList()
                    }
            }
        }.getOrElse { e ->
            Log.e(TAG, "Failed to parse decrypted data", e)
            parseLocationHistorySegment(decryptedBytes, key, databaseId)?.let { listOf(it) } ?: emptyList()
        }
    }

    private fun parseSnapshot(snapshot: ExternalDbSnapshot, databaseId: Long): List<SemanticSegment> {
        val columns = snapshot.columnNames
        val segmentColumnIndex = columns.indexOf("semantic_segment")
        if (segmentColumnIndex < 0) {
            Log.w(TAG, "No semantic_segment column found in columns: $columns")
            return emptyList()
        }
        val segmentIdColumnIndex = columns.indexOf("segment_id")

        val segments = snapshot.rows.mapNotNull { row ->
            runCatching {
                val values = row.values
                if (segmentColumnIndex >= values.size) return@runCatching null

                val segmentId = if (segmentIdColumnIndex in 0 until values.size) {
                    values[segmentIdColumnIndex].stringValue
                } else null

                val bytes = values[segmentColumnIndex].bytesValue?.toByteArray()
                if (bytes != null) {
                    parseLocationHistorySegment(bytes, segmentId, databaseId)
                } else {
                    null
                }
            }.getOrElse { e ->
                Log.w(TAG, "Failed to parse snapshot row: ${e.message}")
                null
            }
        }

        Log.d(TAG, "parseSnapshot: parsed ${segments.size}/${snapshot.rows.size} rows")
        return segments
    }

    private fun parseLocationHistorySegment(data: ByteArray, key: String?, databaseId: Long): SemanticSegment? {
        return runCatching {
            val proto = LocationHistorySegmentProto.ADAPTER.decode(data)

            val segmentId = proto.segment_id ?: key ?: UUID.randomUUID().toString()

            val startTime = proto.start_time?.seconds ?: 0L
            val endTime = proto.end_time?.seconds ?: 0L

            val segmentData = proto.segment_data
            val (segmentType, visitHierarchyLevel, visitFprint) = when {
                segmentData?.visit != null -> {
                    val visit = segmentData.visit!!
                    val fp = visit.place?.feature_id?.low
                    Triple(SEGMENT_TYPE_VISIT, visit.hierarchy_level ?: 0, fp)
                }

                segmentData?.activity != null -> Triple(SEGMENT_TYPE_ACTIVITY, 0, null)
                segmentData?.path != null -> Triple(SEGMENT_TYPE_PATH, 0, null)
                segmentData?.memory != null -> Triple(SEGMENT_TYPE_MEMORY, 0, null)
                segmentData?.summary != null -> Triple(SEGMENT_TYPE_PERIOD_SUMMARY, 0, null)
                else -> Triple(SEGMENT_TYPE_UNKNOWN, 0, null)
            }

            val hierarchyLevel = proto.hierarchy_level ?: visitHierarchyLevel
            val fprint = proto.finalization_state?.toLong() ?: visitFprint
            val shownInTimeline = !(proto.is_deleted ?: false)
            val isFinalized = proto.finalization_status in listOf(
                FinalizationStatus.BACKFILLED,
                FinalizationStatus.USER_EDITED
            )

            SemanticSegment(
                segmentId = segmentId,
                segmentType = segmentType,
                data = data,
                startTimestamp = startTime,
                endTimestamp = endTime,
                hierarchyLevel = hierarchyLevel,
                fprint = fprint,
                shownInTimeline = shownInTimeline,
                timestampMillis = System.currentTimeMillis(),
                databaseId = databaseId,
                isFinalized = isFinalized
            )
        }.getOrElse { e ->
            Log.e(TAG, "Failed to parse LocationHistorySegmentProto: ${e.message}", e)
            null
        }
    }

    internal fun decryptE2eeElement(context: Context, accountName: String, e2eeElement: GellerE2eeElement): ByteArray? {
        val encryptedData = e2eeElement.encryptedData?.toByteArray() ?: return null
        val encryptionVersion = e2eeElement.encryptionVersion ?: 0

        val keyMaterials = loadKeyMaterials(context, accountName)
        if (keyMaterials.isEmpty()) {
            Log.e(TAG, "No decryption keys found for ODLH")
            return null
        }

        val sortedMaterials = keyMaterials.let { materials ->
            if (encryptionVersion != 0) {
                materials.sortedByDescending { it.second == encryptionVersion }
            } else {
                materials
            }
        }

        for ((keyMaterial, _) in sortedMaterials) {
            for (aad in listOf(ByteArray(0), null)) {
                runCatching { decryptAesGcm(keyMaterial, encryptedData, aad) }
                    .onSuccess { return it }
            }
        }

        Log.e(TAG, "All decryption attempts failed")
        return null
    }

    internal fun decryptAesGcm(key: ByteArray, encryptedData: ByteArray, aad: ByteArray? = null): ByteArray {
        require(key.size == AES_KEY_SIZE) { "Key must be $AES_KEY_SIZE bytes (AES-256)" }
        require(encryptedData.size > AES_GCM_IV_SIZE + 16) { "Encrypted data too short" }

        val iv = encryptedData.copyOfRange(0, AES_GCM_IV_SIZE)
        val ciphertext = encryptedData.copyOfRange(AES_GCM_IV_SIZE, encryptedData.size)

        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(AES_GCM_TAG_BITS, iv))
            aad?.takeIf { it.isNotEmpty() }?.let { updateAAD(it) }
        }.doFinal(ciphertext)
    }
}
