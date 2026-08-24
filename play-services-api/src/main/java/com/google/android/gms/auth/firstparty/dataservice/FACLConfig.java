/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.firstparty.dataservice;

import org.microg.safeparcel.AutoSafeParcelable;

public class FACLConfig extends AutoSafeParcelable {
    @Field(1)
    public int versionCode = 1;
    @Field(2)
    public boolean allCirclesVisible;
    @Field(3)
    public String visibleEdges;
    @Field(4)
    public boolean allContactsVisible;
    @Field(5)
    public boolean showCircles;
    @Field(6)
    public boolean showContacts;
    @Field(7)
    public boolean hasShowCircles;

    public static final Creator<FACLConfig> CREATOR = new AutoCreator<FACLConfig>(FACLConfig.class);
}
