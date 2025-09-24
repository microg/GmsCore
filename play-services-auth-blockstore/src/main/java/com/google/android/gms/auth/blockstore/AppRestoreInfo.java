/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.blockstore;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class AppRestoreInfo extends AbstractSafeParcelable {
    @Field(value = 1)
    public String restoreSessionId;

    @Field(value = 2)
    public String restoreSource;

    public AppRestoreInfo() {
    }

    @Constructor
    public AppRestoreInfo(@Param(1) String restoreSessionId, @Param(2) String restoreSource) {
        this.restoreSessionId = restoreSessionId;
        this.restoreSource = restoreSource;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Override
    public String toString() {
        return ToStringHelper.name("AppRestoreInfo").field("restoreSessionId", restoreSessionId).field("restoreSource", restoreSource).end();
    }

    public static final SafeParcelableCreatorAndWriter<AppRestoreInfo> CREATOR = findCreator(AppRestoreInfo.class);
}
