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
public class HmacSecretExtension extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getCoseKeyAgreement")
    private final byte[] coseKeyAgreement;

    @Field(value = 2, getterName = "getSaltEnc")
    private final byte[] saltEnc;

    @Field(value = 3, getterName = "getSaltAuth")
    private final byte[] saltAuth;

    @Field(value = 4, getterName = "getPinUvAuthProtocol")
    private final int pinUvAuthProtocol;

    @Constructor
    public HmacSecretExtension(@Param(1) byte[] coseKeyAgreement, @Param(2) byte[] saltEnc, @Param(3) byte[] saltAuth, @Param(4) int pinUvAuthProtocol) {
        this.coseKeyAgreement = coseKeyAgreement;
        this.saltEnc = saltEnc;
        this.saltAuth = saltAuth;
        this.pinUvAuthProtocol = pinUvAuthProtocol;
    }

    public byte[] getCoseKeyAgreement() {
        return coseKeyAgreement;
    }

    public byte[] getSaltEnc() {
        return saltEnc;
    }

    public byte[] getSaltAuth() {
        return saltAuth;
    }

    public int getPinUvAuthProtocol() {
        return pinUvAuthProtocol;
    }

    @Override
    public String toString() {
        return ToStringHelper.name("HmacSecretExtension").field("coseKeyAgreement", coseKeyAgreement == null ? "" : coseKeyAgreement.length).field("saltEnc", saltEnc == null ? "" : saltEnc.length).field("saltAuth", saltAuth == null ? "" : saltAuth.length).field("pinUvAuthProtocol", pinUvAuthProtocol).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<HmacSecretExtension> CREATOR = AbstractSafeParcelable.findCreator(HmacSecretExtension.class);
}
