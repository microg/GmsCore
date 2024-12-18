/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.games.snapshot;

import org.microg.gms.common.Hide;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Hide
public class SnapshotColumns {
    public static final String EXTERNAL_SNAPSHOT_ID = "external_snapshot_id";
    public static final String COVER_ICON_IMAGE_URI = "cover_icon_image_uri";
    public static final String COVER_ICON_IMAGE_URL = "cover_icon_image_url";
    public static final String COVER_ICON_IMAGE_HEIGHT = "cover_icon_image_height";
    public static final String COVER_ICON_IMAGE_WIDTH = "cover_icon_image_width";
    public static final String UNIQUE_NAME = "unique_name";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";
    public static final String DURATION = "duration";
    public static final String PENDING_CHANGE_COUNT = "pending_change_count";
    public static final String PROGRESS_VALUE = "progress_value";
    public static final String DEVICE_NAME = "device_name";

    public static final List<String> CURRENT_GAME_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            EXTERNAL_SNAPSHOT_ID, COVER_ICON_IMAGE_URI, COVER_ICON_IMAGE_URL, COVER_ICON_IMAGE_HEIGHT,
            COVER_ICON_IMAGE_WIDTH, UNIQUE_NAME, TITLE, DESCRIPTION, LAST_MODIFIED_TIMESTAMP, DURATION,
            PENDING_CHANGE_COUNT, PROGRESS_VALUE, DEVICE_NAME
    ));
}
