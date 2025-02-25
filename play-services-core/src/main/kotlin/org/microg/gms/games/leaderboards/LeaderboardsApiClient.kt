/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.leaderboards

import android.content.Context
import com.android.volley.Request
import org.microg.gms.games.requestGamesInfo
import java.util.Locale
import java.util.UUID

/**
 * https://developers.google.com/games/services/web/api/rest#rest-resource:-leaderboards
 * https://developers.google.com/games/services/web/api/rest#rest-resource:-scores
 */
object LeaderboardsApiClient {

    /**
     * Lists all the leaderboard metadata for your application.
     */
    suspend fun requestAllLeaderboards(mContext: Context, oauthToken: String, pageToken: String? = null) =
        requestGamesInfo(mContext, Request.Method.GET, oauthToken, "https://games.googleapis.com/games/v1/leaderboards", HashMap<String, String>().apply {
            put("language", Locale.getDefault().language)
            if (pageToken != null) {
                put("pageToken", pageToken)
            }
        }).toLeaderboardListResponse()

    /**
     * Retrieves the metadata of the leaderboard with the given ID.
     */
    suspend fun getLeaderboardById(mContext: Context, oauthToken: String, leaderboardId: String) = requestGamesInfo(mContext,
        Request.Method.GET,
        oauthToken,
        "https://games.googleapis.com/games/v1/leaderboards/${leaderboardId}",
        HashMap<String, String>().apply {
            put("language", Locale.getDefault().language)
        }).toLeaderboardResponse()

    /**
     * Get high scores, and optionally ranks, in leaderboards for the currently authenticated player.
     * For a specific time span, leaderboardId can be set to ALL to retrieve data for all leaderboards in a given time span.
     * `NOTE: You cannot ask for 'ALL' leaderboards and 'ALL' timeSpans in the same request; only one parameter may be set to 'ALL'.
     */
    suspend fun getLeaderboardScoresById(
        mContext: Context, oauthToken: String, leaderboardId: String, timeSpan: ScoreTimeSpan, pageToken: String? = null
    ) = requestGamesInfo(mContext,
        Request.Method.GET,
        oauthToken,
        "https://games.googleapis.com/games/v1/players/me/leaderboards/$leaderboardId/scores/$timeSpan",
        HashMap<String, String>().apply {
            put("language", Locale.getDefault().language)
            put("includeRankType", IncludeRankType.PUBLIC.toString())
            if (pageToken != null) {
                put("pageToken", pageToken)
            }
        }).toGetLeaderboardScoresResponse()

    /**
     * Lists the scores in a leaderboard, starting from the top.
     */
    suspend fun requestLeaderboardScoresById(
        mContext: Context, oauthToken: String, leaderboardId: String, pageToken: String? = null
    ) = requestGamesInfo(mContext,
        Request.Method.GET,
        oauthToken,
        "https://games.googleapis.com/games/v1/leaderboards/$leaderboardId/scores/${IncludeRankType.PUBLIC}",
        HashMap<String, String>().apply {
            put("language", Locale.getDefault().language)
            put("timeSpan", ScoreTimeSpan.ALL_TIME.toString())
            if (pageToken != null) {
                put("pageToken", pageToken)
            }
        }).toListLeaderboardScoresResponse()

    /**
     * Lists the scores in a leaderboard around (and including) a player's score.
     */
    suspend fun requestLeaderboardScoresListWindowById(
        mContext: Context, oauthToken: String, leaderboardId: String, collection: IncludeRankType, timeSpan: ScoreTimeSpan, pageToken: String? = null
    ) = requestGamesInfo(mContext,
        Request.Method.GET,
        oauthToken,
        "https://games.googleapis.com/games/v1/leaderboards/$leaderboardId/window/$collection",
        HashMap<String, String>().apply {
            put("language", Locale.getDefault().language)
            put("timeSpan", timeSpan.toString())
            put("returnTopIfAbsent", "true")
            if (pageToken != null) {
                put("pageToken", pageToken)
            }
        }).toListLeaderboardScoresResponse()

    /**
     * Submits a score to the specified leaderboard.
     */
    suspend fun submitLeaderboardScores(mContext: Context, oauthToken: String, leaderboardId: String, score: String) = requestGamesInfo(
        mContext,
        Request.Method.POST,
        oauthToken,
        "https://games.googleapis.com/games/v1/leaderboards/$leaderboardId/scores",
        HashMap<String, String>().apply {
            put("language", Locale.getDefault().language)
            put("score", score)
            put("scoreTag", UUID.fromString(leaderboardId + score).toString())
        }).toSubmitLeaderboardScoreResponse()

    /**
     * Submits multiple scores to leaderboards.
     */
    suspend fun submitMultipleLeaderboardScores(
        mContext: Context, oauthToken: String, list: PlayerScoreSubmissionList
    ) = requestGamesInfo(
        mContext, Request.Method.POST, oauthToken, "https://games.googleapis.com/games/v1/leaderboards/scores", HashMap<String, String>().apply {
            put("language", Locale.getDefault().language)
        }, list.toJSONObject()
    ).toSubmitLeaderboardScoreListResponse()
}
