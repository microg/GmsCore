/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.games.snapshot;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.data.BitmapTeleporter;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class SnapshotMetadataChangeEntity extends AbstractSafeParcelable implements SnapshotMetadataChange {

    @Field(value = 1, getterName = "getDescription")
    @Nullable
    private final String description;
    @Field(value = 2, getterName = "getPlayedTimeMillis")
    @Nullable
    private final Long playedTimeMillis;
    @Field(value = 4, getterName = "getCoverImageUri")
    @Nullable
    private final Uri coverImageUri;
    @Field(value = 5, getterName = "getCoverImageTeleporter")
    @Nullable
    private final BitmapTeleporter coverImageTeleporter;
    @Field(value = 6, getterName = "getProgressValue")
    @Nullable
    private final Long progressValue;

    @Constructor
    SnapshotMetadataChangeEntity(@Nullable @Param(value = 1) String description, @Nullable @Param(value = 2) Long playedTimeMillis, @Nullable @Param(value = 5) BitmapTeleporter coverImageTeleporter, @Nullable @Param(value = 4) Uri coverImageUri, @Nullable @Param(value = 6) Long progressValue) {
        this.description = description;
        this.playedTimeMillis = playedTimeMillis;
        this.coverImageTeleporter = coverImageTeleporter;
        this.coverImageUri = coverImageUri;
        this.progressValue = progressValue;
    }

    /**
     * Returns the new cover image to set for the snapshot.
     */
    @Override
    @Nullable
    public Bitmap getCoverImage() {
        return this.coverImageTeleporter == null ? null : this.coverImageTeleporter.createTargetBitmap();
    }

    @Nullable
    public BitmapTeleporter getCoverImageTeleporter() {
        return this.coverImageTeleporter;
    }

    @Nullable
    Uri getCoverImageUri() {
        return coverImageUri;
    }

    /**
     * Returns the new description to set for the snapshot.
     */
    @Override
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the new played time to set for the snapshot.
     */
    @Override
    @Nullable
    public Long getPlayedTimeMillis() {
        return this.playedTimeMillis;
    }

    /**
     * Returns the new progress value to set for the snapshot.
     */
    @Override
    @Nullable
    public Long getProgressValue() {
        return this.progressValue;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SnapshotMetadataChangeEntity> CREATOR = findCreator(SnapshotMetadataChangeEntity.class);

    @Override
    public String toString() {
        return "SnapshotMetadataChangeEntity{" +
                "description='" + description + '\'' +
                ", playedTimeMillis=" + playedTimeMillis +
                ", coverImageUri=" + coverImageUri +
                ", coverImageTeleporter=" + coverImageTeleporter +
                ", progressValue=" + progressValue +
                '}';
    }
}
