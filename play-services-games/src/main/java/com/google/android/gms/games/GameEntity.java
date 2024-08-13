/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import android.database.CharArrayBuffer;
import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.util.DataUtils;

@SafeParcelable.Class
public class GameEntity extends AbstractSafeParcelable implements Game {
    @Field(value = 1, getterName = "getApplicationId")
    private final String applicationId;
    @Field(value = 2, getterName = "getDisplayName")
    private final String displayName;
    @Field(value = 3, getterName = "getPrimaryCategory")
    private final String primaryCategory;
    @Field(value = 4, getterName = "getSecondaryCategory")
    private final String secondaryCategory;
    @Field(value = 5, getterName = "getDescription")
    private final String description;
    @Field(value = 6, getterName = "getDeveloperName")
    private final String developerName;
    @Field(value = 7, getterName = "getIconImageUri")
    private final Uri iconImageUri;
    @Field(value = 8, getterName = "getHiResImageUri")
    private final Uri hiResImageUri;
    @Field(value = 9, getterName = "getFeaturedImageUri")
    private final Uri featuredImageUri;
    @Field(value = 10, getterName = "isPlayEnabledGame")
    private final boolean isPlayEnabledGame;
    @Field(value = 11, getterName = "isInstanceInstalled")
    private final boolean isInstanceInstalled;
    @Field(value = 12, getterName = "getInstancePackageName")
    private final String instancePackageName;
    @Field(value = 13, getterName = "getGameplayAclStatus")
    private final int gameplayAclStatus;
    @Field(value = 14, getterName = "getAchievementTotalCount")
    private final int achievementTotalCount;
    @Field(value = 15, getterName = "getLeaderboardCount")
    private final int leaderboardCount;
    @Field(value = 16, getterName = "isRealTimeMultiplayerEnabled")
    private final boolean isRealTimeMultiplayerEnabled;
    @Field(value = 17, getterName = "isTurnBasedMultiplayerEnabled")
    private final boolean isTurnBasedMultiplayerEnabled;
    @Field(value = 18, getterName = "getIconImageUrl")
    private final String iconImageUrl;
    @Field(value = 19, getterName = "getHiResImageUrl")
    private final String hiResImageUrl;
    @Field(value = 20, getterName = "getFeaturedImageUrl")
    private final String featuredImageUrl;
    @Field(value = 21, getterName = "isMuted")
    private final boolean isMuted;
    @Field(value = 22, getterName = "isIdentitySharingConfirmed")
    private final boolean isIdentitySharingConfirmed;
    @Field(value = 23, getterName = "areSnapshotsEnabled")
    private final boolean areSnapshotsEnabled;
    @Field(value = 24, getterName = "getThemeColor")
    private final String getThemeColor;
    @Field(value = 25, getterName = "hasGamepadSupport")
    private final boolean hasGamepadSupport;

    public GameEntity(Game game) {
        this.applicationId = game.getApplicationId();
        this.primaryCategory = game.getPrimaryCategory();
        this.secondaryCategory = game.getSecondaryCategory();
        this.description = game.getDescription();
        this.developerName = game.getDeveloperName();
        this.displayName = game.getDisplayName();
        this.iconImageUri = game.getIconImageUri();
        this.iconImageUrl = game.getIconImageUrl();
        this.hiResImageUri = game.getHiResImageUri();
        this.hiResImageUrl = game.getHiResImageUrl();
        this.featuredImageUri = game.getFeaturedImageUri();
        this.featuredImageUrl = game.getFeaturedImageUrl();
        this.isPlayEnabledGame = game.isPlayEnabledGame();
        this.isInstanceInstalled = game.isInstanceInstalled();
        this.instancePackageName = game.getInstancePackageName();
        this.gameplayAclStatus = 1;
        this.achievementTotalCount = game.getAchievementTotalCount();
        this.leaderboardCount = game.getLeaderboardCount();
        this.isRealTimeMultiplayerEnabled = game.isRealTimeMultiplayerEnabled();
        this.isTurnBasedMultiplayerEnabled = game.isTurnBasedMultiplayerEnabled();
        this.isMuted = game.isMuted();
        this.isIdentitySharingConfirmed = game.isIdentitySharingConfirmed();
        this.areSnapshotsEnabled = game.areSnapshotsEnabled();
        this.getThemeColor = game.getThemeColor();
        this.hasGamepadSupport = game.hasGamepadSupport();
    }

    @Constructor
    GameEntity(@Param(value = 1) String var1, @Param(value = 2) String var2, @Param(value = 3) String var3, @Param(value = 4) String var4, @Param(value = 5) String var5, @Param(value = 6) String var6, @Param(value = 7) Uri var7, @Param(value = 8) Uri var8, @Param(value = 9) Uri var9, @Param(value = 10) boolean var10, @Param(value = 11) boolean var11, @Param(value = 12) String var12, @Param(value = 13) int var13, @Param(value = 14) int var14, @Param(value = 15) int var15, @Param(value = 16) boolean var16, @Param(value = 17) boolean var17, @Param(value = 18) String var18, @Param(value = 19) String var19, @Param(value = 20) String var20, @Param(value = 21) boolean var21, @Param(value = 22) boolean var22, @Param(value = 23) boolean var23, @Param(value = 24) String var24, @Param(value = 25) boolean var25) {
        this.applicationId = var1;
        this.displayName = var2;
        this.primaryCategory = var3;
        this.secondaryCategory = var4;
        this.description = var5;
        this.developerName = var6;
        this.iconImageUri = var7;
        this.iconImageUrl = var18;
        this.hiResImageUri = var8;
        this.hiResImageUrl = var19;
        this.featuredImageUri = var9;
        this.featuredImageUrl = var20;
        this.isPlayEnabledGame = var10;
        this.isInstanceInstalled = var11;
        this.instancePackageName = var12;
        this.gameplayAclStatus = var13;
        this.achievementTotalCount = var14;
        this.leaderboardCount = var15;
        this.isRealTimeMultiplayerEnabled = var16;
        this.isTurnBasedMultiplayerEnabled = var17;
        this.isMuted = var21;
        this.isIdentitySharingConfirmed = var22;
        this.areSnapshotsEnabled = var23;
        this.getThemeColor = var24;
        this.hasGamepadSupport = var25;
    }

    public final String getApplicationId() {
        return this.applicationId;
    }

    public final String getDisplayName() {
        return this.displayName;
    }

    public final void getDisplayName(CharArrayBuffer var1) {
        DataUtils.copyStringToBuffer(this.displayName, var1);
    }

    public final String getPrimaryCategory() {
        return this.primaryCategory;
    }

    public final String getSecondaryCategory() {
        return this.secondaryCategory;
    }

    public final String getDescription() {
        return this.description;
    }

    public final void getDescription(CharArrayBuffer var1) {
        DataUtils.copyStringToBuffer(this.description, var1);
    }

    public final String getDeveloperName() {
        return this.developerName;
    }

    public final void getDeveloperName(CharArrayBuffer var1) {
        DataUtils.copyStringToBuffer(this.developerName, var1);
    }

    public final Uri getIconImageUri() {
        return this.iconImageUri;
    }

    public final String getIconImageUrl() {
        return this.iconImageUrl;
    }

    public final Uri getHiResImageUri() {
        return this.hiResImageUri;
    }

    public final String getHiResImageUrl() {
        return this.hiResImageUrl;
    }

    public final Uri getFeaturedImageUri() {
        return this.featuredImageUri;
    }

    public final String getFeaturedImageUrl() {
        return this.featuredImageUrl;
    }

    public final boolean isMuted() {
        return this.isMuted;
    }

    public final boolean isIdentitySharingConfirmed() {
        return this.isIdentitySharingConfirmed;
    }

    public final boolean isPlayEnabledGame() {
        return this.isPlayEnabledGame;
    }

    public final boolean isInstanceInstalled() {
        return this.isInstanceInstalled;
    }

    public final String getInstancePackageName() {
        return this.instancePackageName;
    }

    public final int getGameplayAclStatus() { return gameplayAclStatus; }

    public final int getAchievementTotalCount() {
        return this.achievementTotalCount;
    }

    public final int getLeaderboardCount() {
        return this.leaderboardCount;
    }

    public final boolean isRealTimeMultiplayerEnabled() {
        return this.isRealTimeMultiplayerEnabled;
    }

    public final boolean isTurnBasedMultiplayerEnabled() {
        return this.isTurnBasedMultiplayerEnabled;
    }

    public final boolean areSnapshotsEnabled() {
        return this.areSnapshotsEnabled;
    }

    public final String getThemeColor() {
        return this.getThemeColor;
    }

    public final boolean hasGamepadSupport() {
        return this.hasGamepadSupport;
    }

    public final Game freeze() {
        return this;
    }

    public final boolean isDataValid() {
        return true;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GameEntity> CREATOR = findCreator(GameEntity.class);
}
