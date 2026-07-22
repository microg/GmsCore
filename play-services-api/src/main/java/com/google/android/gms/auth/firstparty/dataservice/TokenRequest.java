/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.firstparty.dataservice;

import android.accounts.Account;
import android.os.Bundle;

import com.google.android.gms.auth.firstparty.shared.AppDescription;
import com.google.android.gms.auth.firstparty.shared.CaptchaSolution;
import org.microg.safeparcel.AutoSafeParcelable;

public class TokenRequest extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 8;
    @Field(2)
    private String service;
    @Field(3)
    public String accountName;
    @Field(4)
    public Bundle extras;
    @Field(5)
    public FACLConfig faclConfig;
    @Field(6)
    public PACLConfig paclConfig;
    @Field(7)
    public boolean signingIn;
    @Field(9)
    public String consent;
    @Field(10)
    public AppDescription callingAppDescription;
    @Field(11)
    public CaptchaSolution captchaSolution;
    @Field(14)
    public boolean useCache;
    @Field(15)
    public String accountType;
    @Field(16)
    public int delegationType;
    @Field(17)
    public String delegateeUserId;
    @Field(19)
    public String consentResult;
    @Field(24)
    public int mode;

    public Account getAccount() {
        return new Account(accountName, accountType);
    }

    public static final Creator<TokenRequest> CREATOR = new AutoCreator<TokenRequest>(TokenRequest.class);
}
