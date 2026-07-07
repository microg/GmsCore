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
import android.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

/**
 * Data interface for retrieving a user credential.
 */
@SafeParcelable.Class
public class GetCredentialRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getCredentialOptions")
    @NonNull
    private final List<CredentialOption> credentialOptions;
    @Field(value = 2, getterName = "getData")
    @NonNull
    private final Bundle data;
    @Field(value = 3, getterName = "getOrigin")
    @Nullable
    private final String origin;
    @Field(value = 4, getterName = "getResultReceiver")
    @Deprecated
    @NonNull
    private final ResultReceiver resultReceiver;

    /**
     * constructs an instance of {@link GetCredentialRequest}
     *
     * @param credentialOptions the list of credential options
     * @param data              the additional data to be used for retrieving the credential
     * @param origin            the origin of the request, only settable by a browser
     * @param resultReceiver    deprecated
     */
    @Constructor
    public GetCredentialRequest(@NonNull @Param(1) List<CredentialOption> credentialOptions, @NonNull @Param(2) Bundle data, @Nullable @Param(3) String origin, @Deprecated @NonNull @Param(4) ResultReceiver resultReceiver) {
        this.credentialOptions = credentialOptions;
        this.data = data;
        this.origin = origin;
        this.resultReceiver = resultReceiver;
    }

    /**
     * the list of credential options
     */
    @NonNull
    public List<CredentialOption> getCredentialOptions() {
        return credentialOptions;
    }

    /**
     * the additional data to be used for retrieving the credential
     */
    @NonNull
    public Bundle getData() {
        return data;
    }

    /**
     * the origin of the request, only settable by a browser
     */
    @Nullable
    public String getOrigin() {
        return origin;
    }

    /**
     * @deprecated
     */
    @Deprecated
    @NonNull
    public ResultReceiver getResultReceiver() {
        return resultReceiver;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetCredentialRequest> CREATOR = findCreator(GetCredentialRequest.class);
}
