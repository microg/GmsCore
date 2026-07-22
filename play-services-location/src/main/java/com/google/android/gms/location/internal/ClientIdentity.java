/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.Feature;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Hide
public class ClientIdentity extends AutoSafeParcelable {
    @Field(1)
    public int uid;
    @Field(2)
    public int pid;
    @Field(3)
    public String packageName;
    @Field(4)
    @Nullable
    public String attributionTag;
    @Field(5)
    public int clientSdkVersion;
    @Field(6)
    @Nullable
    public String listenerId;
    @Field(7)
    @Nullable
    public ClientIdentity impersonator;
    @Field(8)
    public List<Feature> clientFeatures = Collections.emptyList();

    private ClientIdentity() {}

    public ClientIdentity(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientIdentity)) return false;

        ClientIdentity that = (ClientIdentity) o;
        return uid == that.uid && pid == that.pid && clientSdkVersion == that.clientSdkVersion && Objects.equals(packageName, that.packageName) && Objects.equals(attributionTag, that.attributionTag) && Objects.equals(listenerId, that.listenerId) && Objects.equals(impersonator, that.impersonator) && Objects.equals(clientFeatures, that.clientFeatures);
    }

    @Override
    public int hashCode() {
        int result = uid;
        result = 31 * result + pid;
        result = 31 * result + Objects.hashCode(packageName);
        result = 31 * result + Objects.hashCode(attributionTag);
        result = 31 * result + clientSdkVersion;
        result = 31 * result + Objects.hashCode(listenerId);
        result = 31 * result + Objects.hashCode(impersonator);
        result = 31 * result + Objects.hashCode(clientFeatures);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ClientIdentity").value(uid).value(packageName).end();
    }

    public static final Creator<ClientIdentity> CREATOR = new AutoCreator<>(ClientIdentity.class);
}
