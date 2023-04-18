/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.internal;

import androidx.annotation.Nullable;
import com.google.android.gms.common.Feature;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Collections;
import java.util.List;

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

    public static final Creator<ClientIdentity> CREATOR = new AutoCreator<>(ClientIdentity.class);
}
