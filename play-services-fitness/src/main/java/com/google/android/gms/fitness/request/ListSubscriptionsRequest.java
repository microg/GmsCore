/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.request;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.internal.IListSubscriptionsCallback;

@SafeParcelable.Class
public class ListSubscriptionsRequest extends AbstractSafeParcelable {
    @Field(1)
    public DataType dataType;
    @Field(2)
    public IListSubscriptionsCallback callback;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ListSubscriptionsRequest> CREATOR = findCreator(ListSubscriptionsRequest.class);
}
