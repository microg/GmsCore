/*
 * SPDX-FileCopyrightText: 2019, e Foundation
 * SPDX-FileCopyrightText: 2021, Google LLC
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.dynamiclinks.internal;

import android.net.Uri;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.List;


public class ShortDynamicLinkImpl extends AutoSafeParcelable {
    @Field(1)
    public final Uri shortLink;

    @Field(2)
    public final Uri previewLink;

    @Field(3)
    public final List<WarningImpl> warnings;


    public ShortDynamicLinkImpl() {
        shortLink = Uri.EMPTY;
        previewLink = Uri.EMPTY;

        warnings = new ArrayList<>();
    }

    public ShortDynamicLinkImpl(Uri shortLink, Uri previewLink, List<WarningImpl> warnings) {
        this.shortLink = shortLink;
        this.previewLink = previewLink;
        this.warnings = warnings;
    }

    public static final Creator<ShortDynamicLinkImpl> CREATOR = new AutoCreator<ShortDynamicLinkImpl>(ShortDynamicLinkImpl.class);
}
