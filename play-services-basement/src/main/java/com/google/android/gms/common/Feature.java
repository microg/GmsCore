/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common;

import org.microg.safeparcel.AutoSafeParcelable;

public class Feature extends AutoSafeParcelable {
    @Field(1)
    private String name;
    @Field(2)
    private int oldVersion;
    @Field(3)
    private long version = -1;

    private Feature() {
    }

    public Feature(String name, long version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public long getVersion() {
        if (version == -1) return oldVersion;
        return version;
    }

    public static final Creator<Feature> CREATOR = new AutoSafeParcelable.AutoCreator<>(Feature.class);
}
