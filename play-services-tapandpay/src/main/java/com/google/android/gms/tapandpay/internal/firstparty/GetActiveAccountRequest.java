/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.internal.firstparty;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetActiveAccountRequest extends AutoSafeParcelable {
    public static final Creator<GetActiveAccountRequest> CREATOR = new AutoCreator<>(GetActiveAccountRequest.class);
}
