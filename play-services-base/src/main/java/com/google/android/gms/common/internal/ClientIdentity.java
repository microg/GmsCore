/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import androidx.annotation.NonNull;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Objects;

public class ClientIdentity extends AutoSafeParcelable {
    @Field(1)
    public int uid;
    @Field(2)
    public String packageName;

    @Override
    public int hashCode() {
        return uid;
    }

    @NonNull
    @Override
    public String toString() {
        return uid + ":" + packageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientIdentity that = (ClientIdentity) o;
        return uid == that.uid && Objects.equals(packageName, that.packageName);
    }

    public static final Creator<ClientIdentity> CREATOR = new AutoCreator<ClientIdentity>(ClientIdentity.class);
}
