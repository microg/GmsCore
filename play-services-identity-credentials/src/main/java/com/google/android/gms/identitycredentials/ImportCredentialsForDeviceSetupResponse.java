/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.identitycredentials;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Response for importing the credentials from the primary provider
 */
@SafeParcelable.Class
public class ImportCredentialsForDeviceSetupResponse extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getResponseBundle")
    @NonNull
    private final Bundle responseBundle;

    /**
     * @param responseBundle the bundle containing response extras.
     */
    @Constructor
    public ImportCredentialsForDeviceSetupResponse(@NonNull @Param(1) Bundle responseBundle) {
        this.responseBundle = responseBundle;
    }

    /**
     * the bundle containing response extras.
     */
    @NonNull
    public Bundle getResponseBundle() {
        return responseBundle;
    }

    /**
     * Returns the provider app info.
     */
    @Nullable
    public final CallingAppInfoParcelable getProviderAppInfo() {
        return this.responseBundle.getParcelable("PROVIDER_APP_INFO");
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ImportCredentialsForDeviceSetupResponse> CREATOR = findCreator(ImportCredentialsForDeviceSetupResponse.class);
}
