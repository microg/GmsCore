/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.common.internal;

import android.accounts.Account;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.Feature;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Constants;
import org.microg.gms.common.GmsService;

import java.util.Arrays;

@SafeParcelable.Class
public class GetServiceRequest extends AbstractSafeParcelable {
    @Field(1)
    int versionCode = 6;
    @Field(2)
    public final int serviceId;
    @Field(3)
    public int gmsVersion;
    @Field(4)
    public String packageName;
    @Field(5)
    public IBinder accountAccessor;
    @Field(6)
    public Scope[] scopes;
    @Field(7)
    public Bundle extras;
    @Field(8)
    public Account account;
    @Field(9)
    @Deprecated
    long field9;
    @Field(10)
    public Feature[] defaultFeatures;
    @Field(11)
    public Feature[] apiFeatures;
    @Field(12)
    boolean supportsConnectionInfo;
    @Field(13)
    int field13;
    @Field(14)
    boolean field14;
    @Field(15)
    String attributionTag;

    private GetServiceRequest() {
        serviceId = -1;
        gmsVersion = Constants.GMS_VERSION_CODE;
    }

    @Constructor
    public GetServiceRequest(@Param(2) int serviceId) {
        this.serviceId = serviceId;
        this.gmsVersion = Constants.GMS_VERSION_CODE;
        this.supportsConnectionInfo = true;
    }

    @Override
    public String toString() {
        return "GetServiceRequest{" +
                "serviceId=" + GmsService.nameFromServiceId(serviceId) +
                ", gmsVersion=" + gmsVersion +
                ", packageName='" + packageName + '\'' +
                (scopes == null || scopes.length == 0 ? "" : (", scopes=" + Arrays.toString(scopes))) +
                (extras == null ? "" : (", extras=" + extras)) +
                (account == null ? "" : (", account=" + account)) +
                '}';
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static SafeParcelableCreatorAndWriter<GetServiceRequest> CREATOR = findCreator(GetServiceRequest.class);
}
