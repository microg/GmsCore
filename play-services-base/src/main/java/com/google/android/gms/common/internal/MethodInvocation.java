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

@SafeParcelable.Class
public class MethodInvocation extends AbstractSafeParcelable {

    @Field(1)
    final int methodKey;
    @Field(2)
    final int resultStatusCode;
    @Field(3)
    final int connectionResultStatusCode;
    @Field(4)
    final long startTimeMillis;
    @Field(5)
    final long endTimeMillis;
    @Field(6)
    @Nullable
    final String callingModuleId;
    @Field(7)
    @Nullable
    final String callingEntryPoint;
    @Field(8)
    final int serviceId;
    @Field(value = 9, defaultValue = "-1")
    final int latencyMillis;

    @Constructor
    public MethodInvocation(@Param(1) int methodKey, @Param(2) int resultStatusCode, @Param(3) int connectionResultStatusCode, @Param(4) long startTimeMillis, @Param(5) long endTimeMillis, @Param(6) @Nullable String callingModuleId, @Param(7) @Nullable String callingEntryPoint, @Param(8) int serviceId, @Param(9) int latencyMillis) {
        this.methodKey = methodKey;
        this.resultStatusCode = resultStatusCode;
        this.connectionResultStatusCode = connectionResultStatusCode;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.callingModuleId = callingModuleId;
        this.callingEntryPoint = callingEntryPoint;
        this.serviceId = serviceId;
        this.latencyMillis = latencyMillis;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("MethodInvocation")
                .field("methodKey", methodKey)
                .field("resultStatusCode", resultStatusCode)
                .field("connectionResultStatusCode", connectionResultStatusCode)
                .field("startTimeMillis", startTimeMillis)
                .field("endTimeMillis", endTimeMillis)
                .field("callingModuleId", callingModuleId)
                .field("callingEntryPoint", callingEntryPoint)
                .field("serviceId", serviceId)
                .field("latencyMillis", latencyMillis)
                .end();
    }

    public static SafeParcelableCreatorAndWriter<MethodInvocation> CREATOR = findCreator(MethodInvocation.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}