/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.os.IBinder;
import androidx.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class ResolveAccountResponse extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 2;
    @Field(2)
    public IBinder accountAccessor;
    @Field(3)
    public ConnectionResult connectionResult;
    @Field(4)
    public boolean saveDefaultAccount;
    @Field(5)
    public boolean fromCrossClientAuth;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ResolveAccountResponse")
                .field("connectionResult", connectionResult)
                .field("saveDefaultAccount", saveDefaultAccount)
                .field("fromCrossClientAuth", fromCrossClientAuth)
                .end();
    }

    public static final Creator<ResolveAccountResponse> CREATOR = new AutoCreator<>(ResolveAccountResponse.class);
}
