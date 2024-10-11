/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
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

@SafeParcelable.Class
public class SnapshotMetadataChangeEntity extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getDescription")
    private final String description;
    @Field(value = 2, getterName = "getPlayedTimeMillis")
    private final Long playedTimeMillis;
    @Field(value = 4, getterName = "getCoverImageUri")
    private final Uri coverImageUri;
    @Field(value = 5, getterName = "getBitmapTeleporter")
    private BitmapTeleporter coverImageTeleporter;
    @Field(value = 6, getterName = "getProgressValue")
    private final Long progressValue;

    @Constructor
    SnapshotMetadataChangeEntity(@Param(value = 1) String var1, @Param(value = 2) Long var2, @Param(value = 5) BitmapTeleporter var3, @Param(value = 4) Uri var4, @Param(value = 6) Long var5) {
        this.description = var1;
        this.playedTimeMillis = var2;
        this.coverImageTeleporter = var3;
        this.coverImageUri = var4;
        this.progressValue = var5;
    }

    @Nullable
    public final String getDescription() {
        return this.description;
    }

    @Nullable
    public final Long getPlayedTimeMillis() {
        return this.playedTimeMillis;
    }

    @Nullable
    public final Long getProgressValue() {
        return this.progressValue;
    }

    @Nullable
    public final BitmapTeleporter getBitmapTeleporter() {
        return this.coverImageTeleporter;
    }

    @Nullable
    public final Bitmap getCoverImage() {
        return this.coverImageTeleporter == null ? null : this.coverImageTeleporter.createTargetBitmap();
    }

    public Uri getCoverImageUri() {
        return coverImageUri;
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
