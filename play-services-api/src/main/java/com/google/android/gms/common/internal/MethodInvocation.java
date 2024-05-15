/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class MethodInvocation extends AbstractSafeParcelable {

    @Field(1)
    public int requestCode;
    @Field(2)
    public int callingUid;
    @Field(3)
    public int resultCode;
    @Field(4)
    public long startTime;
    @Field(5)
    public long endTime;
    @Field(6)
    public String packageName;
    @Field(7)
    public String methodName;
    @Field(8)
    public int duration;
    @Field(9)
    public int flags;

    public MethodInvocation() {
    }

    public MethodInvocation(int requestCode, int callingUid, int resultCode, long startTime, long endTime, String packageName, String methodName, int duration, int flags) {
        this.requestCode = requestCode;
        this.callingUid = callingUid;
        this.resultCode = resultCode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.packageName = packageName;
        this.methodName = methodName;
        this.duration = duration;
        this.flags = flags;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("MethodInvocation")
                .field("requestCode", requestCode)
                .field("callingUid", callingUid)
                .field("resultCode", resultCode)
                .field("startTime", startTime)
                .field("endTime", endTime)
                .field("packageName", packageName)
                .field("methodName", methodName)
                .field("duration", duration)
                .field("flags", flags)
                .end();
    }

    public static SafeParcelableCreatorAndWriter<MethodInvocation> CREATOR = findCreator(MethodInvocation.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}