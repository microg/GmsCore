/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.signin.internal;

import android.accounts.Account;
import com.google.android.gms.common.api.Scope;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class RecordConsentRequest extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;
    @Field(2)
    public Account account;
    @Field(3)
    public Scope[] scopesToConsent;
    @Field(4)
    public String serverClientId;

    public static final Creator<RecordConsentRequest> CREATOR = findCreator(RecordConsentRequest.class);
}
