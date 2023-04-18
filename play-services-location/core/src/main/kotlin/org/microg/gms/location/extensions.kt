/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location

import android.location.Location
import androidx.core.location.LocationCompat
import com.google.android.gms.common.Feature

internal val Location.elapsedMillis: Long
    get() = LocationCompat.getElapsedRealtimeMillis(this)

internal val FEATURES = arrayOf(
    Feature("name_ulr_private", 1),
    Feature("driving_mode", 6),
    Feature("name_sleep_segment_request", 1),
    Feature("support_context_feature_id", 1),
    Feature("get_current_location", 2),
    Feature("get_last_activity_feature_id", 1),
    Feature("get_last_location_with_request", 1),
    Feature("set_mock_mode_with_callback", 1),
    Feature("set_mock_location_with_callback", 1),
    Feature("inject_location_with_callback", 1),
    Feature("location_updates_with_callback", 1),
    Feature("user_service_developer_features", 1),
    Feature("user_service_location_accuracy", 1),
    Feature("user_service_safety_and_emergency", 1),

    Feature("use_safe_parcelable_in_intents", 1)
)