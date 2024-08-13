/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.leaderboard;

import androidx.annotation.IntDef;

import com.google.android.gms.common.data.Freezable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Data interface for a specific variant of a leaderboard;
 * a variant is defined by the combination of the leaderboard's collection (public or friends) and time span (daily, weekly, or all-time).
 */
public interface LeaderboardVariant extends Freezable<LeaderboardVariant> {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {Collection.COLLECTION_FRIENDS, Collection.COLLECTION_PUBLIC})
    @interface Collection {
        /**
         * Collection constant for public leaderboards.
         * Public leaderboards contain the scores of players who are sharing their gameplay activity publicly.
         */
        int COLLECTION_PUBLIC = 0;
        /**
         * Collection constant for friends leaderboards.
         * These leaderboards contain the scores of players in the viewing player's friends list.
         */
        int COLLECTION_FRIENDS = 3;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {TimeSpan.TIME_SPAN_DAILY, TimeSpan.TIME_SPAN_WEEKLY, TimeSpan.TIME_SPAN_ALL_TIME, TimeSpan.NUM_TIME_SPANS})
    @interface TimeSpan {
        /**
         * Scores are reset every day.
         * The reset occurs at 11:59PM PST.
         */
        int TIME_SPAN_DAILY = 0;
        /**
         * Scores are reset once per week.
         * The reset occurs at 11:59PM PST on Sunday.
         */
        int TIME_SPAN_WEEKLY = 1;
        /**
         * Scores are never reset.
         */
        int TIME_SPAN_ALL_TIME = 2;
        /**
         * Number of time spans that exist.
         * Needs to be updated if we ever have more.
         */
        int NUM_TIME_SPANS = 3;
    }

    /**
     * Constant returned when a player's score for this variant is unknown.
     */
    int PLAYER_SCORE_UNKNOWN = -1;

    /**
     * Constant returned when the total number of scores for this variant is unknown.
     */
    int NUM_SCORES_UNKNOWN = -1;

    /**
     * Constant returned when a player's rank for this variant is unknown.
     */
    int PLAYER_RANK_UNKNOWN = -1;

    /**
     * Retrieves the collection of scores contained by this variant.
     * Possible values are {@link Collection#COLLECTION_PUBLIC} or {@link Collection#COLLECTION_FRIENDS}.
     *
     * @return The collection of scores contained by this variant.
     */
    int getCollection();

    /**
     * Retrieves the viewing player's formatted rank for this variant, if any.
     * Note that this value is only accurate if {@link LeaderboardVariant#hasPlayerInfo()} returns true.
     *
     * @return The String representation of the viewing player's rank, or {@code null) if the player has no rank for this variant.
     */
    String getDisplayPlayerRank();

    /**
     * Retrieves the viewing player's score for this variant, if any.
     * Note that this value is only accurate if {@link LeaderboardVariant#hasPlayerInfo()} returns true.
     *
     * @return The String representation of the viewing player's score, or null if the player has no score for this variant.
     */
    String getDisplayPlayerScore();

    /**
     * Retrieves the total number of scores for this variant. Not all of these scores will always be present on the local device.
     * Note that if scores for this variant have not been loaded, this method will return {@link LeaderboardVariant#NUM_SCORES_UNKNOWN}.
     *
     * @return The number of scores for this variant, or {@link LeaderboardVariant#NUM_SCORES_UNKNOWN}.
     */
    long getNumScores();

    /**
     * Retrieves the viewing player's rank for this variant, if any.
     * Note that this value is only accurate if {@link LeaderboardVariant#hasPlayerInfo()} returns true.
     *
     * @return The long representation of the viewing player's rank, or {@link LeaderboardVariant#PLAYER_RANK_UNKNOWN}
     * if the player has no rank for this variant.
     */
    long getPlayerRank();

    /**
     * Retrieves the viewing player's score tag for this variant, if any.
     * Note that this value is only accurate if {@link LeaderboardVariant#hasPlayerInfo()} returns true.
     *
     * @return The score tag associated with the viewing player's score, or null if the player has no score for this variant.
     */
    String getPlayerScoreTag();

    /**
     * Retrieves the viewing player's score for this variant, if any.
     * Note that this value is only accurate if {@link LeaderboardVariant#hasPlayerInfo()} returns true.
     *
     * @return The long representation of the viewing player's score, or {@link LeaderboardVariant#PLAYER_SCORE_UNKNOWN}
     * if the player has no score for this variant.
     */
    long getRawPlayerScore();

    /**
     * Retrieves the time span that the scores for this variant are drawn from.
     * Possible values are {@link TimeSpan#TIME_SPAN_ALL_TIME}, {@link TimeSpan#TIME_SPAN_WEEKLY}, or {@link TimeSpan#TIME_SPAN_DAILY}.
     *
     * @return The time span that the scores for this variant are drawn from.
     */
    int getTimeSpan();

    /**
     * Get whether or not this variant contains score information for the viewing player or not.
     * There are several possible reasons why this might be false.
     * If the scores for this variant have never been loaded, we won't know if the player has a score or not. Similarly,
     * if the player has not submitted a score for this variant, this will return false.
     * <p>
     * It is possible to have a score but no rank. For instance, on leaderboard variants of {@link Collection#COLLECTION_PUBLIC},
     * players who are not sharing their scores publicly will never have a rank.
     *
     * @return Whether or not this variant contains score information for the viewing player.
     */
    boolean hasPlayerInfo();

}
