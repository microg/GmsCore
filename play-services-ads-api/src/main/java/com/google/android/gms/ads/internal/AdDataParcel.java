/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class AdDataParcel extends AutoSafeParcelable {
    public static final Creator<AdDataParcel> CREATOR = new AutoCreator<>(AdDataParcel.class);
}
