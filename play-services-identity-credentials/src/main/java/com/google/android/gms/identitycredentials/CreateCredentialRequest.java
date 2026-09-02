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

/**
 * Data interface for creating or saving a user credential.
 */
@SafeParcelable.Class
public class CreateCredentialRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getType")
    @NonNull
    private final String type;
    @Field(value = 2, getterName = "getCredentialData")
    @NonNull
    private final Bundle credentialData;
    @Field(value = 3, getterName = "getCandidateQueryData")
    @NonNull
    private final Bundle candidateQueryData;
    @Field(value = 4, getterName = "getOrigin")
    @Nullable
    private final String origin;
    @Field(value = 5, getterName = "getRequestJson")
    @Nullable
    private final String requestJson;
    @Field(value = 6, getterName = "getResultReceiver")
    @Nullable
    private final ResultReceiver resultReceiver;

    /**
     * constructs an instance of {@link CreateCredentialRequest}
     *
     * @param type               the type of the credential to be created or saved
     * @param credentialData     the complete request data in {@link Bundle} format, consisting of all the data that will be sent to the provider during the final credential creation stage
     * @param candidateQueryData the partial request data in {@link Bundle} format, that will be sent to the provider during the initial candidate query stage, which will not contain sensitive user information
     * @param origin             the origin of the request, only settable by a browser
     */
    @Constructor
    public CreateCredentialRequest(@Param(1) @NonNull String type, @Param(2) @NonNull Bundle credentialData, @Param(3) @NonNull Bundle candidateQueryData, @Param(4) @Nullable String origin, @Param(5) @Nullable String requestJson, @Param(6) @Nullable ResultReceiver resultReceiver) {
        this.type = type;
        this.credentialData = credentialData;
        this.candidateQueryData = candidateQueryData;
        this.origin = origin;
        this.requestJson = requestJson;
        this.resultReceiver = resultReceiver;
    }

    /**
     * the partial request data in {@link Bundle} format, that will be sent to the provider during the initial candidate query stage, which will not contain sensitive user information
     */
    @NonNull
    public Bundle getCandidateQueryData() {
        return candidateQueryData;
    }

    /**
     * the complete request data in {@link Bundle} format, consisting of all the data that will be sent to the provider during the final credential creation stage
     */
    @NonNull
    public Bundle getCredentialData() {
        return credentialData;
    }

    /**
     * the origin of the request, only settable by a browser
     */
    @Nullable
    public String getOrigin() {
        return origin;
    }

    @Nullable
    public String getRequestJson() {
        return requestJson;
    }

    @Nullable
    public ResultReceiver getResultReceiver() {
        return resultReceiver;
    }

    /**
     * the type of the credential to be created or saved
     */
    @NonNull
    public String getType() {
        return type;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CreateCredentialRequest> CREATOR = findCreator(CreateCredentialRequest.class);
}
