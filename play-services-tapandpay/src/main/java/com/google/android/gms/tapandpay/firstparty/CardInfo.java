/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.firstparty;

import android.net.Uri;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class CardInfo extends AbstractSafeParcelable {
    @Field(2)
    public String billingCardId;
    @Field(3)
    public byte[] serverToken;
    @Field(4)
    public String cardholderName;
    @Field(5)
    public String displayName;
    @Field(6)
    public int cardNetwork;
    @Field(7)
    public TokenStatus tokenStatus;
    @Field(8)
    public String panLastDigits;
    @Field(9)
    @Nullable
    public Uri cardImageUrl;
    @Field(10)
    public int cardColor;
    @Field(11)
    public int overlayTextColor;
    @Field(12)
    public IssuerInfo issuerInfo;
    @Field(13)
    public String tokenLastDigits;
    @Field(15)
    public TransactionInfo transactionInfo;
    @Field(16)
    public String issuerTokenId;
    @Field(17)
    public byte[] inAppCardToken;
    @Field(18)
    public int cachedEligibility;
    @Field(20)
    public int paymentProtocol;
    @Field(21)
    public int tokenType;
    @Field(22)
    public InStoreCvmConfig inStoreCvmConfig;
    @Field(23)
    public InAppCvmConfig inAppCvmConfig;
    @Field(24)
    public String tokenDisplayName;
    @Field(25)
    public OnlineAccountCardLinkInfo[] onlineAccountCardLinkInfos;
    @Field(26)
    public boolean allowAidSelection;
    @Field(27)
    public List<BadgeInfo> badges;
    @Field(28)
    public boolean upgradeAvailable;
    @Field(29)
    public boolean requiresSignature;
    @Field(30)
    public long googleTokenId;
    @Field(31)
    public long lastTapTimestamp;
    @Field(32)
    public boolean isTransit;
    @Field(33)
    public long googleWalletId;
    @Field(34)
    public String devicePaymentMethodId;
    @Field(35)
    public String cloudPaymentMethodId;
    @Field(36)
    @Nullable
    public CardRewardsInfo cardRewardsInfo;
    @Field(37)
    public int tapStrategy;
    @Field(38)
    public boolean hideFromGlobalActions;
    @Field(39)
    @Nullable
    public String rawPanLastDigits;
    @Field(40)
    public int cardDisplayType;
    @Field(41)
    public boolean b41;
    @Field(42)
    public long auxiliaryGoogleTokenId;
    @Field(43)
    public String auxiliaryIssuerTokenId;
    @Field(44)
    public int auxiliaryNetwork;
    @Field(45)
    public String auxClientTokenId;

    public static final SafeParcelableCreatorAndWriter<CardInfo> CREATOR = findCreator(CardInfo.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
