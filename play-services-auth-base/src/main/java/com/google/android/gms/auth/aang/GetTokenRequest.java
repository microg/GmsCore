/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.aang;

import android.net.Network;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

@Hide
public class GetTokenRequest extends AutoSafeParcelable {
    @Field(1)
    public GoogleAccount account;
    @Field(2)
    public String field2;
    @Field(3)
    public List<String> oauth2Scopes;
    @Field(4)
    public List<String> webLoginUrls;
    @Field(5)
    public List<String> clientLoginScopes;
    @Field(6)
    public List<String> oauth2TokenIdScopes;
    @Field(7)
    public int delegationType;
    @Field(8)
    public String delegateeUserId;
    @Field(9)
    public boolean handleNotification;
    @Field(10)
    public byte[] field10;
    @Field(11)
    public String packageName;
    @Field(12)
    public boolean suppressProgressScreen;
    @Field(13)
    public Network network;
    @Field(14)
    public boolean useNewExceptions;
    @Field(15)
    public int clientVersion;

    public static final Creator<GetTokenRequest> CREATOR = new AutoCreator<>(GetTokenRequest.class);
}
