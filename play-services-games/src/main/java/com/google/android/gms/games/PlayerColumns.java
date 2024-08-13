/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayerColumns {
    public static final String externalPlayerId = "external_player_id";
    public static final String profileName = "profile_name";
    public static final String profileIconImageId = "profile_icon_image_id";
    public static final String profileIconImageUri = "profile_icon_image_uri";
    public static final String profileIconImageUrl = "profile_icon_image_url";
    public static final String profileHiResImageId = "profile_hi_res_image_id";
    public static final String profileHiResImageUri = "profile_hi_res_image_uri";
    public static final String profileHiResImageUrl = "profile_hi_res_image_url";
    public static final String lastUpdated = "last_updated";
    public static final String isInCircles = "is_in_circles";
    public static final String playedWithTimestamp = "played_with_timestamp";
    public static final String currentXpTotal = "current_xp_total";
    public static final String currentLevel = "current_level";
    public static final String currentLevelMinXp = "current_level_min_xp";
    public static final String currentLevelMaxXp = "current_level_max_xp";
    public static final String nextLevel = "next_level";
    public static final String nextLevelMaxXp = "next_level_max_xp";
    public static final String lastLevelUpTimestamp = "last_level_up_timestamp";
    public static final String playerTitle = "player_title";
    public static final String hasAllPublicAcls = "has_all_public_acls";
    public static final String isProfileVisible = "is_profile_visible";
    public static final String mostRecentExternalGameId = "most_recent_external_game_id";
    public static final String mostRecentGameName = "most_recent_game_name";
    public static final String mostRecentActivityTimestamp = "most_recent_activity_timestamp";
    public static final String mostRecentGameIconId = "most_recent_game_icon_id";
    public static final String mostRecentGameIconUri = "most_recent_game_icon_uri";
    public static final String mostRecentGameHiResId = "most_recent_game_hi_res_id";
    public static final String mostRecentGameHiResUri = "most_recent_game_hi_res_uri";
    public static final String mostRecentGameFeaturedId = "most_recent_game_featured_id";
    public static final String mostRecentGameFeaturedUri = "most_recent_game_featured_uri";
    public static final String hasDebugAccess = "has_debug_access";
    public static final String gamerTag = "gamer_tag";
    public static final String realName = "real_name";
    public static final String bannerImageLandscapeId = "banner_image_landscape_id";
    public static final String bannerImageLandscapeUri = "banner_image_landscape_uri";
    public static final String bannerImageLandscapeUrl = "banner_image_landscape_url";
    public static final String bannerImagePortraitId = "banner_image_portrait_id";
    public static final String bannerImagePortraitUri = "banner_image_portrait_uri";
    public static final String bannerImagePortraitUrl = "banner_image_portrait_url";
    public static final String gamerFriendStatus = "gamer_friend_status";
    public static final String gamerFriendUpdateTimestamp = "gamer_friend_update_timestamp";
    public static final String isMuted = "is_muted";
    public static final String totalUnlockedAchievements = "total_unlocked_achievements";
    public static final String playTogetherFriendStatus = "play_together_friend_status";
    public static final String playTogetherNickname = "play_together_nickname";
    public static final String playTogetherInvitationNickname = "play_together_invitation_nickname";
    public static final String nicknameAbuseReportToken = "nickname_abuse_report_token";
    public static final String friendsListVisibility = "friends_list_visibility";
    public static final String alwaysAutoSignIn = "always_auto_sign_in";
    public static final String profileCreationTimestamp = "profile_creation_timestamp";
    public static final String gamePlayerId = "game_player_id";
    public static final String externalGameId = "external_game_id";
    public static final String primaryCategory = "primary_category";
    public static final String secondaryCategory = "secondary_category";

    public static final List<String> CURRENT_PLAYER_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            externalPlayerId,externalGameId,primaryCategory,secondaryCategory,
            profileIconImageId, profileHiResImageId, profileIconImageUri, profileIconImageUrl, profileHiResImageUri, profileHiResImageUrl,
            profileName, lastUpdated, isInCircles, hasAllPublicAcls, hasDebugAccess, isProfileVisible,
            currentXpTotal, currentLevel, currentLevelMinXp, currentLevelMaxXp, nextLevel, nextLevelMaxXp, lastLevelUpTimestamp,
            playerTitle,
            mostRecentExternalGameId, mostRecentGameName, mostRecentActivityTimestamp, mostRecentGameIconId, mostRecentGameIconUri, mostRecentGameHiResId, mostRecentGameHiResUri, mostRecentGameFeaturedId, mostRecentGameFeaturedUri,
            gamerTag, realName,
            bannerImageLandscapeId, bannerImageLandscapeUri, bannerImageLandscapeUrl, bannerImagePortraitId, bannerImagePortraitUri, bannerImagePortraitUrl,
            totalUnlockedAchievements,
            playTogetherFriendStatus, playTogetherNickname, playTogetherInvitationNickname,
            profileCreationTimestamp, nicknameAbuseReportToken, friendsListVisibility, alwaysAutoSignIn,
            gamerFriendStatus, gamerFriendUpdateTimestamp,
            isMuted, gamePlayerId
    ));

    public static final List<String> XX = Collections.unmodifiableList(Arrays.asList(
            "owner_id",
            "profile_icon_image_id",
            "is_profile_visible",
            "is_muted",
            "banner_image_portrait_url",
            "profileless_recall_enabled_v3",
            "primary_category",
            "player_title",
            "next_level_max_xp",
            "gamer_friend_status",
            "screenshot_image_uris",
            "gamepad_support",
            "last_updated",
            "gamer_friend_update_timestamp",
            "most_recent_game_hi_res_uri",
            "snapshot_content_download_status",
            "game_hi_res_image_uri",
            "game_hi_res_image_url",
            "banner_image_portrait_uri",
            "turn_based_support",
            "gamer_tag",
            "game_description",
            "cover_icon_image_url",
            "sync_status",
            "snapshot_content_download_url",
            "achievement_total_count",
            "cover_icon_image_uri",
            "banner_image_landscape_id",
            "external_snapshot_id",
            "profile_creation_timestamp",
            "most_recent_game_featured_uri",
            "gameplay_acl_status",
            "real_time_support",
            "modification_token",
            "most_recent_game_featured_id",
            "game_hi_res_image_id",
            "featured_image_uri",
            "current_level_min_xp",
            "featured_image_url",
            "muted",
            "identity_sharing_confirmed",
            "metadata_version",
            "revision_id",
            "content_id",
            "drive_resource_id_string",
            "total_unlocked_achievements",
            "screenshot_image_heights",
            "pending_change_count",
            "most_recent_game_icon_uri",
            "most_recent_activity_timestamp",
            "theme_color",
            "external_game_id",
            "sync_token",
            "current_level",
            "developer_name",
            "current_xp_total",
            "has_debug_access",
            "has_all_public_acls",
            "profile_name",
            "screenshot_image_widths",
            "cover_icon_image_width",
            "play_together_nickname",
            "is_in_circles",
            "next_level",
            "profile_icon_image_url",
            "external_player_id",
            "profile_icon_image_uri",
            "always_auto_sign_in",
            "featured_image_id",
            "last_played_server_time",
            "game_icon_image_id",
            "last_level_up_timestamp",
            "progress_value",
            "visible",
            "friends_list_visibility",
            "most_recent_game_hi_res_id",
            "snapshot_content_size",
            "display_name",
            "most_recent_game_icon_id",
            "banner_image_portrait_id",
            "cover_icon_image_id",
            "secondary_category",
            "unique_name",
            "last_connection_local_time",
            "drive_resolved_id_string",
            "banner_image_landscape_uri",
            "_id",
            "banner_image_landscape_url",
            "most_recent_external_game_id",
            "snapshot_content_filename",
            "installed",
            "target_instance",
            "most_recent_game_name",
            "last_modified_timestamp",
            "description",
            "real_name",
            "title",
            "snapshots_enabled",
            "cover_icon_image_height",
            "game_icon_image_url",
            "duration",
            "game_icon_image_uri",
            "device_name",
            "video_url",
            "current_level_max_xp",
            "leaderboard_count",
            "game_id",
            "screenshot_image_ids",
            "play_together_invitation_nickname",
            "play_enabled_game",
            "nickname_abuse_report_token",
            "play_together_friend_status",
            "profile_hi_res_image_url",
            "profile_hi_res_image_uri",
            "conflict_id",
            "package_name",
            "last_synced_local_time",
            "profile_hi_res_image_id"
    ));
}
