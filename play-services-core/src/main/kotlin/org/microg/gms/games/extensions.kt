/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.Response.success
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.BuildConfig
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.games.CurrentPlayerInfoEntity
import com.google.android.gms.games.Player
import com.google.android.gms.games.PlayerColumns
import com.google.android.gms.games.PlayerEntity
import com.google.android.gms.games.PlayerLevel
import com.google.android.gms.games.PlayerLevelInfo
import com.google.android.gms.games.PlayerRelationshipInfoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import org.json.JSONObject
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthManager
import org.microg.gms.auth.consent.CONSENT_RESULT
import org.microg.gms.auth.signin.consentRequestOptions
import org.microg.gms.auth.signin.performConsentView
import org.microg.gms.auth.signin.performSignIn
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import org.microg.gms.common.Utils
import org.microg.gms.profile.Build
import org.microg.gms.settings.SettingsContract.CheckIn
import org.microg.gms.settings.SettingsContract.getSettings
import org.microg.gms.utils.singleInstanceOf
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

const val SERVICE_GAMES_LITE = "oauth2:https://www.googleapis.com/auth/games_lite"

const val ACTION_START_1P = "com.google.android.play.games.service.START_1P"
const val ACTION_VIEW_LEADERBOARDS = "com.google.android.gms.games.VIEW_LEADERBOARDS"
const val ACTION_VIEW_LEADERBOARDS_SCORES = "com.google.android.gms.games.VIEW_LEADERBOARD_SCORES"
const val ACTION_VIEW_ACHIEVEMENTS = "com.google.android.gms.games.VIEW_ACHIEVEMENTS"
const val ACTION_VIEW_SNAPSHOTS = "com.google.android.gms.games.SHOW_SELECT_SNAPSHOT"
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

const val EXTRA_MAX_SNAPSHOTS = "com.google.android.gms.games.MAX_SNAPSHOTS"
const val EXTRA_ALLOW_CREATE_SNAPSHOT = "com.google.android.gms.games.ALLOW_CREATE_SNAPSHOT"
const val EXTRA_TITLE = "com.google.android.gms.games.TITLE"
const val EXTRA_ALLOW_DELETE_SNAPSHOT = "com.google.android.gms.games.ALLOW_DELETE_SNAPSHOT"
const val EXTRA_SNAPSHOT_NEW = "com.google.android.gms.games.SNAPSHOT_NEW"
const val EXTRA_SNAPSHOT_METADATA = "com.google.android.gms.games.SNAPSHOT_METADATA"

const val EXTRA_LEADERBOARD_ID = "com.google.android.gms.games.LEADERBOARD_ID"
const val EXTRA_LEADERBOARD_TIME_SPAN = "com.google.android.gms.games.LEADERBOARD_TIME_SPAN"
const val EXTRA_LEADERBOARD_COLLECTION = "com.google.android.gms.games.LEADERBOARD_COLLECTION"

const val EXTRA_SHOW_CONNECTING_POPUP = "com.google.android.gms.games.key.showConnectingPopup"
const val EXTRA_ACCOUNT_KEY = "com.google.android.gms.games.ACCOUNT_KEY"
const val GAMES_PACKAGE_NAME = "com.google.android.play.games"

val List<Scope>.realScopes
    get() = if (any { it.scopeUri == Scopes.GAMES }) {
        this
    } else {
        this.toSet() + Scope(Scopes.GAMES_LITE)
    }.toList().sortedBy { it.scopeUri }

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

suspend fun requestGamesInfo(
    context: Context,
    method: Int,
    oauthToken: String,
    url: String,
    params: HashMap<String, String>?,
    requestBody: JSONObject? = null,
    queue: RequestQueue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }
): JSONObject = suspendCoroutine { continuation ->
    val uriBuilder = Uri.parse(url).buildUpon().apply {
        if (!params.isNullOrEmpty()) {
            for (key in params.keys) {
                appendQueryParameter(key, params[key])
            }
        }
    }
    queue.add(object : JsonObjectRequest(method, uriBuilder.build().toString(), requestBody, {
        continuation.resume(it)
    }, {
        continuation.resumeWithException(RuntimeException(it))
    }) {
        override fun getHeaders(): Map<String, String> = hashMapOf<String, String>().apply {
            put("Authorization", "OAuth $oauthToken")
        }
    })
}

suspend fun registerForGames(context: Context, account: Account, queue: RequestQueue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }) {
    val authManager = AuthManager(context, account.name, Constants.GMS_PACKAGE_NAME, "oauth2:${Scopes.GAMES_FIRSTPARTY}")
    authManager.setOauth2Foreground("1")
    val authToken = withContext(Dispatchers.IO) { authManager.requestAuthWithBackgroundResolution(false).auth }
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
    queue: RequestQueue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }
): Boolean {
    val realScopes = scopes.realScopes
    val authManager = AuthManager(context, account.name, packageName, "oauth2:${realScopes.joinToString(" ")}")
    if (realScopes.size == 1) authManager.setItCaveatTypes("2")
    if (permitted) {
        authManager.isPermitted = true
    } else {
        authManager.setTokenRequestOptions(consentRequestOptions)
    }
    var authResponse = withContext(Dispatchers.IO) { authManager.requestAuthWithBackgroundResolution(true) }
    if ("remote_consent" == authResponse.issueAdvice && authResponse.resolutionDataBase64 != null) {
        val consentResult = performConsentView(context, packageName, account, authResponse.resolutionDataBase64)
        if (consentResult == null) return false
        authManager.putDynamicFiled(CONSENT_RESULT, consentResult)
        authResponse = withContext(Dispatchers.IO) { authManager.requestAuthWithBackgroundResolution(true) }
    }
    if (authResponse.auth == null) return false
    if (authResponse.issueAdvice != "stored" || GamesConfigurationService.getPlayer(context, account) == null) {
        val result = try {
            fetchSelfPlayer(context, authResponse.auth, queue)
        } catch (e: Exception) {
            if (e is VolleyError) {
                val statusCode = e.networkResponse?.statusCode
                when (statusCode) {
                    404 -> {
                        try {
                            if (!GameProfileSettings.getAllowCreatePlayer(context)) {
                                return false
                            }
                            registerForGames(context, account, queue)
                            fetchSelfPlayer(context, authResponse.auth, queue)
                        } catch (e : Exception){
                            requestGameToken(context, account, scopes, authManager.isPermitted)?.let {
                                fetchSelfPlayer(context, it, queue)
                            } ?: return false
                        }
                    }
                    403 -> {
                        requestGameToken(context, account, scopes, authManager.isPermitted)?.let {
                            fetchSelfPlayer(context, it, queue)
                        } ?: return false
                    }
                    else -> throw e
                }
            } else {
                throw e
            }
        }
        val defaultAccount = GamesConfigurationService.getDefaultAccount(context, GAMES_PACKAGE_NAME)
        if (defaultAccount == null) {
            GamesConfigurationService.setDefaultAccount(context, GAMES_PACKAGE_NAME, account)
        }
        GamesConfigurationService.setPlayer(context, account, result.toString())
    }
    return true
}

suspend fun fetchSelfPlayer(
    context: Context,
    authToken: String,
    queue: RequestQueue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }
) = suspendCoroutine<JSONObject> { continuation ->
    queue.add(
        object : JsonObjectRequest(
            "https://www.googleapis.com/games/v1/players/me",
            { continuation.resume(it) },
            { continuation.resumeWithException(it) }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "Authorization" to "OAuth $authToken"
                )
            }
        }
    )
}

suspend fun requestGameToken(
    context: Context,
    account: Account,
    scopes: List<Scope> = arrayListOf(Scope(Scopes.GAMES_LITE)),
    isPermitted: Boolean = false,
): String? {
    val realScopes = scopes.realScopes
    val gameAuthManager = AuthManager(context, account.name, GAMES_PACKAGE_NAME, "oauth2:${realScopes.joinToString(" ")}")
    if (gameAuthManager.packageSignature == null) gameAuthManager.packageSignature = Constants.GMS_PACKAGE_SIGNATURE_SHA1
    gameAuthManager.isPermitted = isPermitted
    val authResponse = withContext(Dispatchers.IO) { gameAuthManager.requestAuth(true) }
    if (authResponse.auth == null) return null
    return authResponse.auth
}

suspend fun fetchAllSelfPlayers(context: Context): List<Pair<Account, String>> {
    val googleAccounts = AccountManager.get(context).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
    return googleAccounts.mapNotNull { account ->
        val playerStr = GamesConfigurationService.getPlayer(context, account)
        val playerJSONObject = if (playerStr == null) {
            withContext(Dispatchers.IO) {
                requestGameToken(context, account)?.let {
                    runCatching { fetchSelfPlayer(context, it) }.getOrNull()
                }
            }?.also { GamesConfigurationService.setPlayer(context, account, it.toString()) }
        } else {
            JSONObject(playerStr)
        }
        playerJSONObject?.toPlayer()?.displayName?.let { Pair(account, it) }
    }
}

suspend fun notifyGamePlayed(
    context: Context,
    packageName: String,
    account: Account,
    permitted: Boolean = false,
    queue: RequestQueue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }
) {
    val authManager = AuthManager(context, account.name, packageName, SERVICE_GAMES_LITE)
    if (permitted) authManager.isPermitted = true
    var authResponse = withContext(Dispatchers.IO) { authManager.requestAuthWithBackgroundResolution(true) }
    if (authResponse.auth == null) throw RuntimeException("authToken is null")
    return suspendCoroutine { continuation ->
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
}

class HeaderInterceptor(
    private val context: Context,
    private val oauthToken: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("authorization", "Bearer $oauthToken")
            .header("te", "trailers")
            .header("x-play-games-agent", createPlayGamesAgent())
            .header("x-device-id", LastCheckinInfo.read(context).androidId.toString(16))
            .header("user-agent", "grpc-java-okhttp/1.66.0-SNAPSHOT")
        val request = requestBuilder.build()
        return chain.proceed(request)
    }

    private fun createPlayGamesAgent(): String {
        var playGamesAgent =
            "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID};"
        playGamesAgent += context.packageName + "/" + BuildConfig.VERSION_CODE + ";"
        playGamesAgent += "FastParser/1.1; Games Android SDK/1.0-1052947;"
        playGamesAgent += "com.google.android.play.games/517322040; (gzip); Games module/242632000"
        return playGamesAgent
    }
}