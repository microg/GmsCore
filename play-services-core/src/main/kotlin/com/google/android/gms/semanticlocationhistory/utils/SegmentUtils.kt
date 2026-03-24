/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.utils

import android.util.Log
import com.google.android.gms.semanticlocationhistory.LocationHistorySegment
import com.google.android.gms.semanticlocationhistory.TAG
import org.microg.gms.semanticlocationhistory.FinalizationStatus

object SegmentUtils {

    const val TYPE_VISIT = 1
    const val TYPE_ACTIVITY = 2
    const val TYPE_MEMORY = 4
    const val TYPE_PATH = 3
    const val TYPE_PERIOD_SUMMARY = 5

    const val FINALIZATION_STABILIZED = 1
    const val FINALIZATION_FINALIZED = 2
    const val FINALIZATION_USER_EDITED = 3
    const val FINALIZATION_BACKFILLED = 4

    internal fun getHierarchyLevel(segment: LocationHistorySegment): Int =
        if (segment.type == TYPE_VISIT) segment.visit?.hierarchyLevel ?: 0 else 0

    fun toUserEditedProtoBytes(segment: LocationHistorySegment): ByteArray {
        return try {
            SegmentConverter.buildProto(segment, FinalizationStatus.USER_EDITED).encode()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build USER_EDITED proto, falling back", e)
            SegmentConverter.toProtoBytes(segment)
        }
    }

    internal fun checkConfirmationLevel(segment: LocationHistorySegment, confirmationLevel: Int): Boolean {
        if (confirmationLevel <= 0) return true
        val status = segment.finalizationStatus
        return when (confirmationLevel) {
            1 -> status >= FINALIZATION_STABILIZED
            2 -> status >= FINALIZATION_FINALIZED
            3 -> status == FINALIZATION_USER_EDITED
            4 -> status == FINALIZATION_BACKFILLED || status == FINALIZATION_USER_EDITED
            else -> true
        }
    }
}