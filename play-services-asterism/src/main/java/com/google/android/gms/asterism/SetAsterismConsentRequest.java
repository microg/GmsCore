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
    public final String clientVersion;

    @Field(10)
    @Nullable
    public final String language;

    @Field(11)
    @Nullable
    public final String field11;

    @Field(12)
    @Nullable
    public final String field12;

    @Field(13)
    @Nullable
    public final String field13;

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
    public final int consentVersionValue;

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
            @Param(9) @Nullable String clientVersion,
            @Param(10) @Nullable String locale,
            @Param(11) @Nullable String field11,
            @Param(12) @Nullable String field12,
            @Param(13) @Nullable String field13,
            @Param(14) @Nullable String accountName,
            @Param(15) @Nullable String consentVariant,
            @Param(16) @Nullable String consentTrigger,
            @Param(17) int consentVersionValue,
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
        this.clientVersion = clientVersion;
        this.language = locale;
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
