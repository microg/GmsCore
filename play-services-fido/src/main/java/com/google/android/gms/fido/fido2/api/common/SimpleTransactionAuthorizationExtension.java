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
import org.microg.gms.utils.ToStringHelper;

@PublicApi
@SafeParcelable.Class
public class SimpleTransactionAuthorizationExtension extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getText")
    private final String text;

    @Constructor
    public SimpleTransactionAuthorizationExtension(@Param(1) String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return ToStringHelper.name("SimpleTransactionAuthorizationExtension").field("text", text).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SimpleTransactionAuthorizationExtension> CREATOR = AbstractSafeParcelable.findCreator(SimpleTransactionAuthorizationExtension.class);
}
