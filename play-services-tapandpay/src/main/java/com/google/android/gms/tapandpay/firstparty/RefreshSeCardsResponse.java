/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.firstparty;

import org.microg.safeparcel.AutoSafeParcelable;

public class RefreshSeCardsResponse extends AutoSafeParcelable {
    public static final Creator<RefreshSeCardsResponse> CREATOR = new AutoCreator<>(RefreshSeCardsResponse.class);
}
