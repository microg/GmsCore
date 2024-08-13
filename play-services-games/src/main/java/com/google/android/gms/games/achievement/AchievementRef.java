/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.achievement;

import static com.google.android.gms.games.achievement.AchievementColumns.*;

import android.annotation.SuppressLint;
import android.database.CharArrayBuffer;
import android.net.Uri;
import android.os.Parcel;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.data.DataBufferRef;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerRef;

@SuppressLint("ParcelCreator")
public class AchievementRef extends DataBufferRef implements Achievement {

    AchievementRef(DataHolder dataHolder, int dataRow) {
        super(dataHolder, dataRow);
        Log.d("AchievementRef", "AchievementRef: " + dataHolder);
    }

    @Override
    public final String getApplicationId() {
        return this.getString(DB_FIELD_EXTERNAL_GAME_ID);
    }

    @Override
    public String getAchievementId() {
        return this.getString(DB_FIELD_EXTERNAL_ACHIEVEMENT_ID);
    }

    @Override
    public int getCurrentSteps() {
        return this.getInteger(DB_FIELD_CURRENT_STEPS);
    }

    @Override
    public String getDescription() {
        return this.getString(DB_FIELD_DESCRIPTION);
    }

    @Override
    public void getDescription(CharArrayBuffer dataOut) {
        this.copyToBuffer(DB_FIELD_DESCRIPTION, dataOut);
    }

    @Override
    public void getFormattedCurrentSteps(CharArrayBuffer dataOut) {
        this.copyToBuffer(DB_FIELD_FORMATTED_CURRENT_STEPS, dataOut);
    }

    @Override
    public String getFormattedCurrentSteps() {
        return this.getString(DB_FIELD_FORMATTED_CURRENT_STEPS);
    }

    @Override
    public void getFormattedTotalSteps(CharArrayBuffer dataOut) {
        this.copyToBuffer(DB_FIELD_FORMATTED_TOTAL_STEPS, dataOut);
    }

    @Override
    public String getFormattedTotalSteps() {
        return this.getString(DB_FIELD_FORMATTED_TOTAL_STEPS);
    }

    @Override
    public long getLastUpdatedTimestamp() {
        return this.getLong(DB_FIELD_LAST_UPDATED_TIMESTAMP);
    }

    @Override
    public void getName(CharArrayBuffer dataOut) {
        this.copyToBuffer(DB_FIELD_NAME, dataOut);
    }

    @Override
    public String getName() {
        return this.getString(DB_FIELD_NAME);
    }

    @Override
    public Player getPlayer() {
        return getPlayerInternal();
    }

    @Nullable
    @Override
    public Player getPlayerInternal() {
        return this.hasNull(DB_FIELD_EXTERNAL_PLAYER_ID) ? null : new PlayerRef(dataHolder, dataRow);
    }

    @Override
    public Uri getRevealedImageUri() {
        return this.parseUri(DB_FIELD_REVEALED_ICON_IMAGE_URI);
    }

    @Override
    public final String getRevealedImageUrl() {
        return this.getString(DB_FIELD_REVEALED_ICON_IMAGE_URL);
    }

    @Override
    public int getState() {
        return this.getInteger(DB_FIELD_STATE);
    }

    @Override
    public int getTotalSteps() {
        return this.getInteger(DB_FIELD_TOTAL_STEPS);
    }

    @Override
    public int getType() {
        return this.getInteger(DB_FIELD_TYPE);
    }

    @Override
    public Uri getUnlockedImageUri() {
        return this.parseUri(DB_FIELD_UNLOCKED_ICON_IMAGE_URI);
    }

    @Override
    public final String getUnlockedImageUrl() {
        return this.getString(DB_FIELD_UNLOCKED_ICON_IMAGE_URL);
    }

    @Override
    public long getXpValue() {
        return this.getLong(DB_FIELD_INSTANCE_XP_VALUE);
    }

    @Override
    public float getRarityPercent() {
        return this.hasColumn(DB_FIELD_RARITY_PERCENT) && !this.hasNull(DB_FIELD_RARITY_PERCENT) ? this.getFloat(DB_FIELD_RARITY_PERCENT) : -1.0F;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        this.freeze().writeToParcel(dest, flags);
    }

    @Override
    public Achievement freeze() {
        return new AchievementEntity(this);
    }
}
