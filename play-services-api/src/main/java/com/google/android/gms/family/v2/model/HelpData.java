/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.model;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class HelpData extends AbstractSafeParcelable {
    @Field(1)
    public String linkUrl;
    @Field(2)
    public String appContext;

    public HelpData() {
    }

    public HelpData(String linkUrl, String appContext) {
        this.linkUrl = linkUrl;
        this.appContext = appContext;
    }

    public static final SafeParcelableCreatorAndWriter<HelpData> CREATOR = findCreator(HelpData.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Override
    public String toString() {
        return "HelpData{" +
                "linkUrl='" + linkUrl + '\'' +
                ", appContext='" + appContext + '\'' +
                '}';
    }
}
