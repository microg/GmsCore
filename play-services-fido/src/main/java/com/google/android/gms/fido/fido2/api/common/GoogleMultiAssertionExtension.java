/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.PublicApi;

@PublicApi
@SafeParcelable.Class
public class GoogleMultiAssertionExtension extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "isRequestForMultiAssertion")
    private final boolean requestForMultiAssertion;

    @Constructor
    public GoogleMultiAssertionExtension(@Param(1) boolean requestForMultiAssertion) {
        this.requestForMultiAssertion = requestForMultiAssertion;
    }

    public boolean isRequestForMultiAssertion() {
        return requestForMultiAssertion;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GoogleMultiAssertionExtension> CREATOR = AbstractSafeParcelable.findCreator(GoogleMultiAssertionExtension.class);
}
