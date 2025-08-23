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
public class GoogleSessionIdExtension extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getSessionId")
    private final long sessionId;

    @Constructor
    public GoogleSessionIdExtension(@Param(1) long sessionId) {
        this.sessionId = sessionId;
    }

    public long getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return ToStringHelper.name("GoogleSessionIdExtension").field("sessionId", sessionId).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GoogleSessionIdExtension> CREATOR = AbstractSafeParcelable.findCreator(GoogleSessionIdExtension.class);
}
