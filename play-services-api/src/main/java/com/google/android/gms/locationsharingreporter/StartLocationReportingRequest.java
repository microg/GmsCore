/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class StartLocationReportingRequest extends AbstractSafeParcelable {
    @Field(1)
    public final int makePrimary;
    @Field(2)
    public final int reportingType;
    @Field(3)
    public final LocationShare locationShare;
    @Field(4)
    public final long requestDurationMs;
    @Field(5)
    public final NoticeAckedUpdateRequest noticeAckedUpdateRequest;

    @Constructor
    public StartLocationReportingRequest(@Param(1) int makePrimary, @Param(2) int reportingType, @Param(3) LocationShare locationShare,
                                         @Param(4) long requestDurationMs, @Param(5) NoticeAckedUpdateRequest noticeAckedUpdateRequest) {
        this.makePrimary = makePrimary;
        this.reportingType = reportingType;
        this.requestDurationMs = requestDurationMs;
        this.locationShare = locationShare;
        this.noticeAckedUpdateRequest = noticeAckedUpdateRequest;
    }

    @NonNull
    @Override
    public final String toString() {
        return "StartLocationReportingRequest{" +
                "makePrimary=" + makePrimary +
                ", reportingType=" + reportingType +
                ", locationShare=" + locationShare +
                ", requestDurationMs=" + requestDurationMs +
                ", noticeAckedUpdateRequest=" + noticeAckedUpdateRequest +
                '}';
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<StartLocationReportingRequest> CREATOR = findCreator(StartLocationReportingRequest.class);
}
