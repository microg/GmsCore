/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.snapshot;

import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.games.GameEntity;
import com.google.android.gms.games.PlayerEntity;

@SafeParcelable.Class
public class SnapshotMetadataEntity extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getGame")
    private final GameEntity gameEntity;
    @Field(value = 2, getterName = "getOwner")
    private final PlayerEntity playerEntity;
    @Field(value = 3, getterName = "getSnapshotId")
    private final String snapshotId;
    @Field(value = 5, getterName = "getCoverImageUri")
    @Nullable
    private final Uri coverImageUri;
    @Field(value = 6, getterName = "getCoverImageUrl")
    @Nullable
    private final String coverImageUrl;
    @Field(value = 7, getterName = "getTitle")
    private final String title;
    @Field(value = 8, getterName = "getDescription")
    private final String description;
    @Field(value = 9, getterName = "getLastModifiedTimestamp")
    private final long lastModifiedTimestamp;
    @Field(value = 10, getterName = "getPlayedTime")
    private final long playedTime;
    @Field(value = 11, getterName = "getCoverImageAspectRatio")
    private final float coverImageAspectRatio;
    @Field(value = 12, getterName = "getUniqueName")
    private final String uniqueName;
    @Field(value = 13, getterName = "hasChangePending")
    private final boolean hasChangePending;
    @Field(value = 14, getterName = "getProgressValue")
    private final long progressValue;
    @Field(value = 15, getterName = "getDeviceName")
    @Nullable
    private final String deviceName;

    @Constructor
    public SnapshotMetadataEntity(@Param(value = 1) GameEntity gameEntity, @Param(value = 2) PlayerEntity playerEntity,
                           @Param(value = 3) String snapshotId, @Param(value = 5) @Nullable Uri coverImageUri,
                           @Param(value = 6) @Nullable String coverImageUrl, @Param(value = 7) String title,
                           @Param(value = 8) String description, @Param(value = 9) long lastModifiedTimestamp, @Param(value = 10)
                           long playedTime, @Param(value = 11) float coverImageAspectRatio, @Param(value = 12) String uniqueName,
                           @Param(value = 13) boolean hasChangePending, @Param(value = 14) long progressValue, @Param(value = 15) @Nullable String deviceName) {
        this.gameEntity = gameEntity;
        this.playerEntity = playerEntity;
        this.snapshotId = snapshotId;
        this.coverImageUri = coverImageUri;
        this.coverImageUrl = coverImageUrl;
        this.coverImageAspectRatio = coverImageAspectRatio;
        this.title = title;
        this.description = description;
        this.lastModifiedTimestamp = lastModifiedTimestamp;
        this.playedTime = playedTime;
        this.uniqueName = uniqueName;
        this.hasChangePending = hasChangePending;
        this.progressValue = progressValue;
        this.deviceName = deviceName;
    }

    public final GameEntity getGame() {
        return this.gameEntity;
    }

    public final PlayerEntity getOwner() {
        return this.playerEntity;
    }

    public final String getSnapshotId() {
        return this.snapshotId;
    }

    @Nullable
    public final Uri getCoverImageUri() {
        return this.coverImageUri;
    }

    @Nullable
    public final String getCoverImageUrl() {
        return this.coverImageUrl;
    }

    public final float getCoverImageAspectRatio() {
        return this.coverImageAspectRatio;
    }

    public final String getUniqueName() {
        return this.uniqueName;
    }

    public final String getTitle() {
        return this.title;
    }

    public final String getDescription() {
        return this.description;
    }

    public final long getLastModifiedTimestamp() {
        return this.lastModifiedTimestamp;
    }

    public final long getPlayedTime() {
        return this.playedTime;
    }

    public final boolean hasChangePending() {
        return this.hasChangePending;
    }

    public final long getProgressValue() {
        return this.progressValue;
    }

    public final String getDeviceName() {
        return this.deviceName;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SnapshotMetadataEntity> CREATOR = findCreator(SnapshotMetadataEntity.class);
}
