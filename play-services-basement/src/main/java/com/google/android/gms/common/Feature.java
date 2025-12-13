/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class Feature extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getName")
    private final String name;
    @Field(value = 2, defaultValue = "0")
    final int oldVersion;
    @Field(value = 3, getterName = "getVersion", defaultValue = "-1")
    private final long version;
    @Field(value = 4, defaultValue = "false")
    final boolean fullyRolledOut;

    public Feature(String name) {
        this(name, 1);
    }

    public Feature(String name, long version) {
        this(name, version, false);
    }

    public Feature(String name, long version, boolean fullyRolledOut) {
        this(name, -1, version, fullyRolledOut);
    }

    @Constructor
    Feature(@Param(1) String name, @Param(2) int oldVersion, @Param(3) long version, @Param(4) boolean fullyRolledOut) {
        this.name = name;
        this.oldVersion = oldVersion;
        this.version = version;
        this.fullyRolledOut = fullyRolledOut;
    }

    public String getName() {
        return name;
    }

    public long getVersion() {
        if (version == -1) return oldVersion;
        return version;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("Feature").value(name).value(getVersion()).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Feature> CREATOR = findCreator(Feature.class);
}
