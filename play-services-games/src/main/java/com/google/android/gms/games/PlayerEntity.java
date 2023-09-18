/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.games;

import android.database.CharArrayBuffer;
import android.net.Uri;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.games.internal.player.MostRecentGameInfoEntity;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Data object representing a set of Player data. This is immutable, and therefore safe to cache or store. Note, however, that
 * the data it represents may grow stale.
 * <p>
 * This class exists solely to support parceling these objects and should not be used directly.
 */
public class PlayerEntity extends AutoSafeParcelable implements Player {
    @Field(1)
    private String playerId;
    @Field(2)
    private String displayName;
    @Field(3)
    private Uri iconImageUri;
    @Field(4)
    private Uri hiResImageUri;
    @Field(5)
    private long retrievedTimestamp;
    @Field(6)
    private int isInCircles;
    @Field(7)
    private long lastPlayedWithTimestamp;
    @Field(8)
    private String iconImageUrl;
    @Field(9)
    private String hiResImageUrl;
    @Field(14)
    private String title;
    @Field(15)
    private MostRecentGameInfoEntity mostRecentGameInfo;
    @Field(16)
    private PlayerLevelInfo levelInfo;
    @Field(18)
    private boolean profileVisible;
    @Field(19)
    private boolean hasDebugAccess;
    @Field(20)
    private String gamerTag;
    @Field(21)
    private String name;
    @Field(22)
    private Uri bannerImageLandscapeUri;
    @Field(23)
    private String bannerImageLandscapeUrl;
    @Field(24)
    private Uri bannerImagePortraitUri;
    @Field(25)
    private String bannerImagePortraitUrl;
    @Field(29)
    private long totalUnlockedAchievement = -1;
    @Field(33)
    private PlayerRelationshipInfoEntity relationshipInfo;
    @Field(35)
    private CurrentPlayerInfoEntity currentPlayerInfo;
    @Field(36)
    private boolean alwaysAutoSignIn;
    @Field(37)
    private String gamePlayerId;

    @Hide
    public PlayerEntity() {
    }

    @Hide
    public PlayerEntity(Player copy) {
        bannerImageLandscapeUri = copy.getBannerImageLandscapeUri();
        bannerImagePortraitUri = copy.getBannerImagePortraitUri();
        currentPlayerInfo = new CurrentPlayerInfoEntity(copy.getCurrentPlayerInfo());
        displayName = copy.getDisplayName();
        hiResImageUri = copy.getHiResImageUri();
        iconImageUri = copy.getIconImageUri();
        lastPlayedWithTimestamp = copy.getLastPlayedWithTimestamp();
        levelInfo = copy.getLevelInfo();
        playerId = copy.getPlayerId();
        relationshipInfo = new PlayerRelationshipInfoEntity(copy.getRelationshipInfo());
        retrievedTimestamp = copy.getRetrievedTimestamp();
        title = copy.getTitle();
    }

    @Hide
    public PlayerEntity(String playerId, String displayName, Uri iconImageUri, Uri hiResImageUri, long retrievedTimestamp, int isInCircles, long lastPlayedWithTimestamp, String iconImageUrl, String hiResImageUrl, String title, MostRecentGameInfoEntity mostRecentGameInfo, PlayerLevelInfo levelInfo, boolean profileVisible, boolean hasDebugAccess, String gamerTag, String name, Uri bannerImageLandscapeUri, String bannerImageLandscapeUrl, Uri bannerImagePortraitUri, String bannerImagePortraitUrl, long totalUnlockedAchievement, PlayerRelationshipInfoEntity relationshipInfo, CurrentPlayerInfoEntity currentPlayerInfo, boolean alwaysAutoSignIn, String gamePlayerId) {
        this.playerId = playerId;
        this.displayName = displayName;
        this.iconImageUri = iconImageUri;
        this.hiResImageUri = hiResImageUri;
        this.retrievedTimestamp = retrievedTimestamp;
        this.isInCircles = isInCircles;
        this.lastPlayedWithTimestamp = lastPlayedWithTimestamp;
        this.iconImageUrl = iconImageUrl;
        this.hiResImageUrl = hiResImageUrl;
        this.title = title;
        this.mostRecentGameInfo = mostRecentGameInfo;
        this.levelInfo = levelInfo;
        this.profileVisible = profileVisible;
        this.hasDebugAccess = hasDebugAccess;
        this.gamerTag = gamerTag;
        this.name = name;
        this.bannerImageLandscapeUri = bannerImageLandscapeUri;
        this.bannerImageLandscapeUrl = bannerImageLandscapeUrl;
        this.bannerImagePortraitUri = bannerImagePortraitUri;
        this.bannerImagePortraitUrl = bannerImagePortraitUrl;
        this.totalUnlockedAchievement = totalUnlockedAchievement;
        this.relationshipInfo = relationshipInfo;
        this.currentPlayerInfo = currentPlayerInfo;
        this.alwaysAutoSignIn = alwaysAutoSignIn;
        this.gamePlayerId = gamePlayerId;
    }

    @Override
    public Uri getBannerImageLandscapeUri() {
        return bannerImageLandscapeUri;
    }

    @Override
    public Uri getBannerImagePortraitUri() {
        return bannerImagePortraitUri;
    }

    @Override
    public CurrentPlayerInfo getCurrentPlayerInfo() {
        return currentPlayerInfo;
    }

    @Override
    public void getDisplayName(CharArrayBuffer dataOut) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Uri getHiResImageUri() {
        return hiResImageUri;
    }

    @Override
    public Uri getIconImageUri() {
        return iconImageUri;
    }

    @Override
    public long getLastPlayedWithTimestamp() {
        return lastPlayedWithTimestamp;
    }

    @Override
    public PlayerLevelInfo getLevelInfo() {
        return levelInfo;
    }

    @Override
    public String getPlayerId() {
        return playerId;
    }

    @Override
    public PlayerRelationshipInfo getRelationshipInfo() {
        return relationshipInfo;
    }

    @Override
    public long getRetrievedTimestamp() {
        return retrievedTimestamp;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void getTitle(CharArrayBuffer dataOut) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasHiResImage() {
        return hiResImageUri != null;
    }

    @Override
    public boolean hasIconImage() {
        return iconImageUri != null;
    }

    @Override
    public boolean isDataValid() {
        return true;
    }

    @Override
    public Player freeze() {
        return this;
    }

    @Hide
    public String getGamerTag() {
        return gamerTag;
    }

    @Hide
    public String getName() {
        return name;
    }

    @Hide
    public String getIconImageUrl() {
        return iconImageUrl;
    }

    @Hide
    public String getHiResImageUrl() {
        return hiResImageUrl;
    }

    @Hide
    public String getBannerImageLandscapeUrl() {
        return bannerImageLandscapeUrl;
    }

    @Hide
    public String getBannerImagePortraitUrl() {
        return bannerImagePortraitUrl;
    }

    @Hide
    public int getIsInCircles() {
        return isInCircles;
    }

    @Hide
    public boolean isProfileVisible() {
        return profileVisible;
    }

    @Hide
    public boolean getHasDebugAccess() {
        return hasDebugAccess;
    }

    @Hide
    public long getTotalUnlockedAchievement() {
        return totalUnlockedAchievement;
    }

    @Hide
    public boolean isAlwaysAutoSignIn() {
        return alwaysAutoSignIn;
    }

    @Hide
    public MostRecentGameInfoEntity getMostRecentGameInfo() {
        return mostRecentGameInfo;
    }

    public static final SafeParcelableCreatorAndWriter<PlayerEntity> CREATOR = findCreator(PlayerEntity.class);
}
