/*
 * Copyright (C) 2013-2026 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.asterism;

import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class SetAsterismConsentRequest extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<SetAsterismConsentRequest> CREATOR = findCreator(SetAsterismConsentRequest.class);

    @SafeParcelable.Field(1)
    public final int requestCode;

    @SafeParcelable.Field(2)
    public final int asterismClientValue;

    @SafeParcelable.Field(3)
    public final int flowContextValue;

    @SafeParcelable.Field(4)
    @Nullable
    public final int[] tosResourceIds;

    @SafeParcelable.Field(5)
    @Nullable
    public final Long timestamp;

    @SafeParcelable.Field(6)
    public final int consentValue;

    @SafeParcelable.Field(7)
    @Nullable
    public final Bundle extras;

    @SafeParcelable.Field(8)
    public final int statusValue;

    @SafeParcelable.Field(9)
    @Nullable
    public final String clientVersion;

    @SafeParcelable.Field(10)
    @Nullable
    public final String language;

    @SafeParcelable.Field(11)
    @Nullable
    public final String field11;

    @SafeParcelable.Field(12)
    @Nullable
    public final String field12;

    @SafeParcelable.Field(13)
    @Nullable
    public final String field13;

    @SafeParcelable.Field(14)
    @Nullable
    public final String accountName;

    @SafeParcelable.Field(15)
    @Nullable
    public final String consentVariant;

    @SafeParcelable.Field(16)
    @Nullable
    public final String consentTrigger;

    @SafeParcelable.Field(17)
    public final int consentVersionValue;

    @SafeParcelable.Field(18)
    public final int deviceConsentSourceValue;

    @SafeParcelable.Field(19)
    public final int deviceConsentVersionValue;

    @SafeParcelable.Constructor
    public SetAsterismConsentRequest(
            @SafeParcelable.Param(1) int requestCode,
            @SafeParcelable.Param(2) int asterismClientValue,
            @SafeParcelable.Param(3) int flowContextValue,
            @SafeParcelable.Param(4) @Nullable int[] tosResourceIds,
            @SafeParcelable.Param(5) @Nullable Long timestamp,
            @SafeParcelable.Param(6) int consentValue,
            @SafeParcelable.Param(7) @Nullable Bundle extras,
            @SafeParcelable.Param(8) int statusValue,
            @SafeParcelable.Param(9) @Nullable String clientVersion,
            @SafeParcelable.Param(10) @Nullable String language,
            @SafeParcelable.Param(11) @Nullable String field11,
            @SafeParcelable.Param(12) @Nullable String field12,
            @SafeParcelable.Param(13) @Nullable String field13,
            @SafeParcelable.Param(14) @Nullable String accountName,
            @SafeParcelable.Param(15) @Nullable String consentVariant,
            @SafeParcelable.Param(16) @Nullable String consentTrigger,
            @SafeParcelable.Param(17) int consentVersionValue,
            @SafeParcelable.Param(18) int deviceConsentSourceValue,
            @SafeParcelable.Param(19) int deviceConsentVersionValue) {
        this.requestCode = requestCode;
        this.asterismClientValue = asterismClientValue;
        this.flowContextValue = flowContextValue;
        this.tosResourceIds = tosResourceIds;
        this.timestamp = timestamp;
        this.consentValue = consentValue;
        this.extras = extras;
        this.statusValue = statusValue;
        this.clientVersion = clientVersion;
        this.language = language;
        this.field11 = field11;
        this.field12 = field12;
        this.field13 = field13;
        this.accountName = accountName;
        this.consentVariant = consentVariant;
        this.consentTrigger = consentTrigger;
        this.consentVersionValue = consentVersionValue;
        this.deviceConsentSourceValue = deviceConsentSourceValue;
        this.deviceConsentVersionValue = deviceConsentVersionValue;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
