/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import static com.google.android.gms.games.GameColumns.ACHIEVEMENT_TOTAL_COUNT;
import static com.google.android.gms.games.GameColumns.DEVELOPER_NAME;
import static com.google.android.gms.games.GameColumns.DISPLAY_NAME;
import static com.google.android.gms.games.GameColumns.EXTERNAL_GAME_ID;
import static com.google.android.gms.games.GameColumns.FEATURED_IMAGE_URI;
import static com.google.android.gms.games.GameColumns.FEATURED_IMAGE_URL;
import static com.google.android.gms.games.GameColumns.GAMEPAD_SUPPORT;
import static com.google.android.gms.games.GameColumns.GAME_DESCRIPTION;
import static com.google.android.gms.games.GameColumns.GAME_HI_RES_IMAGE_URI;
import static com.google.android.gms.games.GameColumns.GAME_HI_RES_IMAGE_URL;
import static com.google.android.gms.games.GameColumns.GAME_ICON_IMAGE_URI;
import static com.google.android.gms.games.GameColumns.GAME_ICON_IMAGE_URL;
import static com.google.android.gms.games.GameColumns.IDENTITY_SHARING_CONFIRMED;
import static com.google.android.gms.games.GameColumns.INSTALLED;
import static com.google.android.gms.games.GameColumns.LEADERBOARD_COUNT;
import static com.google.android.gms.games.GameColumns.MUTED;
import static com.google.android.gms.games.GameColumns.PACKAGE_NAME;
import static com.google.android.gms.games.GameColumns.PLAY_ENABLED_GAME;
import static com.google.android.gms.games.GameColumns.PRIMARY_CATEGORY;
import static com.google.android.gms.games.GameColumns.REAL_TIME_SUPPORT;
import static com.google.android.gms.games.GameColumns.SECONDARY_CATEGORY;
import static com.google.android.gms.games.GameColumns.SNAPSHOTS_ENABLED;
import static com.google.android.gms.games.GameColumns.THEME_COLOR;
import static com.google.android.gms.games.GameColumns.TURN_BASED_SUPPORT;

import android.annotation.SuppressLint;
import android.database.CharArrayBuffer;
import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.data.DataBufferRef;
import com.google.android.gms.common.data.DataHolder;

@SuppressLint("ParcelCreator")
public class GameRef extends DataBufferRef implements Game {
    public GameRef(DataHolder var1, int var2) {
        super(var1, var2);
    }

    public String getApplicationId() {
        return this.getString(EXTERNAL_GAME_ID);
    }

    public String getDisplayName() {
        return this.getString(DISPLAY_NAME);
    }

    public void getDisplayName(CharArrayBuffer var1) {
        this.copyToBuffer(DISPLAY_NAME, var1);
    }

    public String getPrimaryCategory() {
        return this.getString(PRIMARY_CATEGORY);
    }

    public String getSecondaryCategory() {
        return this.getString(SECONDARY_CATEGORY);
    }

    public String getDescription() {
        return this.getString(GAME_DESCRIPTION);
    }

    public void getDescription(CharArrayBuffer var1) {
        this.copyToBuffer(GAME_DESCRIPTION, var1);
    }

    public String getDeveloperName() {
        return this.getString(DEVELOPER_NAME);
    }

    public void getDeveloperName(CharArrayBuffer var1) {
        this.copyToBuffer(DEVELOPER_NAME, var1);
    }

    public Uri getIconImageUri() {
        return this.parseUri(GAME_ICON_IMAGE_URI);
    }

    public String getIconImageUrl() {
        return this.getString(GAME_ICON_IMAGE_URL);
    }

    public Uri getHiResImageUri() {
        return this.parseUri(GAME_HI_RES_IMAGE_URI);
    }

    public String getHiResImageUrl() {
        return this.getString(GAME_HI_RES_IMAGE_URL);
    }

    public Uri getFeaturedImageUri() {
        return this.parseUri(FEATURED_IMAGE_URI);
    }

    public String getFeaturedImageUrl() {
        return this.getString(FEATURED_IMAGE_URL);
    }

    public boolean isPlayEnabledGame() {
        return this.getBoolean(PLAY_ENABLED_GAME);
    }

    public boolean isMuted() {
        return this.getBoolean(MUTED);
    }

    public boolean isIdentitySharingConfirmed() {
        return this.getBoolean(IDENTITY_SHARING_CONFIRMED);
    }

    public boolean isInstanceInstalled() {
        return this.getInteger(INSTALLED) > 0;
    }

    public String getInstancePackageName() {
        return this.getString(PACKAGE_NAME);
    }

    @Override
    public int getGameplayAclStatus() {
        return 0;
    }

    public int getAchievementTotalCount() {
        return this.getInteger(ACHIEVEMENT_TOTAL_COUNT);
    }

    public int getLeaderboardCount() {
        return this.getInteger(LEADERBOARD_COUNT);
    }

    public boolean isRealTimeMultiplayerEnabled() {
        return this.getInteger(REAL_TIME_SUPPORT) > 0;
    }

    public boolean isTurnBasedMultiplayerEnabled() {
        return this.getInteger(TURN_BASED_SUPPORT) > 0;
    }

    public boolean areSnapshotsEnabled() {
        return this.getInteger(SNAPSHOTS_ENABLED) > 0;
    }

    public String getThemeColor() {
        return this.getString(THEME_COLOR);
    }

    public boolean hasGamepadSupport() {
        return this.getInteger(GAMEPAD_SUPPORT) > 0;
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        this.freeze().writeToParcel(dest, flags);
    }

    @Override
    public Game freeze() {
        return new GameEntity(this);
    }
}
