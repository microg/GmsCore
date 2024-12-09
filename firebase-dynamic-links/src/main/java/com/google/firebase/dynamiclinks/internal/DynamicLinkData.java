/*
 * SPDX-FileCopyrightText: 2019, e Foundation
 * SPDX-FileCopyrightText: 2021, Google LLC
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.dynamiclinks.internal;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import android.os.Bundle;
import android.net.Uri;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class DynamicLinkData extends AbstractSafeParcelable {
    @Field(1)
    public final String dynamicLink;

    @Field(2)
    public final String deepLink;

    @Field(3)
    public final int minVersion;

    @Field(4)
    public final long clickTimestamp;

    @Field(5)
    public final Bundle extensionBundle;

    @Field(6)
    public final Uri redirectUrl;

    @Constructor
    public DynamicLinkData(@Param(1) String dynamicLink, @Param(2) String deepLink, @Param(3) int minVersion, @Param(4) long clickTimestamp, @Param(5) Bundle extensionBundle, @Param(6) Uri redirectUrl) {
        this.dynamicLink = dynamicLink;
        this.deepLink = deepLink;
        this.minVersion = minVersion;
        this.clickTimestamp = clickTimestamp;
        this.extensionBundle = extensionBundle;
        this.redirectUrl = redirectUrl;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("DynamicLinkData")
                .field("dynamicLink", dynamicLink)
                .field("deepLink", deepLink)
                .field("minVersion", minVersion)
                .field("clickTimestamp", clickTimestamp)
                .field("extensionBundle", extensionBundle)
                .field("redirectUrl", redirectUrl)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DynamicLinkData> CREATOR = findCreator(DynamicLinkData.class);
}
