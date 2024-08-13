/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.games.achievements

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.android.gms.common.data.DataHolder
import com.google.android.gms.games.achievement.Achievement
import com.google.android.gms.games.achievement.AchievementBuffer
import com.google.android.gms.games.achievement.AchievementColumns.*
import com.google.android.gms.games.achievement.AchievementEntity

private const val TAG = "AchievementsDatabase"

private const val DB_NAME = "achievements.db"
private const val DB_VERSION = 2
private const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS achievements (" + "game_package_name TEXT," + "external_achievement_id TEXT," + "external_game_id TEXT," + "type INTEGER," + "name TEXT," + "description TEXT," + "unlocked_icon_image_uri TEXT," + "unlocked_icon_image_url TEXT," + "revealed_icon_image_uri TEXT," + "revealed_icon_image_url TEXT," + "total_steps INTEGER," + "formatted_total_steps TEXT," + "external_player_id TEXT," + "state INTEGER," + "current_steps INTEGER DEFAULT 0," + "formatted_current_steps TEXT," + "instance_xp_value LONGER," + "last_updated_timestamp LONGER," + "PRIMARY KEY (external_achievement_id));"

class AchievementsDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "onCreate: ")
        db.execSQL(CREATE_TABLE)
    }

    @Synchronized
    fun removeAchievements(packageName: String) {
        Log.d(TAG, "removeAchievements packageName: $packageName")
        writableDatabase.delete(DB_TABLE_ACHIEVEMENTS, "$DB_FIELD_GAME_PACKAGE_NAME LIKE ?", arrayOf<String>(packageName))
    }

    @Synchronized
    fun getAchievementsDataHolder(packageName: String): DataHolder? {
        Log.d(TAG, "getAchievementsDataHolder packageName: $packageName")
        writableDatabase.query(DB_TABLE_ACHIEVEMENTS, null, "$DB_FIELD_GAME_PACKAGE_NAME LIKE ?", arrayOf(packageName), null, null, "$DB_FIELD_LAST_UPDATED_TIMESTAMP ASC", null)?.use { cursor ->
            return DataHolder(cursor, 0, null)
        }
        return null
    }

    @Synchronized
    fun getAchievementsData(packageName: String): ArrayList<AchievementEntity>? {
        Log.d(TAG, "getAchievementsData packageName: $packageName")
        val achievementsDataHolder = getAchievementsDataHolder(packageName)
        if (achievementsDataHolder != null) {
            val data = ArrayList<AchievementEntity>()
            val achievementBuffer = AchievementBuffer(achievementsDataHolder)
            for (position in 0..<achievementsDataHolder.count) {
                val element = achievementBuffer.get(position).freeze() as AchievementEntity
                data.add(element)
            }
            return data
        }
        return null
    }

    @Synchronized
    fun getAchievementById(packageName: String, id: String?): Achievement? {
        writableDatabase.query(DB_TABLE_ACHIEVEMENTS, null, "$DB_FIELD_GAME_PACKAGE_NAME LIKE ? AND $DB_FIELD_EXTERNAL_ACHIEVEMENT_ID LIKE ?", arrayOf(packageName, id), null, null, null, null)?.use { cursor ->
            val dataHolder = DataHolder(cursor, 0, null)
            return AchievementBuffer(dataHolder).get(0).freeze()
        }
        return null
    }

    @Synchronized
    fun updateAchievementData(packageName: String, achievementId: String, contentValues: ContentValues): Int {
        return writableDatabase.update(DB_TABLE_ACHIEVEMENTS, contentValues, "$DB_FIELD_GAME_PACKAGE_NAME LIKE ? AND $DB_FIELD_EXTERNAL_ACHIEVEMENT_ID LIKE ?", arrayOf(packageName, achievementId))
    }

    @Synchronized
    fun insertAchievements(achievementList: ArrayList<AchievementEntity>, packageName: String?) {
        Log.d(TAG, "insertAchievements packageName: $packageName achievements: ${achievementList.size}")
        if (achievementList.isEmpty()) {
            return
        }
        runCatching {
            for (entity in achievementList) {
                val cv = ContentValues()
                cv.put(DB_FIELD_GAME_PACKAGE_NAME, packageName)
                cv.put(DB_FIELD_EXTERNAL_ACHIEVEMENT_ID, entity.achievementId)
                cv.put(DB_FIELD_EXTERNAL_GAME_ID, "")
                cv.put(DB_FIELD_TYPE, entity.type)
                cv.put(DB_FIELD_NAME, entity.name)
                cv.put(DB_FIELD_DESCRIPTION, entity.description)
                cv.put(DB_FIELD_UNLOCKED_ICON_IMAGE_URI, entity.unlockedImageUri.toString())
                cv.put(DB_FIELD_UNLOCKED_ICON_IMAGE_URL, entity.unlockedImageUrl)
                cv.put(DB_FIELD_REVEALED_ICON_IMAGE_URI, entity.revealedImageUri.toString())
                cv.put(DB_FIELD_REVEALED_ICON_IMAGE_URL, entity.revealedImageUrl)
                cv.put(DB_FIELD_TOTAL_STEPS, entity.totalSteps)
                cv.put(DB_FIELD_FORMATTED_TOTAL_STEPS, entity.formattedTotalSteps)
                cv.put(DB_FIELD_EXTERNAL_PLAYER_ID, "")
                cv.put(DB_FIELD_STATE, entity.state)
                cv.put(DB_FIELD_CURRENT_STEPS, 0)
                cv.put(DB_FIELD_FORMATTED_CURRENT_STEPS, "0")
                cv.put(DB_FIELD_INSTANCE_XP_VALUE, entity.xpValue)
                cv.put(DB_FIELD_LAST_UPDATED_TIMESTAMP, entity.lastUpdatedTimestamp)
                writableDatabase.insertWithOnConflict(DB_TABLE_ACHIEVEMENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            close()
        }.onFailure {
            Log.d(TAG, "insertAchievements error: ${it.localizedMessage}")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
//        throw IllegalStateException("Upgrades not supported")
    }

}
