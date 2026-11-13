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
 * Columns of a leaderboard {@link com.google.android.gms.common.data.DataHolder}, one row per leaderboard variant.
 */
@Hide
public class LeaderboardColumns {
    public static final String EXTERNAL_LEADERBOARD_ID = "external_leaderboard_id";
    public static final String NAME = "name";
    public static final String BOARD_ICON_IMAGE_URI = "board_icon_image_uri";
    public static final String BOARD_ICON_IMAGE_URL = "board_icon_image_url";
    public static final String SCORE_ORDER = "score_order";

    public static final String TIMESPAN = "timespan";
    public static final String COLLECTION = "collection";
    public static final String PLAYER_RAW_SCORE = "player_raw_score";
    public static final String PLAYER_DISPLAY_SCORE = "player_display_score";
    public static final String PLAYER_RANK = "player_rank";
    public static final String PLAYER_DISPLAY_RANK = "player_display_rank";
    public static final String PLAYER_SCORE_TAG = "player_score_tag";
    public static final String TOTAL_SCORES = "total_scores";
    public static final String TOP_PAGE_TOKEN_NEXT = "top_page_token_next";
    public static final String WINDOW_PAGE_TOKEN_PREV = "window_page_token_prev";
    public static final String WINDOW_PAGE_TOKEN_NEXT = "window_page_token_next";

    public static final List<String> CURRENT_LEADERBOARD_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            EXTERNAL_LEADERBOARD_ID, NAME, BOARD_ICON_IMAGE_URI, BOARD_ICON_IMAGE_URL, SCORE_ORDER,
            TIMESPAN, COLLECTION, PLAYER_RAW_SCORE, PLAYER_DISPLAY_SCORE, PLAYER_RANK, PLAYER_DISPLAY_RANK,
            PLAYER_SCORE_TAG, TOTAL_SCORES, TOP_PAGE_TOKEN_NEXT, WINDOW_PAGE_TOKEN_PREV, WINDOW_PAGE_TOKEN_NEXT
    ));
}
