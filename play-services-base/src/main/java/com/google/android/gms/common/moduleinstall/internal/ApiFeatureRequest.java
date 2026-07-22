/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.moduleinstall.internal;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.Feature;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

import java.util.List;

@SafeParcelable.Class
@Hide
public class ApiFeatureRequest extends AbstractSafeParcelable {
    @Field(1)
    public List<Feature> features;
    @Field(2)
    public boolean urgent;
    @Field(3)
    public String sessionId;
    @Field(4)
    public String callingPackage;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ApiFeatureRequest")
                .field("features", features)
                .field("urgent", urgent)
                .field("sessionId", sessionId)
                .field("callingPackage", callingPackage)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ApiFeatureRequest> CREATOR = findCreator(ApiFeatureRequest.class);
}
