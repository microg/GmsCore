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
    public static SafeParcelableCreatorAndWriter<SetAsterismConsentRequest> CREATOR =
            findCreator(SetAsterismConsentRequest.class);

    @Field(1)
    public final int requestCode;

    @Field(2)
    public final int asterismClientValue;

    @Field(3)
    public final int flowContextValue;

    @Field(4)
    @Nullable
    public final int[] tosResourceIds;

    @Field(5)
    @Nullable
    public final Long timestamp;

    @Field(6)
    public final int consentValue;

    @Field(7)
    @Nullable
    public final Bundle extras;

    @Field(8)
    public final int statusValue;

    @Field(9)
    @Nullable
    public final String tosUrl;

    @Field(10)
    @Nullable
    public final String language;

    @Field(11)
    @Nullable
    public final String country;

    @Field(12)
    @Nullable
    public final String tosVersion;

    @Field(13)
    @Nullable
    public final String tosContentTitle;

    @Field(14)
    @Nullable
    public final String accountName;

    @Field(15)
    @Nullable
    public final String consentVariant;

    @Field(16)
    @Nullable
    public final String consentTrigger;

    @Field(17)
    public final int rcsFlowContextValue;

    @Field(18)
    public final int deviceConsentSourceValue;

    @Field(19)
    public final int deviceConsentVersionValue;

    @Constructor
    public SetAsterismConsentRequest(
            @Param(1) int requestCode,
            @Param(2) int asterismClientValue,
            @Param(3) int flowContextValue,
            @Param(4) @Nullable int[] tosResourceIds,
            @Param(5) @Nullable Long timestamp,
            @Param(6) int consentValue,
            @Param(7) @Nullable Bundle extras,
            @Param(8) int statusValue,
            @Param(9) @Nullable String tosUrl,
            @Param(10) @Nullable String language,
            @Param(11) @Nullable String country,
            @Param(12) @Nullable String tosVersion,
            @Param(13) @Nullable String tosContentTitle,
            @Param(14) @Nullable String accountName,
            @Param(15) @Nullable String consentVariant,
            @Param(16) @Nullable String consentTrigger,
            @Param(17) int rcsFlowContextValue,
            @Param(18) int deviceConsentSourceValue,
            @Param(19) int deviceConsentVersionValue
    ) {
        this.requestCode = requestCode;
        this.asterismClientValue = asterismClientValue;
        this.flowContextValue = flowContextValue;
        this.tosResourceIds = tosResourceIds;
        this.timestamp = timestamp;
        this.consentValue = consentValue;
        this.extras = extras;
        this.statusValue = statusValue;
        this.tosUrl = tosUrl;
        this.language = language;
        this.country = country;
        this.tosVersion = tosVersion;
        this.tosContentTitle = tosContentTitle;
        this.accountName = accountName;
        this.consentVariant = consentVariant;
        this.consentTrigger = consentTrigger;
        this.rcsFlowContextValue = rcsFlowContextValue;
        this.deviceConsentSourceValue = deviceConsentSourceValue;
        this.deviceConsentVersionValue = deviceConsentVersionValue;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}