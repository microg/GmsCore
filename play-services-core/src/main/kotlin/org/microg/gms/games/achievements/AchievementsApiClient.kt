/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.achievements

import android.content.Context
import com.android.volley.Request.Method
import org.microg.gms.games.requestGamesInfo
import java.util.Locale

/**
 * https://developers.google.com/games/services/web/api/rest#rest-resource:-achievementdefinitions
 * https://developers.google.com/games/services/web/api/rest#rest-resource:-achievements
 */
object AchievementsApiClient {

    /**
     * Lists all the achievement definitions for your application.
     */
    suspend fun requestGameAllAchievements(mContext: Context, oauthToken: String, pageToken: String? = null) = requestGamesInfo(mContext,
        Method.GET,
        oauthToken,
        "https://games.googleapis.com/games/v1/achievements",
        HashMap<String, String>().apply {
            put("language", Locale.getDefault().language)
            if (pageToken != null) {
                put("pageToken", pageToken)
            }
        }).toAllAchievementListResponse()

    /**
     * Lists the progress for all your application's achievements for the currently authenticated player.
     */
    suspend fun requestPlayerAllAchievements(mContext: Context, oauthToken: String, pageToken: String? = null) = requestGamesInfo(mContext,
        Method.GET,
        oauthToken,
        "https://games.googleapis.com/games/v1/players/me/achievements",
        HashMap<String, String>().apply {
            put("language", Locale.getDefault().language)
            if (pageToken != null) {
                put("pageToken", pageToken)
            }
        }).toPlayerAchievementListResponse()

    /**
     * Increments the steps of the achievement with the given ID for the currently authenticated player.
     */
    suspend fun incrementAchievement(mContext: Context, oauthToken: String, achievementId: String, numSteps: Int) = requestGamesInfo(
        mContext,
        Method.POST,
        oauthToken,
        "https://games.googleapis.com/games/v1/achievements/$achievementId/increment",
        HashMap<String, String>().apply {
            put("requestId", achievementId.hashCode().toString())
            put("stepsToIncrement", if (numSteps <= 0) "1" else numSteps.toString())
        }).toIncrementResponse()

    /**
     * Sets the state of the achievement with the given ID to REVEALED for the currently authenticated player.
     */
    suspend fun revealAchievement(mContext: Context, oauthToken: String, achievementId: String) = requestGamesInfo(
        mContext,
        Method.POST,
        oauthToken,
        "https://games.googleapis.com/games/v1/achievements/$achievementId/reveal",
        null
    ).toRevealResponse()

    /**
     * Sets the steps for the currently authenticated player towards unlocking an achievement.
     * If the steps parameter is less than the current number of steps that the player already gained for the achievement, the achievement is not modified.
     */
    suspend fun setStepsAtLeast(mContext: Context, oauthToken: String, achievementId: String, steps: Int) = requestGamesInfo(mContext,
        Method.POST,
        oauthToken,
        "https://games.googleapis.com/games/v1/achievements/$achievementId/setStepsAtLeast",
        HashMap<String, String>().apply {
            put("steps", if (steps <= 0) "1" else steps.toString())
        }).toIncrementResponse()

    /**
     * Unlocks this achievement for the currently authenticated player.
     */
    suspend fun unlockAchievement(mContext: Context, oauthToken: String, achievementId: String) = requestGamesInfo(
        mContext,
        Method.POST,
        oauthToken,
        "https://games.googleapis.com/games/v1/achievements/$achievementId/unlock",
        null
    ).toUnlockResponse()

}