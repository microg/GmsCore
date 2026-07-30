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

package com.google.android.gms.constellation;

import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class PhoneNumberInfo extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<PhoneNumberInfo> CREATOR = findCreator(PhoneNumberInfo.class);

    @SafeParcelable.Field(1)
    public final int version;

    @SafeParcelable.Field(2)
    @Nullable
    public final String phoneNumber;

    @SafeParcelable.Field(3)
    public final long verificationTime;

    @SafeParcelable.Field(4)
    @Nullable
    public final Bundle extras;

    @SafeParcelable.Constructor
    public PhoneNumberInfo(
            @SafeParcelable.Param(1) int version,
            @SafeParcelable.Param(2) @Nullable String phoneNumber,
            @SafeParcelable.Param(3) long verificationTime,
            @SafeParcelable.Param(4) @Nullable Bundle extras) {
        this.version = version;
        this.phoneNumber = phoneNumber;
        this.verificationTime = verificationTime;
        this.extras = extras;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
