/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.utils

import android.content.ContentValues
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.data.DataHolder
import com.google.android.gms.geller.GellerDataType
import com.google.android.gms.semanticlocationhistory.LocationHistorySegment
import com.google.android.gms.semanticlocationhistory.LocationHistorySegmentRequest
import com.google.android.gms.semanticlocationhistory.TAG
import com.google.android.gms.semanticlocationhistory.db.OdlhStorageManager
import com.google.android.gms.semanticlocationhistory.utils.SegmentUtils.checkConfirmationLevel
import com.google.android.gms.semanticlocationhistory.utils.SegmentUtils.TYPE_VISIT

object SegmentQueryHandler {

    private const val DEFAULT_RETENTION_DAYS = 36500L

    private const val LOOKUP_TYPE_SEGMENT_ID = 1
    private const val LOOKUP_TYPE_TIME_RANGE = 2
    private const val LOOKUP_TYPE_INCLUDE_DELETED = 3
    private const val LOOKUP_TYPE_SEGMENT_TYPE = 4
    private const val LOOKUP_TYPE_FINALIZATION = 5
    private const val LOOKUP_TYPE_PLACE_FEATURE_ID = 6

    internal data class SegmentQueryParams(
        val startTime: Long = 0L,
        val endTime: Long = Long.MAX_VALUE,
        val segmentType: Int = 0,
        val segmentId: String? = null,
        val includeDeleted: Int = 1,
        val confirmationLevel: Int = -1,
        val placeFprint: Long? = null
    )

    internal fun parseSegmentRequest(request: LocationHistorySegmentRequest?): SegmentQueryParams? {
        if (request?.parameters.isNullOrEmpty()) return SegmentQueryParams()

        var startTime = 0L
        var endTime = Long.MAX_VALUE
        var segmentType = 0
        var segmentId: String? = null
        var includeDeleted = 1
        var confirmationLevel = -1
        var placeFprint: Long? = null

        val seenTypes = mutableSetOf<Int>()

        for (param in request!!.parameters) {
            if (!seenTypes.add(param.type)) {
                Log.w(TAG, "getSegments: duplicate LookupParameters type=${param.type}")
                return null
            }

            when (param.type) {
                LOOKUP_TYPE_SEGMENT_ID -> {
                    segmentId = param.segmentId
                }

                LOOKUP_TYPE_TIME_RANGE -> {
                    param.timeRangeFilter?.let { range ->
                        range.startTime?.let { startTime = it }
                        range.endTime?.let { endTime = it }
                    }
                    if (startTime >= endTime) {
                        Log.w(TAG, "getSegments: invalid time range: start=$startTime >= end=$endTime")
                        return null
                    }
                }

                LOOKUP_TYPE_INCLUDE_DELETED -> {
                    includeDeleted = if (param.b4) 3 else 2
                }

                LOOKUP_TYPE_SEGMENT_TYPE -> {
                    segmentType = param.i5 ?: 0
                }

                LOOKUP_TYPE_FINALIZATION -> {
                    confirmationLevel = param.i6 ?: -1
                }

                LOOKUP_TYPE_PLACE_FEATURE_ID -> {
                    placeFprint = param.fprint
                }
            }
        }

        return SegmentQueryParams(
            startTime = startTime,
            endTime = endTime,
            segmentType = segmentType,
            segmentId = segmentId,
            includeDeleted = includeDeleted,
            confirmationLevel = confirmationLevel,
            placeFprint = placeFprint
        )
    }

    internal fun applyPostFilters(segments: List<LocationHistorySegment>, params: SegmentQueryParams): List<LocationHistorySegment> {
        return segments.filter { segment ->
            if (params.segmentId != null && segment.segmentId != params.segmentId) {
                return@filter false
            }

            if (params.segmentType > 0 && segment.type != params.segmentType) {
                return@filter false
            }

            if (!checkConfirmationLevel(segment, params.confirmationLevel)) {
                return@filter false
            }

            if (params.placeFprint != null && segment.type == TYPE_VISIT) {
                val visitFprint = segment.visit?.place?.identifier?.fprint ?: 0L
                if (visitFprint != params.placeFprint) {
                    return@filter false
                }
            }

            true
        }
    }

    private val DATA_COLUMNS = arrayOf("data")

    fun buildSegmentsDataHolder(segments: List<LocationHistorySegment>): DataHolder {
        val builder = DataHolder.builder(DATA_COLUMNS)

        if (segments.isEmpty()) {
            return builder.build(CommonStatusCodes.SUCCESS)
        }

        segments.forEach { segment ->
            val parcel = Parcel.obtain()
            try {
                segment.writeToParcel(parcel, 0)
                val bytes = parcel.marshall()

                val values = ContentValues().apply {
                    put("data", bytes)
                }
                builder.withRow(values)
            } finally {
                parcel.recycle()
            }
        }

        return builder.build(CommonStatusCodes.SUCCESS)
    }

    fun queryAndFilterSegments(storageManager: OdlhStorageManager, gaiaId: String, request: LocationHistorySegmentRequest?): DataHolder {
        val params = parseSegmentRequest(request)
        if (params == null) {
            Log.w(TAG, "getSegments: invalid request parameters")
            return DataHolder.empty(CommonStatusCodes.SUCCESS)
        }
        Log.d(TAG, "getSegments: params=$params")

        val retentionStartSec = System.currentTimeMillis() / 1000 - DEFAULT_RETENTION_DAYS * 86400
        val adjustedStart = maxOf(params.startTime, retentionStartSec)

        storageManager.purgeDeletedEdits(gaiaId, adjustedStart)

        val lastSyncTime = storageManager.getLastSyncTime(
            GellerDataType.ENCRYPTED_ONDEVICE_LOCATION_HISTORY.value
        )
        val pathMemoryEnd = if (lastSyncTime != null && lastSyncTime > 0)
            minOf(params.endTime, lastSyncTime / 1000) else null

        val segmentTypeFilter = if (params.segmentType > 0) intArrayOf(params.segmentType) else null
        val dbSegments = storageManager.querySegments(
            gaiaId = gaiaId,
            startTime = adjustedStart,
            endTime = params.endTime,
            segmentTypes = segmentTypeFilter,
            placeFprint = params.placeFprint,
            pathMemoryEndTime = pathMemoryEnd
        )
        Log.d(TAG, "getSegments: found ${dbSegments.size} segments in database")

        val converted = dbSegments.mapNotNull { segment ->
            try {
                SegmentConverter.toLocationHistorySegment(segment)
            } catch (e: Exception) {
                Log.w(TAG, "getSegments: failed to convert segment ${segment.segmentId}", e)
                null
            }
        }

        val filtered = applyPostFilters(converted, params)
        Log.d(TAG, "getSegments: ${converted.size} -> ${filtered.size} after filtering")

        storageManager.purgeTombstones(gaiaId)
        val tombstones = storageManager.getTombstones(gaiaId)
        val afterTombstone = if (tombstones.isNotEmpty()) {
            applyTombstoneFilter(filtered, tombstones)
        } else {
            filtered
        }
        Log.d(TAG, "getSegments: ${filtered.size} -> ${afterTombstone.size} after tombstone filter")

        return buildSegmentsDataHolder(afterTombstone)
    }

    internal fun applyTombstoneFilter(segments: List<LocationHistorySegment>, tombstones: List<OdlhStorageManager.Tombstone>): List<LocationHistorySegment> {
        val sorted = tombstones.sortedBy { it.startTimeSec }
        return segments.filter { segment ->
            sorted.none { ts ->
                if (ts.startTimeSec >= segment.endTimestamp) return@none false
                ts.endTimeSec > segment.startTimestamp
            }
        }
    }
}