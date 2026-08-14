/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.internal.firstparty;

import android.accounts.Account;
import org.microg.safeparcel.AutoSafeParcelable;

public class GetAllCardsRequest extends AutoSafeParcelable {
    @Field(2)
    public boolean refreshSeCards;
    @Field(3)
    public Account account;
    @Field(4)
    public int sortOrderCollectionId;

    public static final Creator<GetAllCardsRequest> CREATOR = new AutoCreator<>(GetAllCardsRequest.class);
}
