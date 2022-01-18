/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.clearcut.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class LogVerifierResultParcelable  extends AutoSafeParcelable {
    @Field(1)
    public boolean b;

    public static final Creator<LogVerifierResultParcelable> CREATOR = new AutoCreator<>(LogVerifierResultParcelable.class);
}
