/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.request;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.internal.IStatusCallback;

import java.util.List;

@SafeParcelable.Class
public class SessionInsertRequest extends AbstractSafeParcelable {

    @Field(1)
    public Session seesion;
    @Field(2)
    public List<DataSet> dataSets;

    @Field(3)
    public List<DataPoint> aggregateDataPoints;

    @Field(4)
    public IStatusCallback callback;

    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SessionInsertRequest> CREATOR = findCreator(SessionInsertRequest.class);

}
