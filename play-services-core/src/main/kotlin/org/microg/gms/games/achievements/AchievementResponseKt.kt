package org.microg.gms.games.achievements

import android.net.Uri
import com.google.android.gms.games.achievement.Achievement
import com.google.android.gms.games.achievement.AchievementEntity
import org.json.JSONArray
import org.json.JSONObject

data class AchievementDefinitionsListResponse<T>(
    val items: List<T>,
    val kind: String?,
    val nextPageToken: String?
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
    val initialState: Int,
    val isRevealedIconUrlDefault: Boolean?,
    val isUnlockedIconUrlDefault: Boolean?,
    val kind: String?,
    val name: String?,
    val revealedIconUrl: String?,
    val totalSteps: Int,
    val unlockedIconUrl: String?
) {

    fun toAchievementEntity() = AchievementEntity(
        id,
        achievementType,
        name,
        description,
        Uri.parse(unlockedIconUrl),
        unlockedIconUrl,
        Uri.parse(revealedIconUrl),
        revealedIconUrl,
        totalSteps,
        formattedTotalSteps,
        null,
        initialState,
        0,
        "0",
        System.currentTimeMillis(),
        experiencePoints.toLong(),
        -1f,
        null
    )

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
    val kind: String?,
    val currentSteps: Int,
    val newlyUnlocked: Boolean
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

data class UpdateMultipleAchievements(val kind: String?, val updates: ArrayList<UpdateAchievement>) {
    override fun toString(): String {
        return "UpdateMultipleAchievements(kind=$kind, updates=$updates)"
    }

    fun toJSONObject() = JSONObject().apply {
        putOpt("kind", kind)
        putOpt("updates", JSONArray().apply {
            updates.forEach { put(it.toJSONObject()) }
        })
    }
}

data class UpdateAchievement(val kind: String?, val achievementId: String?, val updateType: Int, val incrementPayload: AchievementIncrement?, val setStepsAtLeastPayload: SetAchievementSteps?) {
    override fun toString(): String {
        return "UpdateAchievement(kind=$kind, achievementId=$achievementId, updateType=$updateType, incrementPayload=$incrementPayload, setStepsAtLeastPayload=$setStepsAtLeastPayload)"
    }

    fun toJSONObject() = JSONObject().apply {
        putOpt("kind", kind)
        putOpt("achievementId", achievementId)
        putOpt("updateType", updateType)
        putOpt("incrementPayload", incrementPayload?.toJSONObject())
        putOpt("setStepsAtLeastPayload", setStepsAtLeastPayload?.toJSONObject())
    }
}

data class AchievementIncrement(val kind: String?, val steps: Int, val requestId: String?) {
    override fun toString(): String {
        return "AchievementIncrement(kind=$kind, steps=$steps, requestId='$requestId')"
    }

    fun toJSONObject() = JSONObject().apply {
        putOpt("kind", kind)
        putOpt("steps", steps)
        putOpt("requestId", requestId)
    }
}

data class SetAchievementSteps(val kind: String?, val steps: Int) {
    override fun toString(): String {
        return "SetAchievementSteps(kind=$kind, steps=$steps)"
    }

    fun toJSONObject() = JSONObject().apply {
        putOpt("kind", kind)
        putOpt("steps", steps)
    }
}

data class UpdatedAchievement(val kind: String?, val achievementId: String?, val updateOccurred: Boolean, val currentState: String, val currentSteps: Int, val newlyUnlocked: Boolean) {
    override fun toString(): String {
        return "UpdatedAchievement(kind=$kind, achievementId=$achievementId, updateOccurred=$updateOccurred, currentState='$currentState', currentSteps=$currentSteps, newlyUnlocked=$newlyUnlocked)"
    }
}

data class UpdateMultipleAchievementResponse(val kind: String?, val updatedAchievements: ArrayList<UpdatedAchievement>) {
    override fun toString(): String {
        return "UpdateMultipleAchievementResponse(kind=$kind, updatedAchievements=$updatedAchievements)"
    }
}

fun ArrayList<Achievement>.toUpdateMultipleAchievements(): JSONObject {
    val updates: ArrayList<UpdateAchievement> = ArrayList()
    for (i in 0 until size) {
        val achievement = get(i)
        val updateAchievement = UpdateAchievement(
            "games#achievementUpdateRequest",
            achievement.achievementId,
            achievement.type,
            AchievementIncrement("games#GamesAchievementIncrement", achievement.currentSteps, achievement.achievementId.hashCode().toString()),
            SetAchievementSteps(" games#GamesAchievementSetStepsAtLeast", achievement.currentSteps)
        )
        updates.add(updateAchievement)
    }
    return UpdateMultipleAchievements("games#achievementUpdateMultipleRequest", updates).toJSONObject()
}

fun JSONObject.toUpdateMultipleResponse(): UpdateMultipleAchievementResponse {
    val list = ArrayList<UpdatedAchievement>()
    val items = optJSONArray("updatedAchievements")
    if (items != null) {
        for (i in 0..<items.length()) {
            val jsonObject = items.getJSONObject(i)
            val achievementDefinition = UpdatedAchievement(
                jsonObject.optString("kind"),
                jsonObject.optString("achievementId"),
                jsonObject.optBoolean("updateOccurred"),
                jsonObject.optString("currentState"),
                jsonObject.optInt("currentSteps"),
                jsonObject.optBoolean("newlyUnlocked"),
            )
            list.add(achievementDefinition)
        }
    }
    return UpdateMultipleAchievementResponse(optString("kind"), list)
}

fun JSONObject.toUnlockResponse(): AchievementUnlockResponse {
    return AchievementUnlockResponse(
        optString("kind"),
        optBoolean("newlyUnlocked")
    )
}

fun JSONObject.toIncrementResponse(): AchievementIncrementResponse {
    return AchievementIncrementResponse(
        optString("kind"),
        optInt("currentSteps"),
        optBoolean("newlyUnlocked")
    )
}

fun JSONObject.toRevealResponse(): AchievementRevealResponse {
    return AchievementRevealResponse(
        optString("kind"),
        optString("currentState")
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
        Achievement.AchievementType.TYPE_STANDARD
    } else Achievement.AchievementType.TYPE_INCREMENTAL
}

fun getAchievementState(state: String): Int {
    if (state == "UNLOCKED") {
        return Achievement.AchievementState.STATE_UNLOCKED
    }
    return if (state == "REVEALED") {
        Achievement.AchievementState.STATE_REVEALED
    } else Achievement.AchievementState.STATE_HIDDEN
}