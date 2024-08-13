/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.leaderboard;

import android.database.CharArrayBuffer;
import android.net.Uri;

import com.google.android.gms.common.data.Freezable;
import com.google.android.gms.common.images.ImageManager;

import java.util.ArrayList;

/**
 * Data interface for leaderboard metadata.
 */
public interface Leaderboard extends Freezable<Leaderboard> {

    /**
     * Score order constant for leaderboards where scores are sorted in descending order.
     */
    int SCORE_ORDER_LARGER_IS_BETTER = 1;

    /**
     * Score order constant for leaderboards where scores are sorted in ascending order.
     */
    int SCORE_ORDER_SMALLER_IS_BETTER = 0;

    /**
     * Retrieves the display name of this leaderboard.
     *
     * @return Display name of this leaderboard.
     */
    String getDisplayName();

    /**
     * Loads this leaderboard's display name into the given {@link CharArrayBuffer}.
     *
     * @param dataOut The buffer to load the data into.
     */
    void getDisplayName(CharArrayBuffer dataOut);

    /**
     * Retrieves an image URI that can be used to load this leaderboard's icon, or null if there was a problem retrieving the icon.
     * <p>
     * To retrieve the Image from the {@link Uri}, use {@link ImageManager}.
     *
     * @return A URI that can be used to load this leaderboard's icon, or null if there was a problem retrieving the icon.
     */
    Uri getIconImageUri();

    /**
     * Retrieves the ID of this leaderboard.
     *
     * @return The ID of this leaderboard.
     */
    String getLeaderboardId();

    /**
     * Retrieves the sort order of scores for this leaderboard.
     * Possible values are {@link Leaderboard#SCORE_ORDER_LARGER_IS_BETTER} or {@link Leaderboard#SCORE_ORDER_SMALLER_IS_BETTER} .
     *
     * @return The score order used by this leaderboard.
     */
    int getScoreOrder();

    /**
     * Retrieves the {@link LeaderboardVariant}s for this leaderboard. These will be returned sorted by time span first, then by variant type.
     * <p>
     * Note that these variants are volatile, and are tied to the lifetime of the original buffer.
     *
     * @return A list containing the {@link LeaderboardVariant}s for this leaderboard.
     */
    ArrayList<LeaderboardVariant> getVariants();
}
