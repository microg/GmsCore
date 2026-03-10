/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.semanticlocation;


import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class SemanticLocationEventRequest extends AbstractSafeParcelable {
    @Field(1)
    public final float position;

    @Constructor
    public SemanticLocationEventRequest(@Param(1) float position) {
        this.position = position;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SemanticLocationEventRequest> CREATOR = findCreator(SemanticLocationEventRequest.class);
}
