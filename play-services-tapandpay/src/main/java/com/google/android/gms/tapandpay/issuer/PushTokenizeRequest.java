/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.issuer;

import android.os.IBinder;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.tapandpay.TapAndPay;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class PushTokenizeRequest extends AbstractSafeParcelable {

    @Field(2)
    public final int network;

    @Field(3)
    public final int tokenServiceProvider;

    @Field(4)
    @NonNull
    public final byte[] opaquePaymentCard;

    @Field(5)
    @NonNull
    public final String lastDigits;

    @Field(6)
    @NonNull
    public final String displayName;

    @Field(7)
    @NonNull
    public final UserAddress userAddress;

    @Field(8)
    public final boolean isTransit;

    @Field(9)
    @Nullable
    public final int[] supportedCallbackRequestTypes;

    @Field(value = 10, type = "android.os.IBinder", getter = "$object.pushTokenizeCallbacks.asBinder()")
    @Nullable
    public final IPushTokenizeRequestCallbacks pushTokenizeCallbacks;

    @Field(11)
    @Nullable
    public final CobadgedTokenInfo cobadgedTokenInfo;

    @Field(12)
    @Nullable
    public final SupervisedUserInfo supervisedUserInfo;

    @Field(13)
    @Nullable
    public final String[] supportedTokenRequestorIds;

    @Field(14)
    @Nullable
    public final PushTokenizeExtraOptions pushTokenizeExtraOptions;

    @Constructor
    PushTokenizeRequest(@Param(2) int network, @Param(3) int tokenServiceProvider, @Param(4) byte[] opaquePaymentCard, @Param(5) String lastDigits, @Param(6) String displayName, @Param(7) UserAddress userAddress, @Param(8) boolean isTransit, @Param(9) int[] supportedCallbackRequestTypes, @Param(10) IBinder pushTokenizeCallbacks, @Param(11) CobadgedTokenInfo cobadgedTokenInfo, @Param(12) SupervisedUserInfo supervisedUserInfo, @Param(13) String[] supportedTokenRequestorIds, @Param(14) PushTokenizeExtraOptions pushTokenizeExtraOptions) {
        this(network, tokenServiceProvider, opaquePaymentCard, lastDigits, displayName, userAddress, isTransit, supportedCallbackRequestTypes, IPushTokenizeRequestCallbacks.Stub.asInterface(pushTokenizeCallbacks), cobadgedTokenInfo, supervisedUserInfo, supportedTokenRequestorIds, pushTokenizeExtraOptions);
    }

    public PushTokenizeRequest(@TapAndPay.CardNetwork int network, @TapAndPay.TokenServiceProvider int tokenServiceProvider, @NonNull byte[] opaquePaymentCard, @NonNull String lastDigits, @NonNull String displayName, @NonNull UserAddress userAddress, boolean isTransit, @Nullable int[] supportedCallbackRequestTypes, @Nullable IPushTokenizeRequestCallbacks pushTokenizeCallbacks, @Nullable CobadgedTokenInfo cobadgedTokenInfo, @Nullable SupervisedUserInfo supervisedUserInfo, @Nullable String[] supportedTokenRequestorIds, @Nullable PushTokenizeExtraOptions pushTokenizeExtraOptions) {
        this.network = network;
        this.tokenServiceProvider = tokenServiceProvider;
        this.opaquePaymentCard = opaquePaymentCard;
        this.lastDigits = lastDigits;
        this.displayName = displayName;
        this.userAddress = userAddress;
        this.isTransit = isTransit;
        this.supportedCallbackRequestTypes = supportedCallbackRequestTypes;
        this.pushTokenizeCallbacks = pushTokenizeCallbacks;
        this.cobadgedTokenInfo = cobadgedTokenInfo;
        this.supervisedUserInfo = supervisedUserInfo;
        this.supportedTokenRequestorIds = supportedTokenRequestorIds;
        this.pushTokenizeExtraOptions = pushTokenizeExtraOptions;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("PushTokenizeRequest")
                .field("network", network)
                .field("tokenServiceProvider", tokenServiceProvider)
                .field("opaquePaymentCard", opaquePaymentCard)
                .field("lastDigits", lastDigits)
                .field("displayName", displayName)
                .field("userAddress", userAddress)
                .field("isTransit", isTransit)
                .field("supportedCallbackRequestTypes", supportedCallbackRequestTypes)
                .field("pushTokenizeCallbacks", pushTokenizeCallbacks)
                .field("cobadgedTokenInfo", cobadgedTokenInfo)
                .field("supervisedUserInfo", supervisedUserInfo)
                .field("supportedTokenRequestorIds", supportedTokenRequestorIds)
                .field("pushTokenizeExtraOptions", pushTokenizeExtraOptions)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<PushTokenizeRequest> CREATOR = findCreator(PushTokenizeRequest.class);
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
