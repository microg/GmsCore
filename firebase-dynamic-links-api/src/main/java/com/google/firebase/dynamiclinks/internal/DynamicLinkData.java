/*
 * SPDX-FileCopyrightText: 2019, e Foundation
 * SPDX-FileCopyrightText: 2021, Google LLC
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.dynamiclinks.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import android.os.Bundle;
import android.net.Uri;

public class DynamicLinkData extends AutoSafeParcelable {
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

    public DynamicLinkData() {
        dynamicLink = "";
        deepLink = "";
        minVersion = 0;
        clickTimestamp = 0;
        extensionBundle = new Bundle();
        redirectUrl = Uri.EMPTY;
    }

    public static final Creator<DynamicLinkData> CREATOR = new AutoCreator<DynamicLinkData>(DynamicLinkData.class);
}
