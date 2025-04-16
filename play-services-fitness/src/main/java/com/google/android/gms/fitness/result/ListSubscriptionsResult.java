/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.result;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fitness.data.Subscription;

import java.util.List;

@SafeParcelable.Class
public class ListSubscriptionsResult extends AbstractSafeParcelable {

    @Field(1)
    public List<Subscription> subscriptions;
    @Field(2)
    public Status status;

    @Constructor
    public ListSubscriptionsResult(@Param(1) List<Subscription> list, @Param(2) Status status) {
        this.subscriptions = list;
        this.status = status;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ListSubscriptionsResult> CREATOR = findCreator(ListSubscriptionsResult.class);
}
