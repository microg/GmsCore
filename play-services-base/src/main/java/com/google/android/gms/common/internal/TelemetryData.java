/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.os.Parcel;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

import java.util.List;

@SafeParcelable.Class
public class TelemetryData extends AbstractSafeParcelable {

    @Field(1)
    final int telemetryConfigVersion;
    @Field(2)
    @Nullable
    List<MethodInvocation> methodInvocations;

    @Constructor
    public TelemetryData(@Param(1) int telemetryConfigVersion, @Param(2) @Nullable List<MethodInvocation> methodInvocations) {
        this.telemetryConfigVersion = telemetryConfigVersion;
        this.methodInvocations = methodInvocations;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("TelemetryData")
                .field("telemetryConfigVersion", telemetryConfigVersion)
                .field("methodInvocations", methodInvocations)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static SafeParcelableCreatorAndWriter<TelemetryData> CREATOR = findCreator(TelemetryData.class);

}