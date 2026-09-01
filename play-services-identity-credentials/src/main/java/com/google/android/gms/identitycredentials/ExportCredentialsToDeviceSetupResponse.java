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
 * Response for exporting the credentials to the primary provider
 */
@SafeParcelable.Class
public class ExportCredentialsToDeviceSetupResponse extends AbstractSafeParcelable {
    @Field(1)
    @NonNull
    public final Bundle responseBundle;

    /**
     * @param responseBundle the bundle containing response extras.
     */
    @Constructor
    public ExportCredentialsToDeviceSetupResponse(@NonNull @Param(1) Bundle responseBundle) {
        this.responseBundle = responseBundle;
    }

    /**
     * Returns the number of credentials failed to stored.
     *
     * @deprecated
     */
    @Deprecated
    @Nullable
    public final Integer getNumFailure() {
        if (this.responseBundle.containsKey("NUM_FAILURE")) {
            return this.responseBundle.getInt("NUM_FAILURE");
        }
        return null;
    }

    /**
     * Returns the number of credentials that were ignored by the provider.
     *
     * @deprecated
     */
    @Deprecated
    @Nullable
    public final Integer getNumIgnored() {
        if (this.responseBundle.containsKey("NUM_IGNORED")) {
            return this.responseBundle.getInt("NUM_IGNORED");
        }
        return null;
    }

    /**
     * Returns the number of credentials successfully stored.
     *
     * @deprecated
     */
    @Deprecated
    @Nullable
    public final Integer getNumSuccess() {
        if (this.responseBundle.containsKey("NUM_SUCCESS")) {
            return this.responseBundle.getInt("NUM_SUCCESS");
        }
        return null;
    }

    /**
     * Returns the provider app info.
     *
     * @deprecated
     */
    @Deprecated
    @Nullable
    public final CallingAppInfoParcelable getProviderAppInfo() {
        return this.responseBundle.getParcelable("PROVIDER_APP_INFO");
    }

    /**
     * the bundle containing response extras.
     */
    @NonNull
    public Bundle getResponseBundle() {
        return responseBundle;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ExportCredentialsToDeviceSetupResponse> CREATOR = findCreator(ExportCredentialsToDeviceSetupResponse.class);
}
