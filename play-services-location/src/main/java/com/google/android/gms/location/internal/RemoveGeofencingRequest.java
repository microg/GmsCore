/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import android.app.PendingIntent;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class RemoveGeofencingRequest extends AbstractSafeParcelable {
    @Field(1)
    @Nullable
    public final List<String> geofenceIds;
    @Field(2)
    @Nullable
    public final PendingIntent pendingIntent;
    @Field(3)
    public final String tag;

    @Constructor
    public RemoveGeofencingRequest(@Nullable @Param(1) List<String> geofenceIds, @Nullable @Param(2) PendingIntent pendingIntent, @Param(3) String tag) {
        this.geofenceIds = geofenceIds;
        this.pendingIntent = pendingIntent;
        this.tag = tag;
    }

    public static RemoveGeofencingRequest byGeofenceIds(@NonNull List<String> geofenceIds) {
        return new RemoveGeofencingRequest(geofenceIds, null, "");
    }

    public static RemoveGeofencingRequest byPendingIntent(@NonNull PendingIntent pendingIntent) {
        return new RemoveGeofencingRequest(null, pendingIntent, "");
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        CREATOR.writeToParcel(this, parcel, i);
    }

    public static final SafeParcelableCreatorAndWriter<RemoveGeofencingRequest> CREATOR = findCreator(RemoveGeofencingRequest.class);
}
