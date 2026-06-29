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
 * Request for exporting credentials to primary credential provider.
 */
@SafeParcelable.Class
public class ExportCredentialsToDeviceSetupRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getUri")
    @NonNull
    private final Uri uri;
    @Field(value = 2, getterName = "getRequestData")
    @NonNull
    private final Bundle requestData;

    /**
     * @param uri         the file URI responsible for the export transport. The export provider will write the credentials here.
     * @param requestData the request bundle
     */
    @Constructor
    public ExportCredentialsToDeviceSetupRequest(@NonNull @Param(1) Uri uri, @NonNull @Param(2) Bundle requestData) {
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
     * the file URI responsible for the export transport. The export provider will write the credentials here.
     */
    @NonNull
    public Uri getUri() {
        return uri;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ExportCredentialsToDeviceSetupRequest> CREATOR = findCreator(ExportCredentialsToDeviceSetupRequest.class);
}
