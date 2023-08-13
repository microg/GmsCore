/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.firstparty;

import android.util.SparseArray;
import androidx.annotation.Nullable;
import org.microg.safeparcel.AutoSafeParcelable;

public class GetAllCardsResponse extends AutoSafeParcelable {
    @Field(2)
    public final CardInfo[] cardInfos;
    @Field(3)
    public final AccountInfo accountInfo;
    @Field(4)
    public final String defaultClientTokenId;
    @Field(5)
    public final String overrideClientTokenId;
    // FIXME: Add support for SparseArray in SafeParcelable library
//    @Field(6)
    public final SparseArray<String> seDefaultCards;
    @Field(7)
    public final byte[] wearSortOrder;

    private GetAllCardsResponse() {
        cardInfos = new CardInfo[0];
        accountInfo = null;
        defaultClientTokenId = null;
        overrideClientTokenId = null;
        seDefaultCards = new SparseArray<>();
        wearSortOrder = new byte[0];
    }

    public GetAllCardsResponse(CardInfo[] cardInfos, AccountInfo accountInfo, String defaultClientTokenId, String overrideClientTokenId, SparseArray<String> seDefaultCards, byte[] wearSortOrder) {
        this.cardInfos = cardInfos;
        this.accountInfo = accountInfo;
        this.defaultClientTokenId = defaultClientTokenId;
        this.overrideClientTokenId = overrideClientTokenId;
        this.seDefaultCards = seDefaultCards;
        this.wearSortOrder = wearSortOrder;
    }

    public static final Creator<GetAllCardsResponse> CREATOR = new AutoCreator<>(GetAllCardsResponse.class);
}
