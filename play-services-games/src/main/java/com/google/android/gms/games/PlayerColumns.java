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

    public static final List<String> CURRENT_PLAYER_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            externalPlayerId,
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
}
