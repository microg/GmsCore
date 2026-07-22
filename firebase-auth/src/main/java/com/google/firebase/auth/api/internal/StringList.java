/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.List;

public class StringList extends AutoSafeParcelable {
    @Field(1)
    public int versionCode = 1;
    @Field(2)
    public List<String> values = new ArrayList<>();
    public static final Creator<StringList> CREATOR = new AutoCreator<>(StringList.class);
}
