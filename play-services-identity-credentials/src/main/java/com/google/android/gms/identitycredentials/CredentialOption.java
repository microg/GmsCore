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

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

/**
 * Base class for getting a specific type of credentials.
 * <p>
 * {@link GetCredentialRequest} will be composed of a list of {@link CredentialOption} subclasses to indicate the specific credential types and
 * configurations that your app accepts.
 */
@SafeParcelable.Class
public class CredentialOption extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getType")
    private final String type;
    @Field(value = 2, getterName = "getCredentialRetrievalData")
    private final Bundle credentialRetrievalData;
    @Field(value = 3, getterName = "getCandidateQueryData")
    private final Bundle candidateQueryData;
    @Field(value = 4, getterName = "getRequestMatcher")
    private final String requestMatcher;
    @Field(value = 5, getterName = "getRequestType")
    @Deprecated
    private final String requestType;
    @Field(value = 6, getterName = "getProtocolType")
    @Deprecated
    private final String protocolType;

    /**
     * constructs an instance of {@link CredentialOption}
     *
     * @param type                    the type of the credential to be requested
     * @param credentialRetrievalData the retrieval data in {@link Bundle} format
     * @param candidateQueryData      the partial request data in the {@link Bundle} format that will be sent to the provider during the initial candidate query stage, which will not contain sensitive user information
     * @param requestMatcher          the criteria used to filter the request (for instance, age 21)
     * @param requestType             deprecated
     * @param protocolType            deprecated
     */
    @Constructor
    public CredentialOption(@Param(1) String type, @Param(2) Bundle credentialRetrievalData, @Param(3) Bundle candidateQueryData, @Param(4) String requestMatcher, @Deprecated @Param(5) String requestType, @Deprecated @Param(6) String protocolType) {
        this.type = type;
        this.credentialRetrievalData = credentialRetrievalData;
        this.candidateQueryData = candidateQueryData;
        this.requestMatcher = requestMatcher;
        this.requestType = requestType;
        this.protocolType = protocolType;
    }

    /**
     * the partial request data in the {@link Bundle} format that will be sent to the provider during the initial candidate query stage, which will not contain sensitive user information
     */
    public Bundle getCandidateQueryData() {
        return candidateQueryData;
    }

    /**
     * the retrieval data in {@link Bundle} format
     */
    public Bundle getCredentialRetrievalData() {
        return credentialRetrievalData;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getProtocolType() {
        return protocolType;
    }

    /**
     * the criteria used to filter the request (for instance, age 21)
     */
    public String getRequestMatcher() {
        return requestMatcher;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getRequestType() {
        return requestType;
    }

    /**
     * the type of the credential to be requested
     */
    public String getType() {
        return type;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CredentialOption> CREATOR = findCreator(CredentialOption.class);
}
