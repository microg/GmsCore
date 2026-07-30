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

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class SetAsterismConsentResponse extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<SetAsterismConsentResponse> CREATOR = findCreator(SetAsterismConsentResponse.class);

    @SafeParcelable.Field(1)
    public final int requestCode;

    @SafeParcelable.Field(2)
    @Nullable
    public final String gmscoreIidToken;

    @SafeParcelable.Field(3)
    @Nullable
    public final String fid;

    @SafeParcelable.Constructor
    public SetAsterismConsentResponse(
            @SafeParcelable.Param(1) int requestCode,
            @SafeParcelable.Param(2) @Nullable String gmscoreIidToken,
            @SafeParcelable.Param(3) @Nullable String fid) {
        this.requestCode = requestCode;
        this.gmscoreIidToken = gmscoreIidToken;
        this.fid = fid;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
