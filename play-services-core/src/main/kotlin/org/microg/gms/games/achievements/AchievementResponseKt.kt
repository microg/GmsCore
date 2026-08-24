/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.achievements

import org.json.JSONObject

data class AchievementDefinitionsListResponse<T>(
    val items: List<T>, val kind: String?, val nextPageToken: String?
) {
    override fun toString(): String {
        return "AchievementDefinitionsListResponse(items=$items, kind='$kind', nextPageToken='$nextPageToken')"
    }
}

data class AchievementDefinition(
    val achievementType: Int,
    val description: String?,
    val experiencePoints: String,
    val formattedTotalSteps: String?,
    val id: String?,
    var initialState: Int,
    val isRevealedIconUrlDefault: Boolean?,
    val isUnlockedIconUrlDefault: Boolean?,
    val kind: String?,
    val name: String?,
    val revealedIconUrl: String?,
    val totalSteps: Int,
    val unlockedIconUrl: String?
) {

    constructor(name: String, type: Int) : this(type, null, "", "", "", 0, null, null, null, name, null, 0, null)

    override fun toString(): String {
        return "AchievementDefinition(achievementType=$achievementType, description='$description', experiencePoints='$experiencePoints', formattedTotalSteps='$formattedTotalSteps', id='$id', initialState=$initialState, isRevealedIconUrlDefault=$isRevealedIconUrlDefault, isUnlockedIconUrlDefault=$isUnlockedIconUrlDefault, kind='$kind', name='$name', revealedIconUrl='$revealedIconUrl', totalSteps=$totalSteps, unlockedIconUrl='$unlockedIconUrl')"
    }
}

data class PlayerAchievement(
    val kind: String?,
    val id: String?,
    val currentSteps: Int,
    val formattedCurrentStepsString: String?,
    val achievementState: String,
    val lastUpdatedTimestamp: String?,
    val experiencePoints: String?
) {
    override fun toString(): String {
        return "PlayerAchievement(kind=$kind, id=$id, currentSteps=$currentSteps, formattedCurrentStepsString=$formattedCurrentStepsString, achievementState=$achievementState, lastUpdatedTimestamp=$lastUpdatedTimestamp, experiencePoints=$experiencePoints)"
    }
}

data class AchievementIncrementResponse(
    val kind: String?, val currentSteps: Int, val newlyUnlocked: Boolean
) {
    override fun toString(): String {
        return "AchievementIncrementResponse(kind=$kind, currentSteps=$currentSteps, newlyUnlocked=$newlyUnlocked)"
    }
}

data class AchievementRevealResponse(
    val kind: String?,
    val currentState: String,
) {
    override fun toString(): String {
        return "AchievementRevealResponse(kind=$kind, currentState=$currentState)"
    }
}

data class AchievementUnlockResponse(
    val kind: String?,
    val newlyUnlocked: Boolean,
) {
    override fun toString(): String {
        return "AchievementUnlockResponse(kind=$kind, newlyUnlocked=$newlyUnlocked)"
    }
}

fun JSONObject.toUnlockResponse(): AchievementUnlockResponse {
    return AchievementUnlockResponse(
        optString("kind"), optBoolean("newlyUnlocked")
    )
}

fun JSONObject.toIncrementResponse(): AchievementIncrementResponse {
    return AchievementIncrementResponse(
        optString("kind"), optInt("currentSteps"), optBoolean("newlyUnlocked")
    )
}

fun JSONObject.toRevealResponse(): AchievementRevealResponse {
    return AchievementRevealResponse(
        optString("kind"), optString("currentState")
    )
}

fun JSONObject.toAllAchievementListResponse(): AchievementDefinitionsListResponse<AchievementDefinition> {
    val items = optJSONArray("items")
    val achievements = ArrayList<AchievementDefinition>()
    if (items != null) {
        for (i in 0..<items.length()) {
            val jsonObject = items.getJSONObject(i)
            val achievementDefinition = AchievementDefinition(
                getAchievementType(jsonObject.optString("achievementType")),
                jsonObject.optString("description"),
                jsonObject.optString("experiencePoints"),
                jsonObject.optString("formattedTotalSteps"),
                jsonObject.optString("id"),
                getAchievementState(jsonObject.optString("initialState")),
                jsonObject.optBoolean("isRevealedIconUrlDefault"),
                jsonObject.optBoolean("isUnlockedIconUrlDefault"),
                jsonObject.optString("kind"),
                jsonObject.optString("name"),
                jsonObject.optString("revealedIconUrl"),
                jsonObject.optInt("totalSteps"),
                jsonObject.optString("unlockedIconUrl")
            )
            achievements.add(achievementDefinition)
        }
    }
    return AchievementDefinitionsListResponse(achievements, optString("kind"), optString("nextPageToken"))
}

fun JSONObject.toPlayerAchievementListResponse(): AchievementDefinitionsListResponse<PlayerAchievement> {
    val items = optJSONArray("items")
    val achievements = ArrayList<PlayerAchievement>()
    if (items != null) {
        for (i in 0..<items.length()) {
            val jsonObject = items.getJSONObject(i)
            val playerAchievement = PlayerAchievement(
                jsonObject.optString("kind"),
                jsonObject.optString("id"),
                jsonObject.optInt("currentSteps"),
                jsonObject.optString("formattedCurrentStepsString"),
                jsonObject.optString("achievementState"),
                jsonObject.optString("lastUpdatedTimestamp"),
                jsonObject.optString("experiencePoints"),
            )
            achievements.add(playerAchievement)
        }
    }
    return AchievementDefinitionsListResponse(achievements, optString("kind"), optString("nextPageToken"))
}

fun getAchievementType(type: String): Int {
    return if (type == "STANDARD") {
        AchievementType.TYPE_STANDARD
    } else AchievementType.TYPE_INCREMENTAL
}

fun getAchievementState(state: String?): Int {
    if (state == "UNLOCKED") {
        return AchievementState.STATE_UNLOCKED
    }
    return if (state == "REVEALED") {
        AchievementState.STATE_REVEALED
    } else AchievementState.STATE_HIDDEN
}


class AchievementState {
    companion object {
        /**
         * Constant indicating an unlocked achievement.
         */
        const val STATE_UNLOCKED = 0

        /**
         * Constant indicating a revealed achievement.
         */
        const val STATE_REVEALED = 1

        /**
         * Constant indicating a hidden achievement.
         */
        const val STATE_HIDDEN = 2
    }
}

class AchievementType {
    companion object {
        /**
         * Constant indicating a standard achievement.
         */
        const val TYPE_STANDARD = 0

        /**
         * Constant indicating an incremental achievement.
         */
        const val TYPE_INCREMENTAL = 1
    }
}
