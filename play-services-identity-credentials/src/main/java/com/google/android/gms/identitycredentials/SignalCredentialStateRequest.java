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
 * Data interface for signaling a user's credential state from the RPs to the credential providers.
 */
@SafeParcelable.Class
public class SignalCredentialStateRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getType")
    @NonNull
    private final String type;

    @Field(value = 2, getterName = "getOrigin")
    @Nullable
    private final String origin;

    @Field(value = 3, getterName = "getRequestData")
    @NonNull
    private final Bundle requestData;

    /**
     * constructs an instance of {@link SignalCredentialStateRequest}
     *
     * @param type        the type of signal request being sent to the provider
     * @param origin      the origin of the request, only settable by a browser
     * @param requestData the request data, containing the credential state information
     */
    @Constructor
    public SignalCredentialStateRequest(@NonNull @Param(1) String type, @Param(2) @Nullable String origin, @NonNull @Param(3) Bundle requestData) {
        this.type = type;
        this.origin = origin;
        this.requestData = requestData;
    }

    /**
     * the origin of the request, only settable by a browser
     */
    @Nullable
    public String getOrigin() {
        return origin;
    }

    /**
     * the request data, containing the credential state information
     */
    @NonNull
    public Bundle getRequestData() {
        return requestData;
    }

    /**
     * the type of signal request being sent to the provider
     */
    @NonNull
    public String getType() {
        return type;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SignalCredentialStateRequest> CREATOR = findCreator(SignalCredentialStateRequest.class);
}
