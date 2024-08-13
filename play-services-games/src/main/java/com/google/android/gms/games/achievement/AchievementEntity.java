/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.achievement;

import android.database.CharArrayBuffer;
import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.util.DataUtils;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerEntity;

@SafeParcelable.Class
public class AchievementEntity extends AbstractSafeParcelable implements Achievement {

    @Field(value = 1, getterName = "getAchievementId")
    private String id;

    @Field(value = 2, getterName = "getType")
    private int type;

    @Field(value = 3, getterName = "getName")
    private String name;

    @Field(value = 4, getterName = "getDescription")
    private String description;

    @Field(value = 5, getterName = "getUnlockedImageUri")
    private Uri unlockedImageUri;

    @Field(value = 6, getterName = "getUnlockedImageUrl")
    private String unlockedImageUrl;

    @Field(value = 7, getterName = "getRevealedImageUri")
    private Uri revealedImageUri;

    @Field(value = 8, getterName = "getRevealedImageUrl")
    private String revealedImageUrl;

    @Field(value = 9, getterName = "getTotalSteps")
    private int totalSteps;

    @Field(value = 10, getterName = "getFormattedTotalSteps")
    private String formattedTotalSteps;

    @Field(value = 11, getterName = "getPlayer")
    private PlayerEntity player;

    @Field(value = 12, getterName = "getState")
    private int state;

    @Field(value = 13, getterName = "getCurrentSteps")
    private int currentSteps;

    @Field(value = 14, getterName = "getFormattedCurrentSteps")
    private String formattedCurrentSteps;

    @Field(value = 15, getterName = "getLastUpdatedTimestamp")
    private long lastUpdatedTimestamp;

    @Field(value = 16, getterName = "getXpValue")
    private long xpValue;

    @Field(value = 17, getterName = "getRarityPercent")
    private float rarityPercent;

    @Field(value = 18, getterName = "getApplicationId")
    private String applicationId;

    public AchievementEntity(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public AchievementEntity(Achievement achievement) {
        this.id = achievement.getAchievementId();
        this.type = achievement.getType();
        this.name = achievement.getName();
        this.description = achievement.getDescription();
        this.unlockedImageUri = achievement.getUnlockedImageUri();
        this.unlockedImageUrl = achievement.getUnlockedImageUrl();
        this.revealedImageUri = achievement.getRevealedImageUri();
        this.revealedImageUrl = achievement.getRevealedImageUrl();
        if (achievement.getPlayerInternal() != null) {
            player = (PlayerEntity) achievement.getPlayerInternal().freeze();
        } else {
            player = null;
        }
        this.state = achievement.getState();
        this.lastUpdatedTimestamp = achievement.getLastUpdatedTimestamp();
        this.xpValue = achievement.getXpValue();
        this.rarityPercent = achievement.getRarityPercent();
        this.applicationId = achievement.getApplicationId();
        if (achievement.getType() == AchievementType.TYPE_INCREMENTAL) {
            this.totalSteps = achievement.getTotalSteps();
            this.formattedTotalSteps = achievement.getFormattedTotalSteps();
            this.currentSteps = achievement.getCurrentSteps();
            this.formattedCurrentSteps = achievement.getFormattedCurrentSteps();
        } else {
            this.totalSteps = 0;
            this.formattedTotalSteps = null;
            this.currentSteps = 0;
            this.formattedCurrentSteps = null;
        }
    }

    @Constructor
    public AchievementEntity(@Param(value = 1) String id, @Param(value = 2) int type, @Param(value = 3) String name, @Param(value = 4) String description, @Param(value = 5) Uri unlockedImageUri, @Param(value = 6) String unlockedImageUrl, @Param(value = 7) Uri revealedImageUri, @Param(value = 8) String revealedImageUrl, @Param(value = 9) int totalSteps, @Param(value = 10) String formattedTotalSteps, @Param(value = 11) @Nullable PlayerEntity player, @Param(value = 12) int state, @Param(value = 13) int currentSteps, @Param(value = 14) String formattedCurrentSteps, @Param(value = 15) long lastUpdatedTimestamp, @Param(value = 16) long xpValue, @Param(value = 17) float rarityPercent, @Param(value = 18) String applicationId) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.unlockedImageUri = unlockedImageUri;
        this.unlockedImageUrl = unlockedImageUrl;
        this.revealedImageUri = revealedImageUri;
        this.revealedImageUrl = revealedImageUrl;
        this.totalSteps = totalSteps;
        this.formattedTotalSteps = formattedTotalSteps;
        this.player = player;
        this.state = state;
        this.currentSteps = currentSteps;
        this.formattedCurrentSteps = formattedCurrentSteps;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        this.xpValue = xpValue;
        this.rarityPercent = rarityPercent;
        this.applicationId = applicationId;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static SafeParcelableCreatorAndWriter<AchievementEntity> CREATOR = findCreator(AchievementEntity.class);

    @Override
    public float getRarityPercent() {
        return rarityPercent;
    }

    @Override
    public String getApplicationId() {
        return applicationId;
    }

    @Override
    public Achievement freeze() {
        return this;
    }

    @Override
    public boolean isDataValid() {
        return true;
    }

    @Override
    public String getAchievementId() {
        return id;
    }

    @Override
    public int getCurrentSteps() {
        return currentSteps;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void getDescription(CharArrayBuffer dataOut) {
        DataUtils.copyStringToBuffer(this.description, dataOut);
    }

    @Override
    public void getFormattedCurrentSteps(CharArrayBuffer dataOut) {
        DataUtils.copyStringToBuffer(this.formattedCurrentSteps, dataOut);
    }

    @Override
    public String getFormattedCurrentSteps() {
        return formattedCurrentSteps;
    }

    @Override
    public void getFormattedTotalSteps(CharArrayBuffer dataOut) {
        DataUtils.copyStringToBuffer(this.formattedTotalSteps, dataOut);
    }

    @Override
    public String getFormattedTotalSteps() {
        return formattedTotalSteps;
    }

    @Override
    public long getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    @Override
    public void getName(CharArrayBuffer dataOut) {
        DataUtils.copyStringToBuffer(this.name, dataOut);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PlayerEntity getPlayer() {
        return player;
    }

    @Nullable
    @Override
    public Player getPlayerInternal() {
        return player;
    }

    @Override
    public Uri getRevealedImageUri() {
        return revealedImageUri;
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public int getTotalSteps() {
        return totalSteps;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public Uri getUnlockedImageUri() {
        return unlockedImageUri;
    }

    @Override
    public long getXpValue() {
        return xpValue;
    }

    @Override
    public String getRevealedImageUrl() {
        return revealedImageUrl;
    }

    @Override
    public String getUnlockedImageUrl() {
        return unlockedImageUrl;
    }

    @Override
    public String toString() {
        return "AchievementEntity{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", unlockedImageUri=" + unlockedImageUri +
                ", unlockedImageUrl='" + unlockedImageUrl + '\'' +
                ", revealedImageUri=" + revealedImageUri +
                ", revealedImageUrl='" + revealedImageUrl + '\'' +
                ", totalSteps=" + totalSteps +
                ", formattedTotalSteps='" + formattedTotalSteps + '\'' +
                ", player=" + player +
                ", state=" + state +
                ", currentSteps=" + currentSteps +
                ", formattedCurrentSteps='" + formattedCurrentSteps + '\'' +
                ", lastUpdatedTimestamp=" + lastUpdatedTimestamp +
                ", xpValue=" + xpValue +
                ", rarityPercent=" + rarityPercent +
                ", applicationId='" + applicationId + '\'' +
                '}';
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUnlockedImageUri(Uri unlockedImageUri) {
        this.unlockedImageUri = unlockedImageUri;
    }

    public void setUnlockedImageUrl(String unlockedImageUrl) {
        this.unlockedImageUrl = unlockedImageUrl;
    }

    public void setRevealedImageUri(Uri revealedImageUri) {
        this.revealedImageUri = revealedImageUri;
    }

    public void setRevealedImageUrl(String revealedImageUrl) {
        this.revealedImageUrl = revealedImageUrl;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public void setFormattedTotalSteps(String formattedTotalSteps) {
        this.formattedTotalSteps = formattedTotalSteps;
    }

    public void setPlayer(PlayerEntity player) {
        this.player = player;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setCurrentSteps(int currentSteps) {
        this.currentSteps = currentSteps;
    }

    public void setFormattedCurrentSteps(String formattedCurrentSteps) {
        this.formattedCurrentSteps = formattedCurrentSteps;
    }

    public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    public void setXpValue(long xpValue) {
        this.xpValue = xpValue;
    }

    public void setRarityPercent(float rarityPercent) {
        this.rarityPercent = rarityPercent;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
}
