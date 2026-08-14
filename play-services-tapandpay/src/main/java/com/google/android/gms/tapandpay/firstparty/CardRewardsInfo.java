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
public class CardRewardsInfo extends AbstractSafeParcelable {
    @Field(1)
    public long expirationTimestamp;
    @Field(2)
    public String websiteUrl;
    @Field(3)
    public String websiteRedirectText;
    @Field(4)
    public String legalDisclaimer;
    @Field(5)
    public String summary;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("CardRewardsInfo")
                .field("expirationTimestamp", expirationTimestamp)
                .field("websiteUrl", websiteUrl)
                .field("websiteRedirectText", websiteRedirectText)
                .field("legalDisclaimer", legalDisclaimer)
                .field("summary", summary)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<CardRewardsInfo> CREATOR = findCreator(CardRewardsInfo.class);
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
