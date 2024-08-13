/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.Releasable;
import com.google.android.gms.games.leaderboard.Leaderboard;
import com.google.android.gms.games.leaderboard.LeaderboardBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer;
import com.google.android.gms.tasks.Task;

public interface LeaderboardsClient {

    /**
     * Returns a Task which asynchronously loads an Intent to show the list of leaderboards for a game.
     * Note that this must be invoked with Activity.startActivityForResult(Intent, int), so that the identity of the calling package can be established.
     */
    Task<Intent> getAllLeaderboardsIntent();

    /**
     * Returns a Task which asynchronously loads an Intent to show a leaderboard for a game specified by a leaderboardId.
     * Note that the Intent returned from the Task must be invoked with Activity.startActivityForResult(Intent, int), so that the identity of the calling package can be established.
     *
     * @param leaderboardId The ID of the leaderboard to view.
     * @param timeSpan      Time span to retrieve data for. Valid values are LeaderboardVariant.TIME_SPAN_DAILY, LeaderboardVariant.TIME_SPAN_WEEKLY, or LeaderboardVariant.TIME_SPAN_ALL_TIME.
     */
    Task<Intent> getLeaderboardIntent(String leaderboardId, int timeSpan);

    /**
     * Returns a Task which asynchronously loads an Intent to show a leaderboard for a game specified by a leaderboardId.
     * Note that the Intent returned from the Task must be invoked with Activity.startActivityForResult(Intent, int), so that the identity of the calling package can be established.
     *
     * @param leaderboardId The ID of the leaderboard to view.
     */
    Task<Intent> getLeaderboardIntent(String leaderboardId);

    /**
     * Returns a Task which asynchronously loads an Intent to show a leaderboard for a game specified by a leaderboardId.
     * Note that the Intent returned from the Task must be invoked with Activity.startActivityForResult(Intent, int), so that the identity of the calling package can be established.
     *
     * @param leaderboardId The ID of the leaderboard to view.
     * @param timeSpan      Time span to retrieve data for. Valid values are LeaderboardVariant.TIME_SPAN_DAILY, LeaderboardVariant.TIME_SPAN_WEEKLY, or LeaderboardVariant.TIME_SPAN_ALL_TIME.
     * @param collection    The collection to show by default. Valid values are LeaderboardVariant.COLLECTION_PUBLIC or LeaderboardVariant.COLLECTION_FRIENDS.
     */
    Task<Intent> getLeaderboardIntent(String leaderboardId, int timeSpan, int collection);

    /**
     * Returns a Task which asynchronously loads an annotated LeaderboardScore that represents the signed-in player's score for the leaderboard specified by leaderboardId.
     * <p>
     * For LeaderboardVariant.COLLECTION_FRIENDS, this call will fail with FriendsResolutionRequiredException if the user has not granted the game access to their friends list. The exception result can be used to ask for consent.
     *
     * @param leaderboardId         ID of the leaderboard to load the score from.
     * @param span                  Time span to retrieve data for. Valid values are LeaderboardVariant.TIME_SPAN_DAILY, LeaderboardVariant.TIME_SPAN_WEEKLY, or LeaderboardVariant.TIME_SPAN_ALL_TIME.
     * @param leaderboardCollection The leaderboard collection to retrieve scores for. Valid values are either LeaderboardVariant.COLLECTION_PUBLIC or LeaderboardVariant.COLLECTION_FRIENDS.
     */
    Task<AnnotatedData<LeaderboardScore>> loadCurrentPlayerLeaderboardScore(String leaderboardId, int span, int leaderboardCollection);

    /**
     * Returns a Task which asynchronously loads an annotated LeaderboardBuffer that represents a list of leaderboards metadata for this game.
     *
     * @param forceReload If true, this call will clear any locally cached data and attempt to fetch the latest data from the server.
     *                    This would commonly be used for something like a user-initiated refresh.
     *                    Normally, this should be set to false to gain advantages of data caching.
     */
    Task<AnnotatedData<LeaderboardBuffer>> loadLeaderboardMetadata(boolean forceReload);

    /**
     * Returns a Task which asynchronously loads an annotated Leaderboard specified by leaderboardId.
     *
     * @param leaderboardId ID of the leaderboard to load metadata for.
     * @param forceReload   If true, this call will clear any locally cached data and attempt to fetch the latest data from the server.
     *                      This would commonly be used for something like a user-initiated refresh.
     *                      Normally, this should be set to false to gain advantages of data caching.
     */
    Task<AnnotatedData<Leaderboard>> loadLeaderboardMetadata(String leaderboardId, boolean forceReload);

    /**
     * Returns a Task which asynchronously loads an annotated LeaderboardsClient.LeaderboardScores that represents an additional page of score data for the given score buffer.
     * A new score buffer will be delivered that replaces the given buffer.
     * <p>
     * LeaderboardsClient.LeaderboardScores.release() should be called to release resources after usage.
     * <p>
     * For LeaderboardVariant.COLLECTION_FRIENDS, this call will fail with FriendsResolutionRequiredException if the user has not granted the game access to their friends list.
     * The exception result can be used to ask for consent.
     *
     * @param buffer        The existing buffer that will be expanded. The buffer is allowed to be closed prior to being passed in to this method.
     * @param maxResults    The maximum number of scores to fetch per page. Must be between 1 and 25. Note that the number of scores returned here may be greater than this value, depending on how much data is cached on the device.
     * @param pageDirection The direction to expand the buffer. Values are defined in PageDirection.
     */
    Task<AnnotatedData<LeaderboardsClient.LeaderboardScores>> loadMoreScores(LeaderboardScoreBuffer buffer, int maxResults, int pageDirection);

    /**
     * Returns a Task which asynchronously loads an annotated LeaderboardsClient.LeaderboardScores that represents the player-centered page of scores for the leaderboard specified by leaderboardId.
     * If the player does not have a score on this leaderboard, this call will return the top page instead.
     * <p>
     * LeaderboardsClient.LeaderboardScores.release() should be called to release resources after usage.
     * <p>
     * For LeaderboardVariant.COLLECTION_FRIENDS, this call will fail with FriendsResolutionRequiredException if the user has not granted the game access to their friends list. The exception result can be used to ask for consent.
     *
     * @param leaderboardId         ID of the leaderboard.
     * @param span                  Time span to retrieve data for. Valid values are LeaderboardVariant.TIME_SPAN_DAILY, LeaderboardVariant.TIME_SPAN_WEEKLY, or LeaderboardVariant.TIME_SPAN_ALL_TIME.
     * @param leaderboardCollection The leaderboard collection to retrieve scores for. Valid values are either LeaderboardVariant.COLLECTION_PUBLIC or LeaderboardVariant.COLLECTION_FRIENDS.
     * @param maxResults            The maximum number of scores to fetch per page. Must be between 1 and 25.
     * @param forceReload           If true, this call will clear any locally cached data and attempt to fetch the latest data from the server. This would commonly be used for something like a user-initiated refresh. Normally, this should be set to false to gain advantages of data caching.
     */
    Task<AnnotatedData<LeaderboardsClient.LeaderboardScores>> loadPlayerCenteredScores(String leaderboardId, int span, int leaderboardCollection, int maxResults, boolean forceReload);

    /**
     * Returns a Task which asynchronously loads an annotated LeaderboardsClient.LeaderboardScores that represents the player-centered page of scores for the leaderboard specified by leaderboardId.
     * If the player does not have a score on this leaderboard, this call will return the top page instead.
     * <p>
     * LeaderboardsClient.LeaderboardScores.release() should be called to release resources after usage.
     * <p>
     * For LeaderboardVariant.COLLECTION_FRIENDS, this call will fail with FriendsResolutionRequiredException if the user has not granted the game access to their friends list. The exception result can be used to ask for consent.
     *
     * @param leaderboardId         ID of the leaderboard.
     * @param span                  Time span to retrieve data for. Valid values are LeaderboardVariant.TIME_SPAN_DAILY, LeaderboardVariant.TIME_SPAN_WEEKLY, or LeaderboardVariant.TIME_SPAN_ALL_TIME.
     * @param leaderboardCollection The leaderboard collection to retrieve scores for. Valid values are either LeaderboardVariant.COLLECTION_PUBLIC or LeaderboardVariant.COLLECTION_FRIENDS.
     * @param maxResults            The maximum number of scores to fetch per page. Must be between 1 and 25.
     */
    Task<AnnotatedData<LeaderboardsClient.LeaderboardScores>> loadPlayerCenteredScores(String leaderboardId, int span, int leaderboardCollection, int maxResults);

    /**
     * Returns a Task which asynchronously loads an annotated LeaderboardsClient.LeaderboardScores that represents the top page of scores for a given leaderboard specified by leaderboardId.
     * <p>
     * LeaderboardsClient.LeaderboardScores.release() should be called to release resources after usage.
     * <p>
     * For LeaderboardVariant.COLLECTION_FRIENDS, this call will fail with FriendsResolutionRequiredException if the user has not granted the game access to their friends list.
     * The exception result can be used to ask for consent.
     *
     * @param leaderboardId         ID of the leaderboard.
     * @param span                  Time span to retrieve data for. Valid values are LeaderboardVariant.TIME_SPAN_DAILY, LeaderboardVariant.TIME_SPAN_WEEKLY, or LeaderboardVariant.TIME_SPAN_ALL_TIME.
     * @param leaderboardCollection The leaderboard collection to retrieve scores for. Valid values are either LeaderboardVariant.COLLECTION_PUBLIC or LeaderboardVariant.COLLECTION_FRIENDS.
     * @param maxResults            The maximum number of scores to fetch per page. Must be between 1 and 25.
     */
    Task<AnnotatedData<LeaderboardsClient.LeaderboardScores>> loadTopScores(String leaderboardId, int span, int leaderboardCollection, int maxResults);

    /**
     * Returns a Task which asynchronously loads an annotated LeaderboardsClient.LeaderboardScores that represents the top page of scores for the leaderboard specified by leaderboardId.
     * <p>
     * LeaderboardsClient.LeaderboardScores.release() should be called to release resources after usage.
     * <p>
     * For LeaderboardVariant.COLLECTION_FRIENDS, this call will fail with FriendsResolutionRequiredException if the user has not granted the game access to their friends list.
     * The exception result can be used to ask for consent.
     *
     * @param leaderboardId         ID of the leaderboard.
     * @param span                  Time span to retrieve data for. Valid values are LeaderboardVariant.TIME_SPAN_DAILY, LeaderboardVariant.TIME_SPAN_WEEKLY, or LeaderboardVariant.TIME_SPAN_ALL_TIME.
     * @param leaderboardCollection The leaderboard collection to retrieve scores for. Valid values are either LeaderboardVariant.COLLECTION_PUBLIC or LeaderboardVariant.COLLECTION_FRIENDS.
     * @param maxResults            The maximum number of scores to fetch per page. Must be between 1 and 25.
     * @param forceReload           If true, this call will clear any locally cached data and attempt to fetch the latest data from the server.
     *                              This would commonly be used for something like a user-initiated refresh. Normally, this should be set to false to gain advantages of data caching.
     */
    Task<AnnotatedData<LeaderboardsClient.LeaderboardScores>> loadTopScores(String leaderboardId, int span, int leaderboardCollection, int maxResults, boolean forceReload);

    /**
     * Submit a score to a leaderboard for the currently signed-in player. The score is ignored if it is worse (as defined by the leaderboard configuration) than a previously submitted score for the same player.
     * <p>
     * This form of the API is a fire-and-forget form. Use this if you do not need to be notified of the results of submitting the score, though note that the update may not be sent to the server until the next sync.
     * <p>
     * The meaning of the score value depends on the formatting of the leaderboard established in the developer console. Leaderboards support the following score formats:
     * <p>
     * Fixed-point: score represents a raw value, and will be formatted based on the number of decimal places configured. A score of 1000 would be formatted as 1000, 100.0, or 10.00 for 0, 1, or 2 decimal places.
     * Time: score represents an elapsed time in milliseconds. The value will be formatted as an appropriate time value.
     * Currency: score represents a value in micro units. For example, in USD, a score of 100 would display as $0.0001, while a score of 1000000 would display as $1.00
     * For more details, please see Leaderboard Concepts.
     *
     * @param leaderboardId The leaderboard to submit the score to.
     * @param score         The raw score value.
     * @param scoreTag      Optional metadata about this score. The value may contain no more than 64 URI-safe characters as defined by section 2.3 of RFC 3986.
     */
    void submitScore(String leaderboardId, long score, String scoreTag);

    /**
     * Submit a score to a leaderboard for the currently signed-in player. The score is ignored if it is worse (as defined by the leaderboard configuration) than a previously submitted score for the same player.
     * <p>
     * This form of the API is a fire-and-forget form. Use this if you do not need to be notified of the results of submitting the score, though note that the update may not be sent to the server until the next sync.
     * <p>
     * The meaning of the score value depends on the formatting of the leaderboard established in the developer console. Leaderboards support the following score formats:
     * <p>
     * Fixed-point: score represents a raw value, and will be formatted based on the number of decimal places configured. A score of 1000 would be formatted as 1000, 100.0, or 10.00 for 0, 1, or 2 decimal places.
     * Time: score represents an elapsed time in milliseconds. The value will be formatted as an appropriate time value.
     * Currency: score represents a value in micro units. For example, in USD, a score of 100 would display as $0.0001, while a score of 1000000 would display as $1.00
     * For more details, please see Leaderboard Concepts.
     *
     * @param leaderboardId The leaderboard to submit the score to.
     * @param score         The raw score value.
     */
    void submitScore(String leaderboardId, long score);

    /**
     * Result delivered when leaderboard scores have been loaded.
     */
    class LeaderboardScores implements Releasable {
        private final Leaderboard leaderboard;
        private final LeaderboardScoreBuffer leaderboardScores;

        public LeaderboardScores(@Nullable Leaderboard leaderboard, @NonNull LeaderboardScoreBuffer leaderboardScores) {
            this.leaderboard = leaderboard;
            this.leaderboardScores = leaderboardScores;
        }

        @Nullable
        public Leaderboard getLeaderboard() {
            return this.leaderboard;
        }

        @NonNull
        public LeaderboardScoreBuffer getScores() {
            return this.leaderboardScores;
        }

        public void release() {
            this.leaderboardScores.release();
        }
    }
}
