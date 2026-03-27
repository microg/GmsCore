package com.google.android.gms.constellation;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class GetPnvCapabilitiesResponse extends AbstractSafeParcelable {
    @Field(1)
    public final List<SimCapability> simCapabilities;

    @Constructor
    public GetPnvCapabilitiesResponse(
            @Param(1) List<SimCapability> simCapabilities
    ) {
        this.simCapabilities = simCapabilities;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static SafeParcelableCreatorAndWriter<GetPnvCapabilitiesResponse> CREATOR =
            findCreator(GetPnvCapabilitiesResponse.class);

    @SafeParcelable.Class
    public static class SimCapability extends AbstractSafeParcelable {

        @Field(1)
        public final int slotValue;

        @Field(2)
        public final String subscriberIdDigest;

        @Field(3)
        public final int carrierId;

        @Field(4)
        public final String operatorName;

        @Field(5)
        public final List<VerificationCapability> verificationCapabilities;

        @Constructor
        public SimCapability(
                @Param(1) int slotValue,
                @Param(2) String subscriberIdDigest,
                @Param(3) int carrierId,
                @Param(4) String operatorName,
                @Param(5) List<VerificationCapability> verificationCapabilities
        ) {
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

        public static SafeParcelableCreatorAndWriter<SimCapability> CREATOR =
                findCreator(SimCapability.class);
    }

    @SafeParcelable.Class
    public static class VerificationCapability extends AbstractSafeParcelable {

        @Field(1)
        public final int verificationMethod;

        @Field(2)
        public final int statusValue;

        @Constructor
        public VerificationCapability(
                @Param(1) int verificationMethod,
                @Param(2) int statusValue
        ) {
            this.verificationMethod = verificationMethod;
            this.statusValue = statusValue;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static SafeParcelableCreatorAndWriter<VerificationCapability> CREATOR =
                findCreator(VerificationCapability.class);
    }
}