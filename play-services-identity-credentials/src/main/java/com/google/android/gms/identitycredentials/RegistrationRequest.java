/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.identitycredentials;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

/**
 * A registration request to store credential metadata and matcher logic.
 */
@SafeParcelable.Class
public class RegistrationRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getCredentials")
    @NonNull
    private final byte[] credentials;
    @Field(value = 2, getterName = "getMatcher")
    @NonNull
    private final byte[] matcher;
    @Field(value = 3, getterName = "getType", defaultValue = "\"\"")
    @NonNull
    private final String type;
    @Field(value = 4, getterName = "getRequestType", defaultValue = "\"\"")
    @NonNull
    @Deprecated
    private final String requestType;
    @Field(value = 5, getterName = "getProtocolTypes", defaultValue = "java.util.Collections.emptyList()")
    @NonNull
    @Deprecated
    private final List<String> protocolTypes;
    @Field(value = 6, getterName = "getId", defaultValue = "\"\"")
    @NonNull
    private final String id;

    @Field(value = 7, getterName = "getFulfillmentActionName", defaultValue = "\"\"")
    @NonNull
    private final String fulfillmentActionName;

    public RegistrationRequest(@NonNull byte[] credentials, @NonNull byte[] matcher, @NonNull String type, @Deprecated @NonNull String requestType, @Deprecated @NonNull List<String> protocolTypes) {
        this(credentials, matcher, type, requestType, protocolTypes, "", "");
    }

    /**
     * constructs an instance of {@link RegistrationRequest}
     *
     * @param credentials           the credential information as a ByteArray blob
     * @param matcher               the matcher for the credential info, also as a ByteArray blob
     * @param type                  the type of credentials matching the given registry data
     * @param requestType           deprecated
     * @param protocolTypes         deprecated
     * @param id                    the id of the given registry data, so as not to overwrite existing data of different id
     * @param fulfillmentActionName optionally specify a different intent action to be used for launching the fulfillment activity when one of the registered credentials is selected by the user; otherwise, the default action {@code "androidx.credentials.registry.provider.action.GET_CREDENTIAL"} will be used
     */
    @Constructor
    public RegistrationRequest(@NonNull @Param(1) byte[] credentials, @NonNull @Param(2) byte[] matcher, @NonNull @Param(3) String type, @Deprecated @NonNull @Param(4) String requestType, @Deprecated @NonNull @Param(5) List<String> protocolTypes, @NonNull @Param(6) String id, @NonNull @Param(7) String fulfillmentActionName) {
        this.credentials = credentials;
        this.matcher = matcher;
        this.type = type;
        this.requestType = requestType;
        this.protocolTypes = protocolTypes;
        this.id = id;
        this.fulfillmentActionName = fulfillmentActionName;
    }

    /**
     * the credential information as a ByteArray blob
     */
    @NonNull
    public byte[] getCredentials() {
        return credentials;
    }

    /**
     * optionally specify a different intent action to be used for launching the fulfillment activity when one of the registered credentials is selected
     * by the user; otherwise, the default action {@code "androidx.credentials.registry.provider.action.GET_CREDENTIAL"} will be used
     */
    @NonNull
    public String getFulfillmentActionName() {
        return fulfillmentActionName;
    }

    /**
     * the id of the given registry data, so as not to overwrite existing data of different id
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * the matcher for the credential info, also as a ByteArray blob
     */
    @NonNull
    public byte[] getMatcher() {
        return matcher;
    }

    /**
     * @deprecated
     */
    @Deprecated
    @NonNull
    public List<String> getProtocolTypes() {
        return protocolTypes;
    }

    /**
     * @deprecated
     */
    @Deprecated
    @NonNull
    public String getRequestType() {
        return requestType;
    }

    /**
     * the type of credentials matching the given registry data
     */
    @NonNull
    public String getType() {
        return type;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<RegistrationRequest> CREATOR = findCreator(RegistrationRequest.class);
}
