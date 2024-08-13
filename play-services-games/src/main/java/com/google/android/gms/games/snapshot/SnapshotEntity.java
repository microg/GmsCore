/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.snapshot;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class SnapshotEntity extends AbstractSafeParcelable implements Snapshot {

    @Field(value = 1, getterName = "getMetadata")
    private final SnapshotMetadataEntity metadataEntity;
    @Field(value = 3, getterName = "getSnapshotContents")
    private final SnapshotContentsEntity snapshotContents;

    @Constructor
    public SnapshotEntity(@Param(value = 1) SnapshotMetadata var1, @Param(value = 3) SnapshotContentsEntity var2) {
        this.metadataEntity = new SnapshotMetadataEntity(var1);
        this.snapshotContents = var2;
    }

    public final SnapshotMetadataEntity getMetadata() {
        return this.metadataEntity;
    }

    public final SnapshotContentsEntity getSnapshotContents() {
        return this.snapshotContents.isClosed() ? null : this.snapshotContents;
    }

    public final Snapshot freeze() {
        return this;
    }

    public final boolean isDataValid() {
        return true;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SnapshotEntity> CREATOR = findCreator(SnapshotEntity.class);
}
