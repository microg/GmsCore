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

/**
 * Request for fetching the state of the credentials in the primary provider
 */
@SafeParcelable.Class
public class GetCredentialTransferCapabilitiesRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getRequestData")
    private final Bundle requestData;

    /**
     * @param requestData the request bundle
     */
    @Constructor
    public GetCredentialTransferCapabilitiesRequest(@Param(1) Bundle requestData) {
        this.requestData = requestData;
    }

    /**
     * the request bundle
     */
    public Bundle getRequestData() {
        return requestData;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetCredentialTransferCapabilitiesRequest> CREATOR = findCreator(GetCredentialTransferCapabilitiesRequest.class);
}
