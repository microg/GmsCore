/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.leaderboards

import com.google.android.gms.games.PlayerEntity
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.games.toPlayer

data class LeaderboardListResponse(
    val items: List<LeaderboardDefinition>, val kind: String?, val nextPageToken: String?
) {
    override fun toString(): String {
        return "LeaderboardListResponse(items=$items, kind='$kind', nextPageToken='$nextPageToken')"
    }
}

data class LeaderboardDefinition(
    val kind: String?,
    val id: String?,
    val name: String?,
    val iconUrl: String?,
    val isIconUrlDefault: Boolean,
    val order: String?
) {
    override fun toString(): String {
        return "LeaderboardDefinition(kind=$kind, id=$id, name=$name, iconUrl=$iconUrl, isIconUrlDefault=$isIconUrlDefault, order=$order)"
    }
}

data class GetLeaderboardScoresResponse(
    val kind: String?, val nextPageToken: String?, val player: PlayerEntity?, val items: List<LeaderboardScore>?
) {
    override fun toString(): String {
        return "GetLeaderboardScoresResponse(kind=$kind, nextPageToken=$nextPageToken, player=$player, items=$items)"
    }
}

data class LeaderboardScore(
    val kind: String?,
    val leaderboardId: String?,
    val scoreValue: String?,
    val scoreString: String?,
    val publicRank: LeaderboardScoreRank?,
    val socialRank: LeaderboardScoreRank?,
    val friendsRank: LeaderboardScoreRank?,
    val timeSpan: String,
    val writeTimestamp: String,
    val scoreTag: String
) {
    override fun toString(): String {
        return "LeaderboardScore(kind=$kind, leaderboardId=$leaderboardId, scoreValue=$scoreValue, scoreString=$scoreString, publicRank=$publicRank, socialRank=$socialRank, friendsRank=$friendsRank, timeSpan=$timeSpan, writeTimestamp='$writeTimestamp', scoreTag='$scoreTag')"
    }
}

data class LeaderboardScoreRank(
    val kind: String?,
    val rank: String?,
    val formattedRank: String?,
    val numScores: String?,
    val formattedNumScores: String?,
) {
    override fun toString(): String {
        return "LeaderboardScoreRank(kind=$kind, rank=$rank, formattedRank=$formattedRank, numScores=$numScores, formattedNumScores=$formattedNumScores)"
    }
}

data class ListLeaderboardScoresResponse(
    val kind: String?,
    val nextPageToken: String?,
    val prevPageToken: String?,
    val numScores: String?,
    val playerScore: LeaderboardEntry?,
    val items: ArrayList<LeaderboardEntry>,
) {
    override fun toString(): String {
        return "ListLeaderboardScoresResponse(kind=$kind, nextPageToken=$nextPageToken, prevPageToken=$prevPageToken, numScores=$numScores, playerScore=$playerScore, items=$items)"
    }
}

data class LeaderboardEntry(
    val kind: String?,
    val player: PlayerEntity?,
    val scoreRank: String?,
    val formattedScoreRank: String?,
    val scoreValue: String?,
    val formattedScore: String?,
    val timeSpan: String?,
    val writeTimestampMillis: String?,
    val scoreTag: String?,
) {
    constructor(leaderboardTitle: String?, leaderboardLogoUrl: String?) : this(
        null, null, null, null, leaderboardTitle, null, null, null, leaderboardLogoUrl
    )

    override fun toString(): String {
        return "LeaderboardEntry(kind=$kind, player=$player, scoreRank=$scoreRank, formattedScoreRank=$formattedScoreRank, scoreValue=$scoreValue, formattedScore=$formattedScore, timeSpan=$timeSpan, writeTimestampMillis=$writeTimestampMillis, scoreTag=$scoreTag)"
    }
}

data class SubmitLeaderboardScoreListResponse(
    val kind: String?,
    val submittedScores: List<SubmitLeaderboardScoreResponse>?,
) {
    override fun toString(): String {
        return "SubmitLeaderboardScoreListResponse(kind=$kind, submittedScores=$submittedScores)"
    }
}

data class SubmitLeaderboardScoreResponse(
    val kind: String?,
    val beatenScoreTimeSpans: List<String>?,
    val unbeatenScores: List<PlayerScore>?,
    val formattedScore: String?,
    val leaderboardId: String?,
    val scoreTag: String?,
) {
    override fun toString(): String {
        return "SubmitLeaderboardScoreResponse(kind=$kind, beatenScoreTimeSpans=$beatenScoreTimeSpans, unbeatenScores=$unbeatenScores, formattedScore=$formattedScore, leaderboardId=$leaderboardId, scoreTag=$scoreTag)"
    }
}

data class PlayerScore(
    val kind: String?, val timeSpan: String?, val score: String?, val formattedScore: String?, val scoreTag: String?
) {
    override fun toString(): String {
        return "PlayerScore(kind=$kind, timeSpan=$timeSpan, score=$score, formattedScore=$formattedScore, scoreTag=$scoreTag)"
    }
}

data class PlayerScoreSubmissionList(
    val kind: String?, val scores: List<ScoreSubmission>
) {
    override fun toString(): String {
        return "PlayerScoreSubmissionList(kind=$kind, scores=$scores)"
    }
}

data class ScoreSubmission(
    val kind: String?,
    val leaderboardId: String?,
    val score: String?,
    val scoreTag: String?,
    val signature: String?,
) {
    override fun toString(): String {
        return "ScoreSubmission(kind=$kind, leaderboardId=$leaderboardId, score=$score, scoreTag=$scoreTag, signature=$signature)"
    }
}

fun PlayerScoreSubmissionList.toJSONObject() = JSONObject().apply {
    put("kind", kind)
    put("scores", JSONArray().apply {
        for (score in scores) {
            put(score.toJSONObject())
        }
    })
}

fun ScoreSubmission.toJSONObject() = JSONObject().apply {
    put("kind", kind)
    put("leaderboardId", leaderboardId)
    put("score", score)
    put("scoreTag", scoreTag)
    put("signature", signature)
}

fun JSONObject.toSubmitLeaderboardScoreListResponse() = SubmitLeaderboardScoreListResponse(
    optString("kind"),
    optJSONArray("submittedScores")?.toSubmitLeaderboardScoreResponseList(),
)

fun JSONArray.toSubmitLeaderboardScoreResponseList(): List<SubmitLeaderboardScoreResponse> {
    val list = arrayListOf<SubmitLeaderboardScoreResponse>()
    for (i in 0..<length()) {
        val jsonObject = optJSONObject(i)
        list.add(jsonObject.toSubmitLeaderboardScoreResponse())
    }
    return list
}

fun JSONObject.toSubmitLeaderboardScoreResponse() = SubmitLeaderboardScoreResponse(
    optString("kind"),
    optJSONArray("beatenScoreTimeSpans")?.toTimeSpans(),
    optJSONArray("unbeatenScores")?.toPlayerScore(),
    optString("formattedScore"),
    optString("leaderboardId"),
    optString("scoreTag"),
)

fun JSONArray.toTimeSpans(): List<String> {
    val list = arrayListOf<String>()
    for (i in 0..<length()) {
        list.add(optString(i))
    }
    return list
}

fun JSONArray.toPlayerScore(): List<PlayerScore> {
    val list = arrayListOf<PlayerScore>()
    for (i in 0..<length()) {
        val jsonObject = optJSONObject(i)
        list.add(
            PlayerScore(
                jsonObject.optString("kind"),
                jsonObject.optString("timeSpan"),
                jsonObject.optString("score"),
                jsonObject.optString("formattedScore"),
                jsonObject.optString("scoreTag"),
            )
        )
    }
    return list
}

fun JSONObject.toListLeaderboardScoresResponse() = ListLeaderboardScoresResponse(
    optString("kind"),
    optString("nextPageToken"),
    optString("prevPageToken"),
    optString("numScores"),
    optJSONObject("playerScore")?.toLeaderboardEntry(),
    getJSONArray("items").toLeaderboardEntryList(),
)

fun JSONArray.toLeaderboardEntryList(): ArrayList<LeaderboardEntry> {
    val list = arrayListOf<LeaderboardEntry>()
    for (i in 0..<length()) {
        val jsonObject = optJSONObject(i)
        list.add(jsonObject.toLeaderboardEntry())
    }
    return list
}

fun JSONObject.toLeaderboardEntry() = LeaderboardEntry(
    optString("kind"),
    optJSONObject("player")?.toPlayer(),
    optString("scoreRank"),
    optString("formattedScoreRank"),
    optString("scoreValue"),
    optString("formattedScore"),
    optString("timeSpan"),
    optString("writeTimestampMillis"),
    optString("scoreTag"),
)

fun JSONObject.toGetLeaderboardScoresResponse() = GetLeaderboardScoresResponse(
    optString("kind"),
    optString("nextPageToken"),
    optJSONObject("player")?.toPlayer(),
    optJSONArray("items")?.toLeaderboardScoreList()
)

fun JSONArray.toLeaderboardScoreList(): List<LeaderboardScore> {
    val list = arrayListOf<LeaderboardScore>()
    for (i in 0..<length()) {
        val jsonObject = optJSONObject(i)
        list.add(
            LeaderboardScore(
                jsonObject.optString("kind"),
                jsonObject.optString("leaderboard_id"),
                jsonObject.optString("scoreValue"),
                jsonObject.optString("scoreString"),
                jsonObject.optJSONObject("publicRank")?.toLeaderboardScoreRank(),
                jsonObject.optJSONObject("socialRank")?.toLeaderboardScoreRank(),
                jsonObject.optJSONObject("friendsRank")?.toLeaderboardScoreRank(),
                jsonObject.optString("timeSpan"),
                jsonObject.optString("writeTimestamp"),
                jsonObject.optString("scoreTag"),
            )
        )
    }
    return list
}

fun JSONObject.toLeaderboardScoreRank() = LeaderboardScoreRank(
    optString("kind"),
    optString("rank"),
    optString("formattedRank"),
    optString("numScores"),
    optString("formattedNumScores"),
)

fun JSONObject.toLeaderboardResponse(): LeaderboardDefinition {
    return LeaderboardDefinition(
        optString("kind"),
        optString("id"),
        optString("name"),
        optString("iconUrl"),
        optBoolean("isIconUrlDefault"),
        optString("order")
    )
}

fun JSONObject.toLeaderboardListResponse(): LeaderboardListResponse {
    val items = optJSONArray("items")
    val leaderboardDefinitions = ArrayList<LeaderboardDefinition>()
    if (items != null) {
        for (i in 0..<items.length()) {
            val jsonObject = items.getJSONObject(i)
            val achievementDefinition = LeaderboardDefinition(
                jsonObject.optString("kind"),
                jsonObject.optString("id"),
                jsonObject.optString("name"),
                jsonObject.optString("iconUrl"),
                jsonObject.optBoolean("isIconUrlDefault"),
                jsonObject.optString("order")
            )
            leaderboardDefinitions.add(achievementDefinition)
        }
    }
    return LeaderboardListResponse(leaderboardDefinitions, optString("kind"), optString("nextPageToken"))
}

enum class ScoreTimeSpan {
    ALL, ALL_TIME, WEEKLY, DAILY
}

enum class IncludeRankType {
    ALL, PUBLIC, FRIENDS
}