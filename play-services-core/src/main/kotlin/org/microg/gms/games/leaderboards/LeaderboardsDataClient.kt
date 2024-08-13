/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.leaderboards

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.games.Games
import com.google.android.gms.games.leaderboard.Leaderboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LeaderboardsDataClient(val context: Context) {

    suspend fun loadLeaderboards(packageName: String, account: Account) = withContext(Dispatchers.IO) {
        val token = GoogleAuthUtil.getToken(context, account, Games.SERVICE_GAMES_LITE, packageName)
        var playerPageToken: String? = null
        val leaderboards = arrayListOf<LeaderboardDefinition>()
        do {
            val response = runCatching {
                LeaderboardsApiClient.requestAllLeaderboards(
                    context, token, playerPageToken
                )
            }.getOrNull()
            response?.items?.let { leaderboards.addAll(it) }
            playerPageToken = response?.nextPageToken
        } while (!playerPageToken.isNullOrEmpty())
        return@withContext leaderboards
    }

    suspend fun getLeaderboardById(packageName: String, account: Account, leaderboardId:String) = withContext(Dispatchers.IO) {
        val token = GoogleAuthUtil.getToken(context, account, Games.SERVICE_GAMES_LITE, packageName)
        return@withContext runCatching { LeaderboardsApiClient.getLeaderboardById(context, token, leaderboardId) }.getOrNull()
    }

    suspend fun getLeaderboardScoresById(
        leaderboardId: String, packageName: String, account: Account
    ) = withContext(Dispatchers.IO) {
        val token = GoogleAuthUtil.getToken(context, account, Games.SERVICE_GAMES_LITE, packageName)
        var playerPageToken: String? = null
        val leaderboardScores = arrayListOf<LeaderboardEntry>()
        do {
            val response = runCatching {
                LeaderboardsApiClient.requestLeaderboardScoresById(
                    context, token, leaderboardId, playerPageToken
                )
            }.getOrNull()
            response?.items?.let { leaderboardScores.addAll(it) }
            playerPageToken = response?.nextPageToken
        } while (!playerPageToken.isNullOrEmpty() && leaderboardScores.size < 60)
        return@withContext leaderboardScores
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: LeaderboardsDataClient? = null
        fun get(context: Context): LeaderboardsDataClient = instance ?: synchronized(this) {
            instance ?: LeaderboardsDataClient(context.applicationContext).also { instance = it }
        }
    }
}