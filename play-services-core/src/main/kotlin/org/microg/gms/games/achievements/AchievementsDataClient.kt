/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.achievements

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.data.DataHolder
import com.google.android.gms.games.Games
import com.google.android.gms.games.achievement.Achievement
import com.google.android.gms.games.achievement.AchievementBuffer
import com.google.android.gms.games.achievement.AchievementColumns.DB_FIELD_LAST_UPDATED_TIMESTAMP
import com.google.android.gms.games.achievement.AchievementColumns.DB_FIELD_STATE
import com.google.android.gms.games.achievement.AchievementColumns.DB_FIELD_TYPE
import com.google.android.gms.games.achievement.AchievementEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class AchievementsDataClient(val context: Context) {

    private val database by lazy { AchievementsDatabase(context) }
    private val achievementsDataLoaded = AtomicBoolean(false)
    private val activeMutexLock = Mutex()

    suspend fun loadAchievementsData(packageName: String, account: Account, forceReload: Boolean): DataHolder? {
        val deferred = activeMutexLock.withLock { CompletableDeferred<DataHolder?>() }
        if (achievementsDataLoaded.compareAndSet(false, true)) {
            withContext(Dispatchers.IO) {
                val oauthToken =  GoogleAuthUtil.getToken(context, account, Games.SERVICE_GAMES_LITE, packageName)
                Log.d(TAG, "loadAchievementsData executing by $packageName")
                val startTime = System.currentTimeMillis()
                if (forceReload) {
                    database.removeAchievements(packageName)
                }

                val allAchievements = database.getAchievementsData(packageName) ?: ArrayList()

                Log.d(TAG, "loadAchievementsData: allAchievements " + allAchievements.size)

                val playerAchievements = ArrayList<PlayerAchievement>()
                var playerPageToken: String? = null
                do {
                    val response =
                        AchievementsApiClient.requestPlayerAllAchievements(context, oauthToken, playerPageToken)
                    playerAchievements.addAll(response.items)
                    playerPageToken = response.nextPageToken
                } while (!playerPageToken.isNullOrEmpty())

                Log.d(TAG, "loadAchievementsData: playerAchievements " + playerAchievements.size)

                if (allAchievements.size != playerAchievements.size) {
                    database.removeAchievements(packageName)
                    allAchievements.clear()
                    var pageToken: String? = null
                    do {
                        val response = AchievementsApiClient.requestGameAllAchievements(context, oauthToken, pageToken)
                        response.items.forEach { allAchievements.add(it.toAchievementEntity()) }
                        pageToken = response.nextPageToken
                    } while (!pageToken.isNullOrEmpty())
                }

                if (playerAchievements.isNotEmpty()) {
                    for (playerAchievement in playerAchievements) {
                        allAchievements.find { it.achievementId == playerAchievement.id }?.apply {
                            currentSteps = playerAchievement.currentSteps
                            state = getAchievementState(playerAchievement.achievementState)
                            formattedCurrentSteps =
                                if (playerAchievement.formattedCurrentStepsString.isNullOrEmpty()) formattedCurrentSteps else playerAchievement.formattedCurrentStepsString
                            lastUpdatedTimestamp =
                                if (playerAchievement.lastUpdatedTimestamp.isNullOrEmpty()) lastUpdatedTimestamp else playerAchievement.lastUpdatedTimestamp.toLong()
                            xpValue =
                                if (playerAchievement.experiencePoints.isNullOrEmpty()) xpValue else playerAchievement.experiencePoints.toLong()
                        }
                    }
                }

                database.insertAchievements(allAchievements, packageName)
                Log.d(TAG, "loadAchievementsData: " + allAchievements.size)
                deferred.complete(database.getAchievementsDataHolder(packageName))
                achievementsDataLoaded.set(false)
                Log.d(TAG, "loadAchievementsData end cost: ${System.currentTimeMillis() - startTime}")
            }
        } else {
            Log.d(TAG, "loadAchievementsData has already been executed")
            return null
        }
        return deferred.await()
    }

    suspend fun setAchievementSteps(account: Account, packageName: String, achievementId: String, numStep: Int) =
        withContext(Dispatchers.IO) {
            if (numStep < 1 || achievementsDataLoaded.get()) {
                Log.d(
                    TAG,
                    "setAchievementSteps: The steps field has an invalid value (0). The allowed range is between 1 and 2147483647."
                )
                return@withContext -1
            }
            database.getAchievementsDataHolder(packageName) ?: return@withContext -1

            val oauthToken =  GoogleAuthUtil.getToken(context, account, Games.SERVICE_GAMES_LITE, packageName)
            val response = AchievementsApiClient.setStepsAtLeast(context, oauthToken, achievementId, numStep)
            Log.d(TAG, "setAchievementSteps: setStepsAtLeast: $response")
            val contentValues = ContentValues()
            if (response.newlyUnlocked) {
                contentValues.put(DB_FIELD_STATE, Achievement.AchievementState.STATE_UNLOCKED)
            }
            contentValues.put(DB_FIELD_TYPE, if (response.currentSteps == 0) numStep else response.currentSteps)
            contentValues.put(DB_FIELD_LAST_UPDATED_TIMESTAMP, System.currentTimeMillis())
            database.updateAchievementData(packageName, achievementId, contentValues)
        }

    suspend fun revealAchievement(account: Account, packageName: String, achievementId: String) =
        withContext(Dispatchers.IO) {
            database.getAchievementsDataHolder(packageName) ?: return@withContext -1
            val oauthToken =  GoogleAuthUtil.getToken(context, account, Games.SERVICE_GAMES_LITE, packageName)
            val response = AchievementsApiClient.revealAchievement(context, oauthToken, achievementId)
            Log.d(TAG, "revealAchievement: $response")
            val contentValues = ContentValues()
            contentValues.put(DB_FIELD_STATE, getAchievementState(response.currentState))
            contentValues.put(DB_FIELD_LAST_UPDATED_TIMESTAMP, System.currentTimeMillis())
            database.updateAchievementData(packageName, achievementId, contentValues)
        }

    suspend fun unlockAchievement(account: Account, packageName: String, achievementId: String) =
        withContext(Dispatchers.IO) {
            database.getAchievementsDataHolder(packageName) ?: return@withContext -1
            val oauthToken =  GoogleAuthUtil.getToken(context, account, Games.SERVICE_GAMES_LITE, packageName)
            val response = AchievementsApiClient.unlockAchievement(context, oauthToken, achievementId)
            Log.d(TAG, "unlockAchievement: $response")
            val contentValues = ContentValues()
            if (response.newlyUnlocked) {
                contentValues.put(DB_FIELD_STATE, Achievement.AchievementState.STATE_UNLOCKED)
            }
            contentValues.put(DB_FIELD_LAST_UPDATED_TIMESTAMP, System.currentTimeMillis())
            database.updateAchievementData(packageName, achievementId, contentValues)
        }

    suspend fun incrementAchievement(account: Account, packageName: String, achievementId: String, numStep: Int) =
        withContext(Dispatchers.IO) {
            database.getAchievementsDataHolder(packageName) ?: return@withContext -1
            val oauthToken =  GoogleAuthUtil.getToken(context, account, Games.SERVICE_GAMES_LITE, packageName)
            val response = AchievementsApiClient.incrementAchievement(context, oauthToken, achievementId, numStep)
            Log.d(TAG, "incrementAchievement: $response")
            val contentValues = ContentValues()
            if (response.newlyUnlocked) {
                contentValues.put(DB_FIELD_STATE, Achievement.AchievementState.STATE_UNLOCKED)
            }
            contentValues.put(DB_FIELD_TYPE, if (response.currentSteps == 0) numStep else response.currentSteps)
            contentValues.put(DB_FIELD_LAST_UPDATED_TIMESTAMP, System.currentTimeMillis())
            database.updateAchievementData(packageName, achievementId, contentValues)
        }

    suspend fun loadAchievements(packageName: String, account: Account, forceReload: Boolean): ArrayList<AchievementEntity> {
        val allAchievements = database.getAchievementsData(packageName) ?: arrayListOf()
        if (allAchievements.isEmpty()) {
            achievementsDataLoaded.set(false)
            val dataHolder = loadAchievementsData(packageName, account, forceReload)
            if (dataHolder != null) {
                val achievementBuffer = AchievementBuffer(dataHolder)
                for (position in 0..<dataHolder.count) {
                    val element = achievementBuffer.get(position).freeze() as AchievementEntity
                    allAchievements.add(element)
                }
            }
        }
        return allAchievements
    }

    fun release() {
        achievementsDataLoaded.set(false)
        database.close()
    }

    companion object {
        private const val TAG = "AchievementsDataClient"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: AchievementsDataClient? = null
        fun get(context: Context): AchievementsDataClient = instance ?: synchronized(this) {
            instance ?: AchievementsDataClient(context.applicationContext).also { instance = it }
        }
    }
}