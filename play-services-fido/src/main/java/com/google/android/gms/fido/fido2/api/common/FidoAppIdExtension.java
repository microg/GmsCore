/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.PublicApi;

import java.util.Arrays;

/**
 * Extension for FIDO appId, to support U2F backward compatibility in FIDO2 assertion requests.
 * <p>
 * This authentication extension allows Relying Parties that have previously registered a credential using the legacy
 * FIDO U2F APIs to request an assertion. Specifically, this extension allows Relying Parties to specify an appId to
 * overwrite the computed rpId for U2F authenticators.
 * <p>
 * Note that this extension is only valid if used during the get() call; other usage should result in client error.
 */
@PublicApi
@SafeParcelable.Class
public class FidoAppIdExtension extends AbstractSafeParcelable {
    @Field(value = 2, getterName = "getAppId")
    @NonNull
    private String appId;

    private FidoAppIdExtension() {
    }

    @Constructor
    public FidoAppIdExtension(@Param(2) @NonNull String appId) {
        this.appId = appId;
    }

    @NonNull
    public String getAppId() {
        return appId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FidoAppIdExtension)) return false;

        FidoAppIdExtension that = (FidoAppIdExtension) o;

        return appId.equals(that.appId);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{appId});
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FidoAppIdExtension> CREATOR = findCreator(FidoAppIdExtension.class);
}
