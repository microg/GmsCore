/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.games.leaderboard;

import org.microg.gms.common.Hide;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Columns of a leaderboard score {@link com.google.android.gms.common.data.DataHolder}, in addition to the player
 * columns of the score holder.
 */
@Hide
public class LeaderboardScoreColumns {
    public static final String RANK = "rank";
    public static final String DISPLAY_RANK = "display_rank";
    public static final String RAW_SCORE = "raw_score";
    public static final String DISPLAY_SCORE = "display_score";
    public static final String ACHIEVED_TIMESTAMP = "achieved_timestamp";
    public static final String SCORE_TAG = "score_tag";
    public static final String DEFAULT_DISPLAY_NAME = "default_display_name";
    public static final String DEFAULT_DISPLAY_IMAGE_URI = "default_display_image_uri";
    public static final String DEFAULT_DISPLAY_IMAGE_URL = "default_display_image_url";

    public static final List<String> CURRENT_SCORE_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            RANK, DISPLAY_RANK, RAW_SCORE, DISPLAY_SCORE, ACHIEVED_TIMESTAMP, SCORE_TAG, DEFAULT_DISPLAY_NAME,
            DEFAULT_DISPLAY_IMAGE_URI, DEFAULT_DISPLAY_IMAGE_URL
    ));
}
