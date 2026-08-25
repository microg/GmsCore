/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@SafeParcelable.Class
@Hide
public class SessionDataSet extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<SessionDataSet> CREATOR = findCreator(SessionDataSet.class);

    @Field(1)
    public final Session session;
    @Field(2)
    public final DataSet dataSet;

    @Constructor
    public SessionDataSet(@Param(1) Session session, @Param(2) DataSet dataSet) {
        this.session = session;
        this.dataSet = dataSet;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
