package com.google.android.gms.constellation;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class VerifyPhoneNumberRequest extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<VerifyPhoneNumberRequest> CREATOR =
            findCreator(VerifyPhoneNumberRequest.class);

    @Field(1)
    public final String policyId;

    @Field(2)
    public final long timeout;

    @Field(3)
    public final IdTokenRequest idTokenRequest;

    @Field(4)
    public final Bundle extras;

    @Field(5)
    public final List<ImsiRequest> targetedSims;

    @Field(6)
    public final boolean includeUnverified;

    @Field(7)
    public final int apiVersion;

    @Field(8)
    public final List<Integer> verificationMethodsValues;

    @Constructor
    public VerifyPhoneNumberRequest(
            @Param(1) String policyId,
            @Param(2) long timeout,
            @Param(3) IdTokenRequest idTokenRequest,
            @Param(4) Bundle extras,
            @Param(5) List<ImsiRequest> targetedSims,
            @Param(6) boolean includeUnverified,
            @Param(7) int apiVersion,
            @Param(8) List<Integer> verificationMethodsValues
    ) {
        this.policyId = policyId;
        this.timeout = timeout;
        this.idTokenRequest = idTokenRequest;
        this.extras = extras;
        this.targetedSims = targetedSims;
        this.includeUnverified = includeUnverified;
        this.apiVersion = apiVersion;
        this.verificationMethodsValues = verificationMethodsValues;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @SafeParcelable.Class
    public static class IdTokenRequest extends AbstractSafeParcelable {
        public static SafeParcelableCreatorAndWriter<IdTokenRequest> CREATOR =
                findCreator(IdTokenRequest.class);
        @Field(1)
        public final String idToken;
        @Field(2)
        public final String subscriberHash;

        @Constructor
        public IdTokenRequest(
                @Param(1) String idToken,
                @Param(2) String subscriberHash
        ) {
            this.idToken = idToken;
            this.subscriberHash = subscriberHash;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }
    }

    @SafeParcelable.Class
    public static class ImsiRequest extends AbstractSafeParcelable {
        public static SafeParcelableCreatorAndWriter<ImsiRequest> CREATOR =
                findCreator(ImsiRequest.class);
        @Field(1)
        public final String imsi;
        @Field(2)
        public final String phoneNumberHint;

        @Constructor
        public ImsiRequest(
                @Param(1) String imsi,
                @Param(2) String phoneNumberHint
        ) {
            this.imsi = imsi;
            this.phoneNumberHint = phoneNumberHint;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }
    }
}