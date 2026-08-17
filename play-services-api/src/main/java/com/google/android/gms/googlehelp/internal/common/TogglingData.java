/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.googlehelp.internal.common;

import org.microg.safeparcel.AutoSafeParcelable;

public class TogglingData extends AutoSafeParcelable {
    public static final Creator<TogglingData> CREATOR = findCreator(TogglingData.class);
}
