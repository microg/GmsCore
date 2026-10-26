/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class SetAsterismConsentRequest extends AbstractSafeParcelable {
    @Field(1)
    public String packageName;

    @Field(2)
    public String accountName;

    @Field(3)
    public AsterismConsent consent;

    @Field(4)
    public int consentStatus;

    public SetAsterismConsentRequest() {
    }

    public SetAsterismConsentRequest(String packageName, String accountName, AsterismConsent consent, int consentStatus) {
        this.packageName = packageName;
        this.accountName = accountName;
        this.consent = consent;
        this.consentStatus = consentStatus;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SetAsterismConsentRequest")
                .field("packageName", packageName)
                .field("accountName", accountName)
                .field("consent", consent)
                .field("consentStatus", consentStatus)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SetAsterismConsentRequest> CREATOR = findCreator(SetAsterismConsentRequest.class);
}
