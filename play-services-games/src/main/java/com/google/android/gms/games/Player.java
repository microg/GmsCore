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
import android.os.Parcelable;
import androidx.annotation.IntDef;
import com.google.android.gms.common.data.Freezable;
import com.google.android.gms.common.images.ImageManager;

/**
 * Data interface for retrieving player information.
 */
public interface Player extends Freezable<Player>, Parcelable {
    /**
     * Friends list visibility statuses.
     */
    @IntDef({FriendsListVisibilityStatus.UNKNOWN, FriendsListVisibilityStatus.VISIBLE, FriendsListVisibilityStatus.REQUEST_REQUIRED, FriendsListVisibilityStatus.FEATURE_UNAVAILABLE})
    @interface FriendsListVisibilityStatus {
        /**
         * Constant indicating that currently it's unknown if the friends list is visible to the game, or whether the game can ask for
         * access from the user. Use {@link PlayersClient#getCurrentPlayer(boolean)} to force reload the latest status.
         */
        int UNKNOWN = 0;
        /**
         * Constant indicating that the friends list is currently visible to the game.
         */
        int VISIBLE = 1;
        /**
         * Constant indicating that the friends list is not visible to the game, but the game can ask for access.
         */
        int REQUEST_REQUIRED = 2;
        /**
         * Constant indicating that the friends list is currently unavailable for the game. You cannot request access at this time,
         * either because the user has permanently declined or the friends feature is not available to them. In this state, any
         * attempts to request access to the friends list will be unsuccessful.
         */
        int FEATURE_UNAVAILABLE = 3;
    }

    /**
     * Player friend statuses.
     */
    @IntDef({PlayerFriendStatus.UNKNOWN, PlayerFriendStatus.NO_RELATIONSHIP, PlayerFriendStatus.FRIEND})
    @interface PlayerFriendStatus {
        /**
         * Constant indicating that the currently signed-in player's friend status with this player is unknown. This may happen if the
         * user has not shared the friends list with the game.
         */
        int UNKNOWN = -1;
        /**
         * Constant indicating that the currently signed-in player is not a friend of this player, and there are no pending invitations
         * between them.
         */
        int NO_RELATIONSHIP = 0;
        /**
         * Constant indicating that the currently signed-in player and this player are friends.
         */
        int FRIEND = 4;
    }

    /**
     * Constant indicating that the current XP total for a player is not known.
     */
    long CURRENT_XP_UNKNOWN = -1;
    /**
     * Constant indicating that a timestamp for a player is not known.
     */
    long TIMESTAMP_UNKNOWN = -1;

    /**
     * Retrieves the URI for loading this player's landscape banner image. Returns null if the player has no landscape banner image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return The image URI for the player's landscape banner image, or null if the player has none.
     */
    Uri getBannerImageLandscapeUri();

    /**
     * Retrieves the URI for loading this player's portrait banner image. Returns null if the player has no portrait banner image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return The image URI for the player's portrait banner image, or null if the player has none.
     */
    Uri getBannerImagePortraitUri();

    /**
     * Returns information only available for the signed-in user. The method will return {@code null} for other players.
     */
    CurrentPlayerInfo getCurrentPlayerInfo();

    /**
     * Loads the player's display name into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getDisplayName(CharArrayBuffer dataOut);

    /**
     * Retrieves the display name for this player.
     *
     * @return The player's display name.
     */
    String getDisplayName();

    /**
     * Retrieves the URI for loading this player's hi-res profile image. Returns null if the player has no profile image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return The image URI for the player's hi-res profile image, or null if the player has none.
     */
    Uri getHiResImageUri();

    /**
     * Retrieves the URI for loading this player's icon-size profile image. Returns null if the player has no profile image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return The image URI for the player's icon-size profile image, or null if the player has none.
     */
    Uri getIconImageUri();

    /**
     * Retrieves the timestamp at which this player last played a multiplayer game with the currently signed in user. If the
     * timestamp is not found, this method returns {@link #TIMESTAMP_UNKNOWN}.
     *
     * @return The timestamp (in ms since epoch) at which the player last played a multiplayer game with the currently signed in user.
     * @deprecated Real-time multiplayer and Turn-based multiplayer support is being shut down on March 31, 2020.
     */
    @Deprecated
    long getLastPlayedWithTimestamp();

    /**
     * Retrieves the player level associated information if any exists. If no level information exists for this player, this method will return {@code null}.
     *
     * @return The {@link PlayerLevelInfo} associated with this player, if any.
     */
    PlayerLevelInfo getLevelInfo();

    /**
     * Retrieves the ID of this player.
     *
     * @return The player ID.
     */
    String getPlayerId();

    /**
     * Returns relationship information of this player. If no relationship information exists for this player, this method will return {@code null}.
     */
    PlayerRelationshipInfo getRelationshipInfo();

    /**
     * Retrieves the timestamp at which this player record was last updated locally.
     *
     * @return The timestamp (in ms since epoch) at which the player data was last updated locally.
     */
    long getRetrievedTimestamp();

    /**
     * Retrieves the title of the player. This is based on the player's gameplay activity in apps using Google Play Games
     * services. Note that not all players have titles, and that a player's title may change over time.
     *
     * @return The player's title, or {@code null} if this player has no title.
     */
    String getTitle();

    /**
     * Loads the player's title into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getTitle(CharArrayBuffer dataOut);

    /**
     * Indicates whether this player has a hi-res profile image to display.
     *
     * @return Whether the player has a hi-res profile image to display.
     */
    boolean hasHiResImage();

    /**
     * Indicates whether this player has an icon-size profile image to display.
     *
     * @return Whether the player has an icon-size profile image to display.
     */
    boolean hasIconImage();
}
