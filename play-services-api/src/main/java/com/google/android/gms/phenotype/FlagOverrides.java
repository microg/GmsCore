/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.phenotype;

import org.microg.safeparcel.AutoSafeParcelable;

public class FlagOverrides extends AutoSafeParcelable {
    public static final Creator<FlagOverrides> CREATOR = new AutoCreator<>(FlagOverrides.class);
}
