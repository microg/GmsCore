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
public class AsterismConsent extends AbstractSafeParcelable {
    @Field(1)
    public int consentStatus;

    @Field(2)
    public long timestamp;

    @Field(3)
    public int tosVersion;

    @Field(4)
    public String accountName;

    @Field(5)
    public byte[] consentToken;

    public AsterismConsent() {
    }

    public AsterismConsent(int consentStatus, long timestamp, int tosVersion, String accountName, byte[] consentToken) {
        this.consentStatus = consentStatus;
        this.timestamp = timestamp;
        this.tosVersion = tosVersion;
        this.accountName = accountName;
        this.consentToken = consentToken;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("AsterismConsent")
                .field("consentStatus", consentStatus)
                .field("timestamp", timestamp)
                .field("tosVersion", tosVersion)
                .field("accountName", accountName)
                .field("consentToken", consentToken)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AsterismConsent> CREATOR = findCreator(AsterismConsent.class);
}
