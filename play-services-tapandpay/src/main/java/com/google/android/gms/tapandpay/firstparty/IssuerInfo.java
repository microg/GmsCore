/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.firstparty;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class IssuerInfo extends AbstractSafeParcelable {
    @Field(2)
    public String issuerName;
    @Field(3)
    public String issuerPhoneNumber;
    @Field(4)
    public String appLogoUrl;
    @Field(5)
    public String appName;
    @Field(6)
    public String appDeveloperName;
    @Field(7)
    public String appPackageName;
    @Field(8)
    public String privacyNoticeUrl;
    @Field(9)
    public String termsAndConditionsUrl;
    @Field(10)
    public String productShortName;
    @Field(11)
    public String appAction;
    @Field(12)
    public String appIntentExtraMessage;
    @Field(13)
    public String issuerMessageHeadline;
    @Field(14)
    public String issuerMessageBody;
    @Field(15)
    public long issuerMessageExpiryTimestampMillis;
    @Field(16)
    public String issuerMessageLinkPackageName;
    @Field(17)
    public String issuerMessageLinkAction;
    @Field(18)
    public String issuerMessageLinkExtraText;
    @Field(20)
    public String issuerMessageLinkUrl;
    @Field(21)
    public String issuerMessageLinkText;
    @Field(22)
    public String issuerWebLinkUrl;
    @Field(23)
    public String issuerWebLinkText;
    @Field(24)
    public int issuerMessageType;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("IssuerInfo")
                .field("issuerName", issuerName)
                .field("issuerPhoneNumber", issuerPhoneNumber)
                .field("appLogoUrl", appLogoUrl)
                .field("appName", appName)
                .field("appDeveloperName", appDeveloperName)
                .field("appPackageName", appPackageName)
                .field("privacyNoticeUrl", privacyNoticeUrl)
                .field("termsAndConditionsUrl", termsAndConditionsUrl)
                .field("productShortName", productShortName)
                .field("appAction", appAction)
                .field("appIntentExtraMessage", appIntentExtraMessage)
                .field("issuerMessageHeadline", issuerMessageHeadline)
                .field("issuerMessageBody", issuerMessageBody)
                .field("issuerMessageExpiryTimestampMillis", issuerMessageExpiryTimestampMillis)
                .field("issuerMessageLinkPackageName", issuerMessageLinkPackageName)
                .field("issuerMessageLinkAction", issuerMessageLinkAction)
                .field("issuerMessageLinkExtraText", issuerMessageLinkExtraText)
                .field("issuerMessageLinkUrl", issuerMessageLinkUrl)
                .field("issuerMessageLinkText", issuerMessageLinkText)
                .field("issuerWebLinkUrl", issuerWebLinkUrl)
                .field("issuerWebLinkText", issuerWebLinkText)
                .field("issuerMessageType", issuerMessageType)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<IssuerInfo> CREATOR = findCreator(IssuerInfo.class);
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
