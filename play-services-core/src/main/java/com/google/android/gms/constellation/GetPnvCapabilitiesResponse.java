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

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class GetPnvCapabilitiesResponse extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<GetPnvCapabilitiesResponse> CREATOR = findCreator(GetPnvCapabilitiesResponse.class);

    @SafeParcelable.Field(1)
    @Nullable
    public final List<SimCapability> simCapabilities;

    @SafeParcelable.Constructor
    public GetPnvCapabilitiesResponse(
            @SafeParcelable.Param(1) @Nullable List<SimCapability> simCapabilities) {
        this.simCapabilities = simCapabilities;
    }

    @SafeParcelable.Class
    public static class SimCapability extends AbstractSafeParcelable {
        public static SafeParcelableCreatorAndWriter<SimCapability> CREATOR = findCreator(SimCapability.class);

        @SafeParcelable.Field(1)
        public final int slotValue;

        @SafeParcelable.Field(2)
        @Nullable
        public final String subscriberIdDigest;

        @SafeParcelable.Field(3)
        public final int carrierId;

        @SafeParcelable.Field(4)
        @Nullable
        public final String operatorName;

        @SafeParcelable.Field(5)
        @Nullable
        public final List<VerificationCapability> verificationCapabilities;

        @SafeParcelable.Constructor
        public SimCapability(
                @SafeParcelable.Param(1) int slotValue,
                @SafeParcelable.Param(2) @Nullable String subscriberIdDigest,
                @SafeParcelable.Param(3) int carrierId,
                @SafeParcelable.Param(4) @Nullable String operatorName,
                @SafeParcelable.Param(5) @Nullable List<VerificationCapability> verificationCapabilities) {
            this.slotValue = slotValue;
            this.subscriberIdDigest = subscriberIdDigest;
            this.carrierId = carrierId;
            this.operatorName = operatorName;
            this.verificationCapabilities = verificationCapabilities;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }
    }

    @SafeParcelable.Class
    public static class VerificationCapability extends AbstractSafeParcelable {
        public static SafeParcelableCreatorAndWriter<VerificationCapability> CREATOR = findCreator(VerificationCapability.class);

        @SafeParcelable.Field(1)
        public final int verificationMethod;

        @SafeParcelable.Field(2)
        public final int statusValue;

        @SafeParcelable.Constructor
        public VerificationCapability(
                @SafeParcelable.Param(1) int verificationMethod,
                @SafeParcelable.Param(2) int statusValue) {
            this.verificationMethod = verificationMethod;
            this.statusValue = statusValue;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
