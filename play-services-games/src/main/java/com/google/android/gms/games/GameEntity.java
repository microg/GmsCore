/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.games;

import android.database.CharArrayBuffer;
import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

/**
 * Data object representing a set of Game data. This is immutable, and therefore safe to cache or store. Note, however, that the data it
 * represents may grow stale.
 * <p>
 * This class exists solely to support parceling these objects and should not be used directly.
 */
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

    @Hide
    @Constructor
    public GameEntity(@Param(value = 1) String applicationId, @Param(value = 2) String displayName, @Param(value = 3) String primaryCategory, @Param(value = 4) String secondaryCategory, @Param(value = 5) String description, @Param(value = 6) String developerName, @Param(value = 7) Uri iconImageUri, @Param(value = 8) Uri hiResImageUri, @Param(value = 9) Uri featuredImageUri, @Param(value = 10) boolean isPlayEnabledGame, @Param(value = 11) boolean isInstanceInstalled, @Param(value = 12) String instancePackageName, @Param(value = 13) int gameplayAclStatus, @Param(value = 14) int achievementTotalCount, @Param(value = 15) int leaderboardCount, @Param(value = 16) boolean isRealTimeMultiplayerEnabled, @Param(value = 17) boolean isTurnBasedMultiplayerEnabled, @Param(value = 18) String iconImageUrl, @Param(value = 19) String hiResImageUrl, @Param(value = 20) String featuredImageUrl, @Param(value = 21) boolean isMuted, @Param(value = 22) boolean isIdentitySharingConfirmed, @Param(value = 23) boolean areSnapshotsEnabled, @Param(value = 24) String getThemeColor, @Param(value = 25) boolean hasGamepadSupport) {
        this.applicationId = applicationId;
        this.displayName = displayName;
        this.primaryCategory = primaryCategory;
        this.secondaryCategory = secondaryCategory;
        this.description = description;
        this.developerName = developerName;
        this.iconImageUri = iconImageUri;
        this.iconImageUrl = iconImageUrl;
        this.hiResImageUri = hiResImageUri;
        this.hiResImageUrl = hiResImageUrl;
        this.featuredImageUri = featuredImageUri;
        this.featuredImageUrl = featuredImageUrl;
        this.isPlayEnabledGame = isPlayEnabledGame;
        this.isInstanceInstalled = isInstanceInstalled;
        this.instancePackageName = instancePackageName;
        this.gameplayAclStatus = gameplayAclStatus;
        this.achievementTotalCount = achievementTotalCount;
        this.leaderboardCount = leaderboardCount;
        this.isRealTimeMultiplayerEnabled = isRealTimeMultiplayerEnabled;
        this.isTurnBasedMultiplayerEnabled = isTurnBasedMultiplayerEnabled;
        this.isMuted = isMuted;
        this.isIdentitySharingConfirmed = isIdentitySharingConfirmed;
        this.areSnapshotsEnabled = areSnapshotsEnabled;
        this.getThemeColor = getThemeColor;
        this.hasGamepadSupport = hasGamepadSupport;
    }

    /**
     * Indicates whether or not this game supports snapshots.
     *
     * @return Whether or not this game supports snapshots.
     */
    @Override
    public boolean areSnapshotsEnabled() {
        return this.areSnapshotsEnabled;
    }

    @Override
    public Game freeze() {
        return this;
    }

    /**
     * Retrieves the number of achievements registered for this game.
     *
     * @return The number of achievements registered for this game.
     */
    @Override
    public int getAchievementTotalCount() {
        return this.achievementTotalCount;
    }

    /**
     * Retrieves the application ID for this game.
     *
     * @return The application ID for this game.
     */
    @Override
    @NonNull
    public String getApplicationId() {
        return this.applicationId;
    }

    /**
     * Retrieves the description of this game.
     *
     * @return The description of this game.
     */
    @Override
    @NonNull
    public String getDescription() {
        return this.description;
    }

    /**
     * Loads the description string into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    @Override
    public void getDescription(@NonNull CharArrayBuffer dataOut) {
        copyStringToBuffer(this.description, dataOut);
    }

    /**
     * Retrieves the name of the developer of this game.
     *
     * @return The name of the developer of this game.
     */
    @Override
    @NonNull
    public String getDeveloperName() {
        return this.developerName;
    }

    /**
     * Loads the developer name into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    @Override
    public void getDeveloperName(@NonNull CharArrayBuffer dataOut) {
        copyStringToBuffer(this.developerName, dataOut);
    }

    /**
     * Retrieves the display name for this game.
     *
     * @return The display name for this game.
     */
    @Override
    @NonNull
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Loads the display name string into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    @Override
    public void getDisplayName(@NonNull CharArrayBuffer dataOut) {
        copyStringToBuffer(this.displayName, dataOut);
    }

    /**
     * Retrieves an image URI that can be used to load the game's featured (banner) image from Google Play. Returns null if game has no featured image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return A URI that can be used to load the game's featured image, or null if the game has no featured image.
     */
    @Override
    @Nullable
    public Uri getFeaturedImageUri() {
        return this.featuredImageUri;
    }

    @Override
    @Hide
    @Deprecated
    public String getFeaturedImageUrl() {
        return this.featuredImageUrl;
    }

    /**
     * Retrieves an image URI that can be used to load the game's hi-res image. Returns null if game has no hi-res image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return A URI that can be used to load the game's hi-res image, or null if the game has no hi-res image.
     */
    @Override
    @Nullable
    public Uri getHiResImageUri() {
        return this.hiResImageUri;
    }

    @Override
    @Hide
    @Deprecated
    public String getHiResImageUrl() {
        return this.hiResImageUrl;
    }

    /**
     * Retrieves an image URI that can be used to load the game's icon. Returns null if game has no icon.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return A URI that can be used to load the game's icon, or null if the game has no icon.
     */
    @Override
    public Uri getIconImageUri() {
        return this.iconImageUri;
    }

    @Override
    @Hide
    @Deprecated
    public String getIconImageUrl() {
        return this.iconImageUrl;
    }

    /**
     * Gets the number of leaderboards registered for this game.
     *
     * @return The number of leaderboards registered for this game.
     */
    @Override
    public int getLeaderboardCount() {
        return this.leaderboardCount;
    }

    /**
     * Retrieves the primary category of the game - this is may be null.
     *
     * @return The primary category of the game.
     */
    @Override
    @Nullable
    public String getPrimaryCategory() {
        return this.primaryCategory;
    }

    /**
     * Retrieves the secondary category of the game - this may be null.
     *
     * @return The secondary category of the game, or null if not provided.
     */
    @Override
    @Nullable
    public String getSecondaryCategory() {
        return this.secondaryCategory;
    }

    /**
     * Retrieves the theme color for this game. The theme color is used to configure the appearance of Play Games UIs.
     *
     * @return The color to use as an RGB hex triplet, e.g. "E0E0E0"
     */
    @Override
    @NonNull
    public String getThemeColor() {
        return this.getThemeColor;
    }

    /**
     * Indicates whether or not this game is marked as supporting gamepads.
     *
     * @return Whether or not this game declares gamepad support.
     */
    @Override
    public boolean hasGamepadSupport() {
        return this.hasGamepadSupport;
    }

    @Override
    public boolean isDataValid() {
        return true;
    }

    int getGameplayAclStatus() {
        return gameplayAclStatus;
    }

    String getInstancePackageName() {
        return this.instancePackageName;
    }

    boolean isMuted() {
        return this.isMuted;
    }

    boolean isIdentitySharingConfirmed() {
        return this.isIdentitySharingConfirmed;
    }

    boolean isPlayEnabledGame() {
        return this.isPlayEnabledGame;
    }

    boolean isInstanceInstalled() {
        return this.isInstanceInstalled;
    }

    @Hide
    boolean isRealTimeMultiplayerEnabled() {
        return this.isRealTimeMultiplayerEnabled;
    }

    @Hide
    boolean isTurnBasedMultiplayerEnabled() {
        return this.isTurnBasedMultiplayerEnabled;
    }

    private static void copyStringToBuffer(@Nullable String toCopy, @NonNull CharArrayBuffer dataOut) {
        if (toCopy == null || toCopy.isEmpty()) {
            dataOut.sizeCopied = 0;
            return;
        }
        if (dataOut.data == null || dataOut.data.length < toCopy.length()) {
            dataOut.data = toCopy.toCharArray();
        } else {
            toCopy.getChars(0, toCopy.length(), dataOut.data, 0);
        }
        dataOut.sizeCopied = toCopy.length();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GameEntity> CREATOR = findCreator(GameEntity.class);
}
