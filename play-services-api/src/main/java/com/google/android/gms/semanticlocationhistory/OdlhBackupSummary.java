/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

import java.util.List;

@SafeParcelable.Class
public class OdlhBackupSummary extends AbstractSafeParcelable {

    @Field(1)
    public final long databaseId;

    @Field(2)
    public final String databaseName;

    @Field(3)
    public final boolean isThisDevice;

    @Field(4)
    public final long lastSyncTime;

    @Field(5)
    public final List<String> gellerKeys;

    @Field(6)
    public final String deviceIdentifier;

    @Field(7)
    public final Integer protoSerializedSize;

    @Field(8)
    public final Integer metadataRowCount;

    @Field(9)
    public final Long earliestTimestamp;

    @Constructor
    public OdlhBackupSummary(
            @Param(1) long databaseId,
            @Param(2) String databaseName,
            @Param(3) boolean isThisDevice,
            @Param(4) long lastSyncTime,
            @Param(5) List<String> gellerKeys,
            @Param(6) String deviceIdentifier,
            @Param(7) Integer protoSerializedSize,
            @Param(8) Integer metadataRowCount,
            @Param(9) Long earliestTimestamp) {
        this.databaseId = databaseId;
        this.databaseName = databaseName;
        this.isThisDevice = isThisDevice;
        this.lastSyncTime = lastSyncTime;
        this.gellerKeys = gellerKeys;
        this.deviceIdentifier = deviceIdentifier;
        this.protoSerializedSize = protoSerializedSize;
        this.metadataRowCount = metadataRowCount;
        this.earliestTimestamp = earliestTimestamp;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<OdlhBackupSummary> CREATOR = findCreator(OdlhBackupSummary.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("OdlhBackupSummary")
                .field("databaseId", databaseId)
                .field("databaseName", databaseName)
                .field("isThisDevice", isThisDevice)
                .field("lastSyncTime", lastSyncTime)
                .field("deviceIdentifier", deviceIdentifier)
                .field("protoSerializedSize", protoSerializedSize)
                .field("metadataRowCount", metadataRowCount)
                .field("earliestTimestamp", earliestTimestamp)
                .end();
    }
}