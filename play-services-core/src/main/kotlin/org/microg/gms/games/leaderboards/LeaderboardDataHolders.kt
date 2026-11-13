/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.leaderboards

import android.content.ContentValues
import android.os.Bundle
import com.google.android.gms.common.data.DataHolder
import com.google.android.gms.games.GameColumns
import com.google.android.gms.games.PlayerColumns
import com.google.android.gms.games.leaderboard.LeaderboardColumns
import com.google.android.gms.games.leaderboard.LeaderboardScoreColumns
import org.microg.gms.games.toContentValues

// Constants of the client library (LeaderboardVariant, Leaderboard, LeaderboardsClient)
const val TIME_SPAN_DAILY = 0
const val TIME_SPAN_WEEKLY = 1
const val TIME_SPAN_ALL_TIME = 2
const val COLLECTION_PUBLIC = 0
const val COLLECTION_FRIENDS = 3
const val PAGE_DIRECTION_PREV = 1
private const val SCORE_ORDER_SMALLER_IS_BETTER = 0
private const val SCORE_ORDER_LARGER_IS_BETTER = 1

// The scores header is handed back by the client in loadMoreScores()
private const val HEADER_LEADERBOARD_ID = "leaderboard_id"
private const val HEADER_TIME_SPAN = "time_span"
private const val HEADER_COLLECTION = "collection"
private const val HEADER_PLAYER_CENTERED = "player_centered"
private const val HEADER_NEXT_PAGE_TOKEN = "next_page_token"
private const val HEADER_PREV_PAGE_TOKEN = "prev_page_token"

fun Int.toScoreTimeSpan() = when (this) {
    TIME_SPAN_DAILY -> ScoreTimeSpan.DAILY
    TIME_SPAN_WEEKLY -> ScoreTimeSpan.WEEKLY
    else -> ScoreTimeSpan.ALL_TIME
}

fun Int.toIncludeRankType() = if (this == COLLECTION_PUBLIC) IncludeRankType.PUBLIC else IncludeRankType.FRIENDS

data class LeaderboardScoresRequest(
    val leaderboardId: String,
    val timeSpan: Int,
    val collection: Int,
    val playerCentered: Boolean,
    val pageToken: String? = null
)

/**
 * Scores loaded so far for one leaderboard variant. Like Play Games, further pages are added to the previous ones,
 * clients (e.g. Slay the Spire) index into the combined result.
 */
class LoadedLeaderboardScores(val request: LeaderboardScoresRequest) {
    val entries = mutableListOf<LeaderboardEntry>()
    var nextPageToken: String? = null
        private set
    var prevPageToken: String? = null
        private set

    fun add(response: ListLeaderboardScoresResponse, pageDirection: Int?) {
        when (pageDirection) {
            null -> {
                entries.clear()
                entries.addAll(response.items)
                nextPageToken = response.nextPageToken
                prevPageToken = response.prevPageToken
            }
            PAGE_DIRECTION_PREV -> {
                entries.addAll(0, response.items)
                prevPageToken = response.prevPageToken
            }
            else -> {
                entries.addAll(response.items)
                nextPageToken = response.nextPageToken
            }
        }
    }

    fun toHeader() = Bundle().apply {
        putString(HEADER_LEADERBOARD_ID, request.leaderboardId)
        putInt(HEADER_TIME_SPAN, request.timeSpan)
        putInt(HEADER_COLLECTION, request.collection)
        putBoolean(HEADER_PLAYER_CENTERED, request.playerCentered)
        putString(HEADER_NEXT_PAGE_TOKEN, nextPageToken)
        putString(HEADER_PREV_PAGE_TOKEN, prevPageToken)
    }
}

fun Bundle.toLeaderboardScoresRequest(): LeaderboardScoresRequest? {
    val leaderboardId = getString(HEADER_LEADERBOARD_ID) ?: return null
    return LeaderboardScoresRequest(leaderboardId, getInt(HEADER_TIME_SPAN), getInt(HEADER_COLLECTION), getBoolean(HEADER_PLAYER_CENTERED))
}

fun Bundle.getPageToken(pageDirection: Int): String? =
    getString(if (pageDirection == PAGE_DIRECTION_PREV) HEADER_PREV_PAGE_TOKEN else HEADER_NEXT_PAGE_TOKEN)

/**
 * Leaderboard metadata with a single variant, the one that was requested.
 */
fun buildLeaderboardDataHolder(
    request: LeaderboardScoresRequest,
    definition: LeaderboardDefinition?,
    response: ListLeaderboardScoresResponse?,
    loaded: LoadedLeaderboardScores? = null
): DataHolder {
    val columns = (GameColumns.CURRENT_GAME_COLUMNS + LeaderboardColumns.CURRENT_LEADERBOARD_COLUMNS).toTypedArray()
    val playerScore = response?.playerScore
    val row = ContentValues().apply {
        put(LeaderboardColumns.EXTERNAL_LEADERBOARD_ID, request.leaderboardId)
        put(LeaderboardColumns.NAME, definition?.name)
        put(LeaderboardColumns.BOARD_ICON_IMAGE_URI, definition?.iconUrl)
        put(LeaderboardColumns.BOARD_ICON_IMAGE_URL, definition?.iconUrl)
        put(
            LeaderboardColumns.SCORE_ORDER,
            if (definition?.order == "SMALLER_IS_BETTER") SCORE_ORDER_SMALLER_IS_BETTER else SCORE_ORDER_LARGER_IS_BETTER
        )
        put(LeaderboardColumns.TIMESPAN, request.timeSpan)
        put(LeaderboardColumns.COLLECTION, request.collection)
        response?.numScores?.toLongOrNull()?.let { put(LeaderboardColumns.TOTAL_SCORES, it) }
        playerScore?.scoreValue?.toLongOrNull()?.let { put(LeaderboardColumns.PLAYER_RAW_SCORE, it) }
        playerScore?.formattedScore?.let { put(LeaderboardColumns.PLAYER_DISPLAY_SCORE, it) }
        playerScore?.scoreRank?.toLongOrNull()?.let { put(LeaderboardColumns.PLAYER_RANK, it) }
        playerScore?.formattedScoreRank?.let { put(LeaderboardColumns.PLAYER_DISPLAY_RANK, it) }
        playerScore?.scoreTag?.takeIf { it.isNotEmpty() }?.let { put(LeaderboardColumns.PLAYER_SCORE_TAG, it) }
        if (request.playerCentered) {
            put(LeaderboardColumns.WINDOW_PAGE_TOKEN_PREV, loaded?.prevPageToken ?: response?.prevPageToken)
            put(LeaderboardColumns.WINDOW_PAGE_TOKEN_NEXT, loaded?.nextPageToken ?: response?.nextPageToken)
        } else {
            put(LeaderboardColumns.TOP_PAGE_TOKEN_NEXT, loaded?.nextPageToken ?: response?.nextPageToken)
        }
    }
    return DataHolder.builder(columns).withRow(row).build(0)
}

fun buildLeaderboardScoresDataHolder(entries: List<LeaderboardEntry>, statusCode: Int, header: Bundle? = null): DataHolder {
    val columns = (PlayerColumns.CURRENT_PLAYER_COLUMNS + LeaderboardScoreColumns.CURRENT_SCORE_COLUMNS).toTypedArray()
    val builder = DataHolder.builder(columns)
    for (entry in entries) {
        val row = entry.player?.toContentValues() ?: ContentValues()
        row.put(LeaderboardScoreColumns.RANK, entry.scoreRank?.toLongOrNull() ?: -1L)
        row.put(LeaderboardScoreColumns.DISPLAY_RANK, entry.formattedScoreRank)
        row.put(LeaderboardScoreColumns.RAW_SCORE, entry.scoreValue?.toLongOrNull() ?: 0L)
        row.put(LeaderboardScoreColumns.DISPLAY_SCORE, entry.formattedScore)
        row.put(LeaderboardScoreColumns.ACHIEVED_TIMESTAMP, entry.writeTimestampMillis?.toLongOrNull() ?: 0L)
        entry.scoreTag?.takeIf { it.isNotEmpty() }?.let { row.put(LeaderboardScoreColumns.SCORE_TAG, it) }
        row.put(LeaderboardScoreColumns.DEFAULT_DISPLAY_NAME, entry.player?.displayName)
        row.put(LeaderboardScoreColumns.DEFAULT_DISPLAY_IMAGE_URI, entry.player?.iconImageUrl)
        row.put(LeaderboardScoreColumns.DEFAULT_DISPLAY_IMAGE_URL, entry.player?.iconImageUrl)
        builder.withRow(row)
    }
    return builder.build(statusCode, header)
}

/**
 * Result of a score submission, one row per time span, as read by the client's ScoreSubmissionData.
 */
fun buildScoreSubmissionDataHolder(
    leaderboardId: String, playerId: String?, score: Long, scoreTag: String?, response: SubmitLeaderboardScoreResponse
): DataHolder {
    val columns = arrayOf("leaderboardId", "playerId", "timeSpan", "hasResult", "rawScore", "formattedScore", "newBest", "scoreTag")
    val builder = DataHolder.builder(columns)
    for (timeSpan in listOf(TIME_SPAN_DAILY, TIME_SPAN_WEEKLY, TIME_SPAN_ALL_TIME)) {
        val name = timeSpan.toScoreTimeSpan().name
        val row = ContentValues().apply {
            put("leaderboardId", leaderboardId)
            put("playerId", playerId)
            put("timeSpan", timeSpan)
        }
        val unbeaten = response.unbeatenScores?.firstOrNull { it.timeSpan == name }
        if (response.beatenScoreTimeSpans?.contains(name) == true) {
            row.put("hasResult", true)
            row.put("rawScore", score)
            row.put("formattedScore", response.formattedScore)
            row.put("newBest", true)
            row.put("scoreTag", scoreTag)
        } else if (unbeaten != null) {
            row.put("hasResult", true)
            row.put("rawScore", unbeaten.score?.toLongOrNull() ?: 0L)
            row.put("formattedScore", unbeaten.formattedScore)
            row.put("newBest", false)
            row.put("scoreTag", unbeaten.scoreTag?.takeIf { it.isNotEmpty() })
        }
        builder.withRow(row)
    }
    return builder.build(0)
}
