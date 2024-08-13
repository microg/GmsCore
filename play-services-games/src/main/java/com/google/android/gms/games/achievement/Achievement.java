/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.achievement;

import android.database.CharArrayBuffer;
import android.net.Uri;
import android.os.Parcelable;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.google.android.gms.common.data.Freezable;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.Player;

/**
 * Data interface for retrieving achievement information.
 */
public interface Achievement extends Freezable<Achievement>, Parcelable {

    @IntDef({AchievementState.STATE_UNLOCKED, AchievementState.STATE_REVEALED, AchievementState.STATE_HIDDEN})
    @interface AchievementState {

        /**
         * Constant indicating an unlocked achievement.
         */
        int STATE_UNLOCKED = 0;

        /**
         * Constant indicating a revealed achievement.
         */
        int STATE_REVEALED = 1;

        /**
         * Constant indicating a hidden achievement.
         */
        int STATE_HIDDEN = 2;
    }

    @IntDef({AchievementType.TYPE_STANDARD, AchievementType.TYPE_INCREMENTAL})
    @interface AchievementType {

        /**
         * Constant indicating a standard achievement.
         */
        int TYPE_STANDARD = 0;

        /**
         * Constant indicating an incremental achievement.
         */
        int TYPE_INCREMENTAL = 1;
    }

    /**
     * Retrieves the ID of this achievement.
     *
     * @return The achievement ID.
     */
    String getAchievementId();

    /**
     * Retrieves the number of steps this user has gone toward unlocking this achievement;
     * only applicable for {@link AchievementType#TYPE_INCREMENTAL} achievement types.
     *
     * @return The number of steps this user has gone toward unlocking this achievement.
     */
    int getCurrentSteps();

    /**
     * Retrieves the description for this achievement.
     *
     * @return The achievement description.
     */
    String getDescription();

    /**
     * Loads the achievement description into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getDescription(CharArrayBuffer dataOut);

    /**
     * Retrieves the number of steps this user has gone toward unlocking this achievement (formatted for the user's locale) into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getFormattedCurrentSteps(CharArrayBuffer dataOut);

    /**
     * Retrieves the number of steps this user has gone toward unlocking this achievement (formatted for the user's locale);
     * only applicable for {@link AchievementType#TYPE_INCREMENTAL} achievement types.
     *
     * @return The formatted number of steps this user has gone toward unlocking this achievement or null if this information is unavailable.
     */
    String getFormattedCurrentSteps();

    /**
     * Loads the total number of steps necessary to unlock this achievement (formatted for the user's locale) into the given CharArrayBuffer;
     * only applicable for {@link AchievementType#TYPE_INCREMENTAL} achievement types.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getFormattedTotalSteps(CharArrayBuffer dataOut);

    /**
     * Retrieves the total number of steps necessary to unlock this achievement, formatted for the user's locale;
     * only applicable for {@link AchievementType#TYPE_INCREMENTAL} achievement types.
     *
     * @return The total number of steps necessary to unlock this achievement or null if this information is unavailable.
     */
    String getFormattedTotalSteps();

    /**
     * Retrieves the timestamp (in millseconds since epoch) at which this achievement was last updated.
     * If the achievement has never been updated, this will return -1.
     *
     * @return Timestamp at which this achievement was last updated.
     */
    long getLastUpdatedTimestamp();

    /**
     * Loads the achievement name into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getName(CharArrayBuffer dataOut);

    /**
     * Retrieves the name of this achievement.
     *
     * @return The achievement name.
     */
    String getName();

    /**
     * Retrieves the player information associated with this achievement.
     * <p>
     * Note that this object is a volatile representation, so it is not safe to cache the output of this directly.
     * Instead, cache the result of {@link Freezable#freeze()}.
     *
     * @return The player associated with this achievement.
     */
    Player getPlayer();

    @Nullable
    Player getPlayerInternal();

    /**
     * Retrieves a URI that can be used to load the achievement's revealed image icon. Returns null if the achievement has no revealed image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return The image URI for the achievement's revealed image icon, or null if the achievement has no revealed image.
     */
    Uri getRevealedImageUri();

    @Deprecated
    @Nullable
    String getRevealedImageUrl();

    /**
     * Returns the {@link AchievementState} of the achievement.
     */
    int getState();

    /**
     * Retrieves the total number of steps necessary to unlock this achievement;
     * only applicable for {@link AchievementType#TYPE_INCREMENTAL} achievement types.
     *
     * @return The total number of steps necessary to unlock this achievement.
     */
    int getTotalSteps();

    /**
     * Returns the {@link Achievement.AchievementType} of this achievement.
     */
    int getType();

    /**
     * Retrieves a URI that can be used to load the achievement's unlocked image icon. Returns null if the achievement has no unlocked image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return The image URI for the achievement's unlocked image icon, or null if the achievement has no unlocked image.
     */
    Uri getUnlockedImageUri();

    @Deprecated
    @Nullable
    String getUnlockedImageUrl();

    /**
     * Retrieves the XP value of this achievement.
     *
     * @return XP value given to players for unlocking this achievement.
     */
    long getXpValue();

    float getRarityPercent();

    String getApplicationId();
}
