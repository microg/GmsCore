/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.measurement.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class ConditionalUserPropertyParcel extends AutoSafeParcelable {
    public static final Creator<ConditionalUserPropertyParcel> CREATOR = new AutoCreator<>(ConditionalUserPropertyParcel.class);
}
