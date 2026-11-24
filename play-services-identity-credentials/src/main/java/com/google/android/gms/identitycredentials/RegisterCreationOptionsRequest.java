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

/**
 * A registration request to store provision / creation candidates' metadata and matcher logic.
 */
@SafeParcelable.Class
public class RegisterCreationOptionsRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getCreateOptions")
    @NonNull
    private final byte[] createOptions;

    @Field(value = 2, getterName = "getMatcher")
    @NonNull
    private final byte[] matcher;

    @Field(value = 3, getterName = "getType")
    @NonNull
    private final String type;

    @Field(value = 4, getterName = "getId")
    @NonNull
    private final String id;

    @Field(value = 5, getterName = "getFulfillmentActionName", defaultValue = "")
    @NonNull
    private final String fulfillmentActionName;

    /**
     * constructs an instance of {@link RegisterCreationOptionsRequest}
     *
     * @param createOptions         the creation candidates data used for display and matching purpose, as a ByteArray blob
     * @param matcher               the matcher for the credential info, as a ByteArray blob
     * @param type                  the type of the creation options registered, matching the {@code CreateCredentialRequest.type} that this registry can handle
     * @param id                    the id of the given registry data, so as not to overwrite existing data of different id
     * @param fulfillmentActionName optionally specify a different intent action to be used for launching the fulfillment activity when one of the registered credentials is selected by the user; otherwise, the default action {@code "androidx.credentials.registry.provider.action.CREATE_CREDENTIAL"} will be used
     */
    @Constructor
    public RegisterCreationOptionsRequest(@NonNull @Param(1) byte[] createOptions, @NonNull @Param(2) byte[] matcher, @NonNull @Param(3) String type, @NonNull @Param(4) String id, @NonNull @Param(5) String fulfillmentActionName) {
        this.createOptions = createOptions;
        this.matcher = matcher;
        this.type = type;
        this.id = id;
        this.fulfillmentActionName = fulfillmentActionName;
    }

    /**
     * the creation candidates data used for display and matching purpose, as a ByteArray blob
     */
    @NonNull
    public byte[] getCreateOptions() {
        return createOptions;
    }

    /**
     * optionally specify a different intent action to be used for launching the fulfillment activity when one of the registered credentials is selected
     * by the user; otherwise, the default action {@code "androidx.credentials.registry.provider.action.CREATE_CREDENTIAL"} will be used
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
     * the matcher for the credential info, as a ByteArray blob
     */
    @NonNull
    public byte[] getMatcher() {
        return matcher;
    }

    /**
     * the type of the creation options registered, matching the {@code CreateCredentialRequest.type} that this registry can handle
     */
    @NonNull
    public String getType() {
        return type;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<RegisterCreationOptionsRequest> CREATOR = findCreator(RegisterCreationOptionsRequest.class);
}
