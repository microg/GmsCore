/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.leaderboard;

import android.database.CharArrayBuffer;
import android.net.Uri;

import com.google.android.gms.common.data.Freezable;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.Player;

/**
 * Data interface representing a single score on a leaderboard.
 */
public interface LeaderboardScore extends Freezable<LeaderboardScore> {

    /**
     * Constant indicating that the score holder's rank was not known.
     */
    int LEADERBOARD_RANK_UNKNOWN = -1;

    /**
     * Load the formatted display rank into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getDisplayRank(CharArrayBuffer dataOut);

    /**
     * Retrieves a formatted string to display for this rank. This handles appropriate localization and formatting.
     *
     * @return Formatted string to display.
     */
    String getDisplayRank();

    /**
     * Retrieves a formatted string to display for this score.
     * The details of the formatting are specified by the developer in their dev console.
     *
     * @return Formatted string to display.
     */
    String getDisplayScore();

    /**
     * Loads the formatted display score into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getDisplayScore(CharArrayBuffer dataOut);

    /**
     * Retrieves the rank returned from the server for this score.
     * Note that this may not be exact and that multiple scores can have identical ranks.
     * Lower ranks indicate a better score, with rank 1 being the best score on the board.
     * <p>
     * If the score holder's rank cannot be determined, this will return {@link LeaderboardScore#LEADERBOARD_RANK_UNKNOWN}.
     *
     * @return Rank of score.
     */
    long getRank();

    /**
     * Retrieves the raw score value.
     *
     * @return The raw score value.
     */
    long getRawScore();

    /**
     * Retrieves the player that scored this particular score.
     * The return value here may be null if the current player is not authorized to see information about the holder of this score.
     * <p>
     * Note that this object is a volatile representation, so it is not safe to cache the output of this directly.
     * Instead, cache the result of {@link Freezable#freeze()}.
     *
     * @return The player associated with this leaderboard score.
     */
    Player getScoreHolder();

    /**
     * Load the display name of the player who scored this score into the provided {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getScoreHolderDisplayName(CharArrayBuffer dataOut);

    /**
     * Retrieves the name to display for the player who scored this score.
     * If the identity of the player is unknown, this will return an anonymous name to display.
     *
     * @return The display name of the holder of this score.
     */
    String getScoreHolderDisplayName();

    /**
     * Retrieves the URI of the hi-res image to display for the player who scored this score.
     * If the identity of the player is unknown, this will return null. It may also be null if the player simply has no image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return The URI of the hi-res image to display for this score.
     */
    Uri getScoreHolderHiResImageUri();

    /**
     * Retrieves the URI of the icon image to display for the player who scored this score.
     * If the identity of the player is unknown, this will return an anonymous image for the player.
     * It may also be null if the player simply has no image.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return The URI of the icon image to display for this score.
     */
    Uri getScoreHolderIconImageUri();

    /**
     * Retrieve the optional score tag associated with this score, if any.
     *
     * @return The score tag associated with this score.
     */
    String getScoreTag();

    /**
     * Retrieves the timestamp (in milliseconds from epoch) at which this score was achieved.
     *
     * @return Timestamp when this score was achieved.
     */
    long getTimestampMillis();
}
