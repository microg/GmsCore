/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.games;

import org.microg.gms.common.Hide;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Hide
public class GameColumns {
    public static final String EXTERNAL_GAME_ID = "external_game_id";
    public static final String DISPLAY_NAME = "display_name";
    public static final String PRIMARY_CATEGORY = "primary_category";
    public static final String SECONDARY_CATEGORY = "secondary_category";
    public static final String GAME_DESCRIPTION = "game_description";
    public static final String DEVELOPER_NAME = "developer_name";
    public static final String GAME_ICON_IMAGE_URI = "game_icon_image_uri";
    public static final String GAME_ICON_IMAGE_URL = "game_icon_image_url";
    public static final String GAME_HI_RES_IMAGE_URI = "game_hi_res_image_uri";
    public static final String GAME_HI_RES_IMAGE_URL = "game_hi_res_image_url";
    public static final String FEATURED_IMAGE_URI = "featured_image_uri";
    public static final String FEATURED_IMAGE_URL = "featured_image_url";
    public static final String PLAY_ENABLED_GAME = "play_enabled_game";
    public static final String MUTED = "muted";
    public static final String IDENTITY_SHARING_CONFIRMED = "identity_sharing_confirmed";
    public static final String INSTALLED = "installed";
    public static final String PACKAGE_NAME = "package_name";
    public static final String ACHIEVEMENT_TOTAL_COUNT = "achievement_total_count";
    public static final String LEADERBOARD_COUNT = "leaderboard_count";
    public static final String REAL_TIME_SUPPORT = "real_time_support";
    public static final String TURN_BASED_SUPPORT = "turn_based_support";
    public static final String SNAPSHOTS_ENABLED = "snapshots_enabled";
    public static final String THEME_COLOR = "theme_color";
    public static final String GAMEPAD_SUPPORT = "gamepad_support";

    public static final List<String> CURRENT_GAME_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            EXTERNAL_GAME_ID, DISPLAY_NAME, PRIMARY_CATEGORY, SECONDARY_CATEGORY, GAME_DESCRIPTION, DEVELOPER_NAME, GAME_ICON_IMAGE_URI, GAME_ICON_IMAGE_URL,
            GAME_HI_RES_IMAGE_URI, GAME_HI_RES_IMAGE_URL, FEATURED_IMAGE_URI, FEATURED_IMAGE_URL, PLAY_ENABLED_GAME, MUTED, IDENTITY_SHARING_CONFIRMED, INSTALLED,
            PACKAGE_NAME, ACHIEVEMENT_TOTAL_COUNT, LEADERBOARD_COUNT, REAL_TIME_SUPPORT, TURN_BASED_SUPPORT, SNAPSHOTS_ENABLED, THEME_COLOR, GAMEPAD_SUPPORT
    ));
}
