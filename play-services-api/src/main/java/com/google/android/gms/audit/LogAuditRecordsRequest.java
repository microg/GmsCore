/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.audit;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class LogAuditRecordsRequest extends AbstractSafeParcelable {
    @Field(1)
    public int writeMode;
    @Field(2)
    public int componentId;
    @Field(3)
    public String accountName;
    @Field(4)
    public byte[][] auditRecords;
    @Field(5)
    public byte[] traceToken;
    @Field(6)
    public byte[] auditToken;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("LogAuditRecordsRequest")
                .field("writeMode", writeMode)
                .field("componentId", componentId)
                .field("accountName", accountName)
                .field("auditRecords", auditRecords)
                .field("traceToken", traceToken)
                .field("auditToken", auditToken)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LogAuditRecordsRequest> CREATOR = findCreator(LogAuditRecordsRequest.class);

}
