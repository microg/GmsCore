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
 * Represents a user credential that can be used to authenticate to your app.
 */
@SafeParcelable.Class
public class Credential extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getType")
    @NonNull
    public final String type;
    @Field(value = 2, getterName = "getData")
    @NonNull
    public final Bundle data;

    /**
     * constructs an instance of Credential
     * @param type the type of the credential
     * @param data the data associated with the credential
     */
    @Constructor
    public Credential(@Param(1) @NonNull String type, @Param(2) @NonNull Bundle data) {
        this.type = type;
        this.data = data;
    }

    /**
     * the data associated with the credential
     */
    @NonNull
    public Bundle getData() {
        return data;
    }

    /**
     * the type of the credential
     */
    @NonNull
    public String getType() {
        return type;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Credential> CREATOR = findCreator(Credential.class);
}
