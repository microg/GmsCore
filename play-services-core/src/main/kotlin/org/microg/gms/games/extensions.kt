/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games

import android.accounts.Account
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import com.android.volley.*
import com.android.volley.Response.success
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.games.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.microg.gms.auth.AuthManager
import org.microg.gms.common.Constants
import org.microg.gms.common.Utils
import org.microg.gms.settings.SettingsContract.CheckIn
import org.microg.gms.settings.SettingsContract.getSettings
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


const val ACTION_START_1P = "com.google.android.play.games.service.START_1P"
const val ACTION_VIEW_LEADERBOARDS = "com.google.android.gms.games.VIEW_LEADERBOARDS"
const val ACTION_VIEW_ACHIEVEMENTS = "com.google.android.gms.games.VIEW_ACHIEVEMENTS"
const val ACTION_PLAYER_SEARCH = "com.google.android.gms.games.PLAYER_SEARCH"
const val ACTION_VIEW_PROFILE = "com.google.android.gms.games.VIEW_PROFILE"
const val ACTION_ADD_FRIEND = "com.google.android.gms.games.ADD_FRIEND"

const val EXTRA_GAME_PACKAGE_NAME = "com.google.android.gms.games.GAME_PACKAGE_NAME"
const val EXTRA_GAME_ID = "com.google.android.gms.games.GAME_ID"
const val EXTRA_PLAYER = "com.google.android.gms.games.PLAYER"
const val EXTRA_PLAYER_ID = "com.google.android.gms.games.PLAYER_ID"
const val EXTRA_IS_SELF = "com.google.android.gms.games.IS_SELF"
const val EXTRA_ACCOUNT = "com.google.android.gms.games.ACCOUNT"
const val EXTRA_SCOPES = "com.google.android.gms.games.SCOPES"
const val EXTRA_POPUP_GRAVITY = "com.google.android.gms.games.key.connectingPopupGravity"
const val EXTRA_SELF_IN_GAME_NAME = "com.google.android.gms.games.EXTRA_SELF_IN_GAME_NAME"
const val EXTRA_OTHER_PLAYER_IN_GAME_NAME = "com.google.android.gms.games.EXTRA_OTHER_PLAYER_IN_GAME_NAME"

const val GAMES_PACKAGE_NAME = "com.google.android.play.games"

fun PlayerEntity.toContentValues(): ContentValues = contentValuesOf(
    PlayerColumns.externalPlayerId to playerId,
    PlayerColumns.profileName to displayName,
    PlayerColumns.gamerTag to gamerTag,
    PlayerColumns.realName to name,
    PlayerColumns.profileIconImageUri to iconImageUri?.toString(),
    PlayerColumns.profileIconImageUrl to iconImageUrl,
    PlayerColumns.profileHiResImageUri to hiResImageUri?.toString(),
    PlayerColumns.profileHiResImageUrl to hiResImageUrl,
    PlayerColumns.bannerImageLandscapeUri to bannerImageLandscapeUri?.toString(),
    PlayerColumns.bannerImageLandscapeUrl to bannerImageLandscapeUrl,
    PlayerColumns.bannerImagePortraitUri to bannerImagePortraitUri?.toString(),
    PlayerColumns.bannerImagePortraitUrl to bannerImagePortraitUrl,
    PlayerColumns.lastUpdated to retrievedTimestamp,
    PlayerColumns.isInCircles to isInCircles,
    PlayerColumns.playedWithTimestamp to lastPlayedWithTimestamp,
    PlayerColumns.playerTitle to title,
    PlayerColumns.isProfileVisible to isProfileVisible,
    PlayerColumns.hasDebugAccess to hasDebugAccess,
    PlayerColumns.gamerFriendStatus to 0,
    PlayerColumns.gamerFriendUpdateTimestamp to 0L,
    PlayerColumns.isMuted to false,
    PlayerColumns.totalUnlockedAchievements to totalUnlockedAchievement,
    PlayerColumns.alwaysAutoSignIn to isAlwaysAutoSignIn,
    PlayerColumns.hasAllPublicAcls to isProfileVisible,

    PlayerColumns.currentLevel to levelInfo?.currentLevel?.levelNumber,
    PlayerColumns.currentLevelMinXp to levelInfo?.currentLevel?.minXp,
    PlayerColumns.currentLevelMaxXp to levelInfo?.currentLevel?.maxXp,
    PlayerColumns.nextLevel to levelInfo?.nextLevel?.levelNumber,
    PlayerColumns.nextLevelMaxXp to levelInfo?.nextLevel?.maxXp,
    PlayerColumns.lastLevelUpTimestamp to (levelInfo?.lastLevelUpTimestamp ?: -1),
    PlayerColumns.currentXpTotal to (levelInfo?.currentXpTotal ?: -1L),

    PlayerColumns.mostRecentExternalGameId to mostRecentGameInfo?.gameId,
    PlayerColumns.mostRecentGameName to mostRecentGameInfo?.gameName,
    PlayerColumns.mostRecentActivityTimestamp to mostRecentGameInfo?.activityTimestampMillis,
    PlayerColumns.mostRecentGameIconUri to mostRecentGameInfo?.gameIconImageUri?.toString(),
    PlayerColumns.mostRecentGameHiResUri to mostRecentGameInfo?.gameHiResImageUri?.toString(),
    PlayerColumns.mostRecentGameFeaturedUri to mostRecentGameInfo?.gameFeaturedImageUri?.toString(),

    PlayerColumns.playTogetherFriendStatus to relationshipInfo?.friendStatus,
    PlayerColumns.playTogetherNickname to (relationshipInfo as? PlayerRelationshipInfoEntity)?.nickname,
    PlayerColumns.playTogetherInvitationNickname to (relationshipInfo as? PlayerRelationshipInfoEntity)?.invitationNickname,
    PlayerColumns.nicknameAbuseReportToken to (relationshipInfo as? PlayerRelationshipInfoEntity)?.nicknameAbuseReportToken,

    PlayerColumns.friendsListVisibility to currentPlayerInfo?.friendsListVisibilityStatus
)

fun JSONObject.toPlayer() = PlayerEntity(
    getString("playerId"),
    getString("displayName"),
    optString("avatarImageUrl").takeIf { it.isNotBlank() }?.toUri(),
    optString("avatarImageUrl").takeIf { it.isNotBlank() }?.toUri(),
    System.currentTimeMillis(),
    0, 0,
    optString("avatarImageUrl").takeIf { it.isNotBlank() },
    optString("avatarImageUrl").takeIf { it.isNotBlank() },
    getString("title"),
    null,
    getJSONObject("experienceInfo")?.let {
        PlayerLevelInfo(
            it.optLong("currentExperiencePoints"),
            0,
            it.getJSONObject("currentLevel")?.let {
                PlayerLevel(it.getInt("level"), it.optLong("minExperiencePoints"), it.optLong("maxExperiencePoints"))
            },
            it.getJSONObject("nextLevel")?.let {
                PlayerLevel(it.getInt("level"), it.optLong("minExperiencePoints"), it.optLong("maxExperiencePoints"))
            }
        )
    },
    optJSONObject("profileSettings")?.optBoolean("profileVisible") ?: false,
    false,
    null, null,
    optString("bannerUrlLandscape").takeIf { it.isNotBlank() }?.toUri(),
    optString("bannerUrlLandscape").takeIf { it.isNotBlank() },
    optString("bannerUrlPortrait").takeIf { it.isNotBlank() }?.toUri(),
    optString("bannerUrlPortrait").takeIf { it.isNotBlank() },
    0, null,
    optJSONObject("profileSettings")?.optString("friendsListVisibility")?.takeIf { it.isNotBlank() }?.let {
        CurrentPlayerInfoEntity(
            when (it) {
                "VISIBLE" -> Player.FriendsListVisibilityStatus.VISIBLE
                "REQUEST_REQUIRED" -> Player.FriendsListVisibilityStatus.REQUEST_REQUIRED
                "FEATURE_UNAVAILABLE" -> Player.FriendsListVisibilityStatus.FEATURE_UNAVAILABLE
                else -> Player.FriendsListVisibilityStatus.UNKNOWN
            }
        )
    },
    false,
    null
)

suspend fun registerForGames(context: Context, account: Account, queue: RequestQueue = Volley.newRequestQueue(context)) {
    val authManager = AuthManager(context, account.name, Constants.GMS_PACKAGE_NAME, "oauth2:${Scopes.GAMES_FIRSTPARTY}")
    authManager.setOauth2Foreground("1")
    val authToken = withContext(Dispatchers.IO) { authManager.requestAuth(false).auth }
    val androidId = getSettings(context, CheckIn.getContentUri(context), arrayOf(CheckIn.ANDROID_ID)) { cursor: Cursor -> cursor.getLong(0) }
    val result = suspendCoroutine<JSONObject> { continuation ->
        queue.add(
            object : JsonObjectRequest(
                "https://www.googleapis.com/games/v1whitelisted/players/me/profilesettings?requestRandomGamerTag=true&language=${Utils.getLocale(context)}",
                { continuation.resume(it) },
                { continuation.resumeWithException(RuntimeException(it)) }) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(
                        "Authorization" to "OAuth $authToken",
                        "X-Device-ID" to androidId.toString(16)
                    )
                }
            }
        )
    }
    suspendCoroutine<JSONObject> { continuation ->
        queue.add(
            object : JsonObjectRequest(
                Method.PUT,
                "https://www.googleapis.com/games/v1whitelisted/players/me/profilesettings?language=${Utils.getLocale(context)}",
                JSONObject().apply {
                    put("alwaysAutoSignIn", false)
                    put("autoSignIn", false)
                    put("gamerTagIsDefault", true)
                    put("gamerTagIsExplicitlySet", false)
                    put("gamesLitePlayerStatsEnabled", false)
                    put("profileDiscoverableViaGoogleAccount", false)
                    put("profileVisibilityWasChosenByPlayer", false)
                    put("profileVisible", false)
                    put("gamerTag", result.getString("gamerTag"))
                    if (result.has("stockGamerAvatarUrl")) put("stockGamerAvatarUrl", result.getString("stockGamerAvatarUrl"))
                },
                { continuation.resume(it) },
                { continuation.resumeWithException(RuntimeException(it)) }) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf(
                        "Content-Type" to "application/json; charset=utf-8",
                        "Authorization" to "OAuth $authToken",
                        "X-Device-ID" to androidId.toString(16)
                    )
                }
            }
        )
    }
}

suspend fun performGamesSignIn(
    context: Context,
    packageName: String,
    account: Account,
    permitted: Boolean = false,
    scopes: List<Scope> = emptyList(),
    queue: RequestQueue = Volley.newRequestQueue(context)
): Boolean {
    val scopes = (scopes.toSet() + Scope(Scopes.GAMES_LITE)).toList().sortedBy { it.scopeUri }
    val authManager = AuthManager(context, account.name, packageName, "oauth2:${scopes.joinToString(" ")}")
    if (scopes.size == 1) authManager.setItCaveatTypes("2")
    if (permitted) authManager.isPermitted = true
    val authResponse = withContext(Dispatchers.IO) { authManager.requestAuth(true) }
    if (authResponse.auth == null) return false
    if (authResponse.issueAdvice != "stored" || GamesConfigurationService.getPlayer(context, packageName, account) == null) {
        suspend fun fetchSelfPlayer() = suspendCoroutine<JSONObject> { continuation ->
            queue.add(
                object : JsonObjectRequest(
                    "https://www.googleapis.com/games/v1/players/me",
                    { continuation.resume(it) },
                    { continuation.resumeWithException(it) }) {
                    override fun getHeaders(): MutableMap<String, String> {
                        return mutableMapOf(
                            "Authorization" to "OAuth ${authResponse.auth}"
                        )
                    }
                }
            )
        }

        val result = try {
            fetchSelfPlayer()
        } catch (e: Exception) {
            if (e is VolleyError && e.networkResponse?.statusCode == 404) {
                registerForGames(context, account, queue)
                fetchSelfPlayer()
            } else {
                throw e
            }
        }
        GamesConfigurationService.setPlayer(context, packageName, account, result.toString())
        if (packageName != GAMES_PACKAGE_NAME) {
            try {
                suspendCoroutine { continuation ->
                    queue.add(object : Request<Unit>(Method.POST, "https://www.googleapis.com/games/v1/applications/played", {
                        continuation.resumeWithException(it)
                    }) {
                        override fun parseNetworkResponse(response: NetworkResponse): Response<Unit> {
                            if (response.statusCode == 204) return success(Unit, null)
                            return Response.error(VolleyError(response))
                        }

                        override fun deliverResponse(response: Unit) {
                            continuation.resume(response)
                        }

                        override fun getHeaders(): MutableMap<String, String> {
                            return mutableMapOf(
                                "Authorization" to "OAuth ${authResponse.auth}"
                            )
                        }
                    })
                }
            } catch (ignored: Exception) {
            }
        }
    }
    return true
}