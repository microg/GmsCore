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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.data.Freezable;
import com.google.android.gms.common.images.ImageManager;
import org.microg.gms.common.Hide;

/**
 * Data interface for retrieving game information.
 */
public interface Game extends Freezable<Game> {
    /**
     * Indicates whether or not this game supports snapshots.
     *
     * @return Whether or not this game supports snapshots.
     */
    boolean areSnapshotsEnabled();

    /**
     * Retrieves the number of achievements registered for this game.
     *
     * @return The number of achievements registered for this game.
     */
    int getAchievementTotalCount();

    /**
     * Retrieves the application ID for this game.
     *
     * @return The application ID for this game.
     */
    @NonNull
    String getApplicationId();

    /**
     * Retrieves the description of this game.
     *
     * @return The description of this game.
     */
    @NonNull
    String getDescription();

    /**
     * Loads the description string into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getDescription(@NonNull CharArrayBuffer dataOut);

    /**
     * Retrieves the name of the developer of this game.
     *
     * @return The name of the developer of this game.
     */
    @NonNull
    String getDeveloperName();

    /**
     * Loads the developer name into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getDeveloperName(@NonNull CharArrayBuffer dataOut);

    /**
     * Retrieves the display name for this game.
     *
     * @return The display name for this game.
     */
    @NonNull
    String getDisplayName();

    /**
     * Loads the display name string into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getDisplayName(@NonNull CharArrayBuffer dataOut);

    /**
     * Retrieves an image URI that can be used to load the game's featured (banner) image from Google Play. Returns null if game has no featured image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return A URI that can be used to load the game's featured image, or null if the game has no featured image.
     */
    @Nullable
    Uri getFeaturedImageUri();

    @Hide
    @Deprecated
    String getFeaturedImageUrl();

    /**
     * Retrieves an image URI that can be used to load the game's hi-res image. Returns null if game has no hi-res image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return A URI that can be used to load the game's hi-res image, or null if the game has no hi-res image.
     */
    @Nullable
    Uri getHiResImageUri();

    @Hide
    @Deprecated
    String getHiResImageUrl();

    /**
     * Retrieves an image URI that can be used to load the game's icon. Returns null if game has no icon.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return A URI that can be used to load the game's icon, or null if the game has no icon.
     */
    @Nullable
    Uri getIconImageUri();

    @Hide
    @Deprecated
    String getIconImageUrl();

    /**
     * Gets the number of leaderboards registered for this game.
     *
     * @return The number of leaderboards registered for this game.
     */
    int getLeaderboardCount();

    /**
     * Retrieves the primary category of the game - this may be null.
     *
     * @return The primary category of the game.
     */
    @Nullable
    String getPrimaryCategory();

    /**
     * Retrieves the secondary category of the game - this may be null.
     *
     * @return The secondary category of the game, or null if not provided.
     */
    @Nullable
    String getSecondaryCategory();

    /**
     * Retrieves the theme color for this game. The theme color is used to configure the appearance of Play Games UIs.
     *
     * @return The color to use as an RGB hex triplet, e.g. "E0E0E0"
     */
    @NonNull
    String getThemeColor();

    /**
     * Indicates whether or not this game is marked as supporting gamepads.
     *
     * @return Whether or not this game declares gamepad support.
     */
    boolean hasGamepadSupport();
}
