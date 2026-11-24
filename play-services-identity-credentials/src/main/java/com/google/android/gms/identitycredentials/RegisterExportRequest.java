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
 * A registration request for declaring that the callee is a credential provider that supports exporting of credentials to other credential providers
 */
@SafeParcelable.Class
public class RegisterExportRequest extends AbstractSafeParcelable {
    @NonNull
    public static final String REQUEST_TYPE = "androidx.identitycredentials.TYPE_CREDENTIALS_SYNC";

    @Field(value = 1, getterName = "getMatcher")
    @NonNull
    private final byte[] matcher;
    @Field(value = 2, getterName = "getData")
    @NonNull
    private final byte[] data;
    @Field(value = 3, getterName = "getId")
    @NonNull
    private final String id;

    /**
     * @param matcher the matcher executor that runs the matching logic
     * @param data    any data to be registered along with the ability to export, typically empty for this use-case
     * @param id      the ID of the given registry data, so as not to overwrite existing data of different ID
     */
    @Constructor
    public RegisterExportRequest(@NonNull @Param(1) byte[] matcher, @NonNull @Param(2) byte[] data, @NonNull @Param(3) String id) {
        this.matcher = matcher;
        this.data = data;
        this.id = id;
    }

    /**
     * any data to be registered along with the ability to export, typically empty for this use-case
     */
    @NonNull
    public byte[] getData() {
        return data;
    }

    /**
     * the ID of the given registry data, so as not to overwrite existing data of different ID
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * the matcher executor that runs the matching logic
     */
    @NonNull
    public byte[] getMatcher() {
        return matcher;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<RegisterExportRequest> CREATOR = findCreator(RegisterExportRequest.class);
}
