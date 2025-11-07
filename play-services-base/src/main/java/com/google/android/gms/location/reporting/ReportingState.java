/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.reporting;

import org.microg.safeparcel.AutoSafeParcelable;

public class ReportingState extends AutoSafeParcelable {
    @Field(1)
    @Deprecated
    private int versionCode = 2;
    @Field(2)
    public int reportingEnabled;
    @Field(3)
    public int historyEnabled;
    @Field(4)
    public boolean allowed;
    @Field(5)
    public boolean active;
    @Field(6)
    public boolean defer;
    @Field(7)
    public int expectedOptInResult;
    @Field(8)
    public Integer deviceTag;
    @Field(9)
    public int expectedOptInResultAssumingLocationEnabled;
    @Field(10)
    public boolean canAccessSettings;

    public static final Creator<ReportingState> CREATOR = new AutoCreator<ReportingState>(ReportingState.class);
}
