/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.identitycredentials;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Request for importing credentials from primary credential provider.
 */
@SafeParcelable.Class
public class ImportCredentialsForDeviceSetupRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getRequestJson")
    @NonNull
    private final String requestJson;
    @Field(value = 2, getterName = "getUri")
    @NonNull
    private final Uri uri;
    @Field(value = 3, getterName = "getRequestData")
    @NonNull
    private final Bundle requestData;

    /**
     * @param requestJson the request in JSON format, based on the CXF prototcol
     * @param uri         the file URI responsible for the credential transport. The importing provider will read the credentials from here.
     * @param requestData the request bundle
     */
    @Constructor
    public ImportCredentialsForDeviceSetupRequest(@NonNull @Param(1) String requestJson, @NonNull @Param(2) Uri uri, @NonNull @Param(3) Bundle requestData) {
        this.requestJson = requestJson;
        this.uri = uri;
        this.requestData = requestData;
    }

    /**
     * the request bundle
     */
    @NonNull
    public Bundle getRequestData() {
        return requestData;
    }

    /**
     * the request in JSON format, based on the CXF prototcol
     */
    @NonNull
    public String getRequestJson() {
        return requestJson;
    }

    /**
     * the file URI responsible for the credential transport. The importing provider will read the credentials from here.
     */
    @NonNull
    public Uri getUri() {
        return uri;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ImportCredentialsForDeviceSetupRequest> CREATOR = findCreator(ImportCredentialsForDeviceSetupRequest.class);
}
