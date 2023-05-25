/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.internal.firstparty;

import org.microg.safeparcel.AutoSafeParcelable;

public class SetActiveAccountRequest extends AutoSafeParcelable {
    @Field(2)
    public String accountName;
    @Field(3)
    public boolean allowSetupErrorMessage;

    public static final Creator<SetActiveAccountRequest> CREATOR = new AutoCreator<>(SetActiveAccountRequest.class);
}
