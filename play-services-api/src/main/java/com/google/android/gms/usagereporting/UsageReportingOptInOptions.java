/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.usagereporting;

import org.microg.safeparcel.AutoSafeParcelable;

public class UsageReportingOptInOptions extends AutoSafeParcelable {
    @Field(2)
    public int optInUsageReporting;

    public static final Creator<UsageReportingOptInOptions> CREATOR = new AutoCreator<>(UsageReportingOptInOptions.class);
}
