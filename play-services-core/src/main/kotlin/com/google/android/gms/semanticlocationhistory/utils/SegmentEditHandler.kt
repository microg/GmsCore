/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.utils

import android.util.Log
import com.google.android.gms.semanticlocationhistory.LocationHistorySegment
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_DELETED
import com.google.android.gms.semanticlocationhistory.SEGMENT_TYPE_MEMORY
import com.google.android.gms.semanticlocationhistory.TAG
import com.google.android.gms.semanticlocationhistory.db.OdlhStorageManager
import com.google.android.gms.semanticlocationhistory.db.SemanticSegment
import com.google.android.gms.semanticlocationhistory.utils.SegmentUtils.getHierarchyLevel
import com.google.android.gms.semanticlocationhistory.utils.SegmentUtils.toUserEditedProtoBytes
import com.google.android.gms.semanticlocationhistory.utils.SegmentUtils.TYPE_VISIT
import com.google.android.gms.semanticlocationhistory.utils.SegmentUtils.TYPE_ACTIVITY
import com.google.android.gms.semanticlocationhistory.utils.SegmentUtils.TYPE_MEMORY

object SegmentEditHandler {

    private const val TYPE_DELETION = 0

    fun editSegments(storageManager: OdlhStorageManager, gaiaId: String, segments: List<LocationHistorySegment>) {
        if (segments.all { it.type == TYPE_MEMORY }) {
            editMemorySegments(storageManager, gaiaId, segments)
            return
        }

        val allowedTypes = setOf(TYPE_DELETION, TYPE_VISIT, TYPE_ACTIVITY)
        for (seg in segments) {
            if (seg.type !in allowedTypes) {
                throw IllegalArgumentException(
                    "Segments must all be either Timeline Memories or Visits/Activities/Deletions, got type=${seg.type}"
                )
            }
        }

        editVisitActivitySegments(storageManager, gaiaId, segments)
    }

    private fun editVisitActivitySegments(
        storageManager: OdlhStorageManager,
        gaiaId: String,
        segments: List<LocationHistorySegment>
    ) {
        val sorted = segments.sortedWith(
            compareBy<LocationHistorySegment> { it.startTimestamp }
                .thenBy { getHierarchyLevel(it) }
        )

        validateBatchRules(sorted)

        val editedSegments = sorted.map { seg ->
            SemanticSegment(
                segmentId = seg.segmentId ?: "edit_${System.nanoTime()}",
                segmentType = seg.type,
                data = toUserEditedProtoBytes(seg),
                startTimestamp = seg.startTimestamp,
                endTimestamp = seg.endTimestamp,
                hierarchyLevel = if (seg.type == TYPE_VISIT) seg.visit?.hierarchyLevel ?: 0 else 0
            )
        }

        checkContiguity(editedSegments)

        storageManager.runInTransaction {
            val editRange = calculateNonSubVisitRange(editedSegments)
            val existingEdits = storageManager.queryEditedSegments(gaiaId, editRange.first, editRange.second)
                .filter { it.segmentType != SEGMENT_TYPE_DELETED }

            storageManager.deleteEditedSegmentsByTimeRange(gaiaId, editRange.first, editRange.second, excludeDeleted = true)

            val merged = storageManager.mergeSegments(existingEdits, editedSegments)
            val mergedRange = calculateNonSubVisitRange(merged)

            for (seg in merged) {
                if (seg.startTimestamp >= mergedRange.first && seg.endTimestamp <= mergedRange.second) {
                    val result = storageManager.insertEditedSegment(
                        gaiaId,
                        seg.copy(
                            blockStartTimestamp = mergedRange.first,
                            blockEndTimestamp = mergedRange.second,
                            isEditUploaded = false
                        )
                    )
                    if (result == -1L) {
                        throw RuntimeException("Failed to insert edited segment ${seg.segmentId}")
                    }
                }
            }
            Log.d(TAG, "editSegments: wrote ${merged.size} edited segments (range=${mergedRange.first}..${mergedRange.second})")
        }
    }

    private fun validateBatchRules(segments: List<LocationHistorySegment>) {
        val grouped = mutableMapOf<Int, MutableList<LocationHistorySegment>>()
        for (seg in segments) {
            val level = getHierarchyLevel(seg)

            if (seg.startTimestamp <= 0 || seg.endTimestamp <= 0 || seg.startTimestamp >= seg.endTimestamp) {
                throw IllegalArgumentException("Invalid segment time range: start=${seg.startTimestamp}, end=${seg.endTimestamp}")
            }

            grouped.getOrPut(level) { mutableListOf() }.add(seg)
        }

        grouped[0]?.let { level0 ->
            val sortedLevel0 = level0.sortedBy { it.startTimestamp }
            for (i in 1 until sortedLevel0.size) {
                if (sortedLevel0[i].startTimestamp != sortedLevel0[i - 1].endTimestamp) {
                    throw IllegalArgumentException("Level-0 segments must be contiguous")
                }
            }
        }

        for ((_, segs) in grouped) {
            val sortedSegs = segs.sortedBy { it.startTimestamp }
            for (i in 1 until sortedSegs.size) {
                if (sortedSegs[i].startTimestamp < sortedSegs[i - 1].endTimestamp) {
                    throw IllegalArgumentException("Segments at the same hierarchy level must not overlap")
                }
            }
        }

        for ((level, segs) in grouped) {
            if (level == 0) continue
            val parentLevel = level - 1
            val parents = grouped[parentLevel]
                ?: throw IllegalArgumentException("Hierarchy level $level has no parent level $parentLevel")

            for (seg in segs) {
                val contained = parents.any { parent ->
                    parent.type == TYPE_VISIT
                            && seg.startTimestamp >= parent.startTimestamp
                            && seg.endTimestamp <= parent.endTimestamp
                }
                if (!contained) {
                    throw IllegalArgumentException("Sub-level segment not contained by parent VISIT")
                }
            }
        }
    }

    private fun checkContiguity(segments: List<SemanticSegment>) {
        if (segments.isEmpty()) return

        val first = segments[0]
        if (first.isSubVisit) {
            throw IllegalArgumentException("First segment cannot be a sub-visit")
        }

        var prevEnd = first.endTimestamp
        for (i in 1 until segments.size) {
            val seg = segments[i]
            if (!seg.isSubVisit) {
                if (seg.startTimestamp != prevEnd) {
                    throw IllegalArgumentException("Non-sub-visit segments must be contiguous")
                }
                prevEnd = seg.endTimestamp
            }
        }
    }

    private fun editMemorySegments(storageManager: OdlhStorageManager, gaiaId: String, segments: List<LocationHistorySegment>) {
        for (segment in segments) {
            val segmentId = segment.segmentId ?: continue
            val protoBytes = toUserEditedProtoBytes(segment)

            val isEmpty = segment.timelineMemory?.trip == null && segment.timelineMemory?.note == null
            if (isEmpty) {
                val deleted = storageManager.deleteBySegmentId(gaiaId, segmentId)
                if (deleted == -1) {
                    throw RuntimeException("Failed to delete Timeline Memory segment $segmentId")
                }
                Log.d(TAG, "editSegments: deleted memory segment $segmentId")
            } else {
                val dbSegment = SemanticSegment(
                    segmentId = segmentId,
                    segmentType = SEGMENT_TYPE_MEMORY,
                    data = protoBytes,
                    startTimestamp = segment.startTimestamp,
                    endTimestamp = segment.endTimestamp,
                    hierarchyLevel = 0,
                    shownInTimeline = true,
                    databaseId = storageManager.getDatabaseId()
                )
                storageManager.runInTransaction {
                    storageManager.deleteBySegmentId(gaiaId, segmentId)
                    val result = storageManager.insertSegment(gaiaId, dbSegment)
                    if (result == -1L) {
                        throw RuntimeException("Failed to store Timeline Memory segment $segmentId")
                    }
                }
                Log.d(TAG, "editSegments: updated memory segment $segmentId")
            }
        }
    }

    private fun calculateNonSubVisitRange(segments: List<SemanticSegment>): Pair<Long, Long> {
        if (segments.isEmpty()) return Pair(0L, 0L)
        val lastNonSubVisitIdx = segments.indexOfLast { !it.isSubVisit }
        if (lastNonSubVisitIdx == -1) return Pair(0L, 0L)
        return Pair(segments.first().startTimestamp, segments[lastNonSubVisitIdx].endTimestamp)
    }

    fun deleteHistory(storageManager: OdlhStorageManager, gaiaId: String, startTime: Long, endTime: Long): Pair<Int, Int> {
        val deletedSegments = storageManager.deleteSegmentsByTimeRange(gaiaId, startTime, endTime)
        Log.d(TAG, "deleteHistory: deleted $deletedSegments segments")

        val deletedEdits = storageManager.deleteEditedSegmentsByTimeRange(gaiaId, startTime, endTime)
        Log.d(TAG, "deleteHistory: deleted $deletedEdits edits")

        if (deletedSegments == -1 || deletedEdits == -1) {
            Log.w(TAG, "deleteHistory: some delete operations failed")
        }

        return Pair(deletedSegments, deletedEdits)
    }
}