/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.List;

public class CreateAuthUriResponse extends AutoSafeParcelable {
    @Field(2)
    public String authUri;
    @Field(3)
    public boolean isRegistered;
    @Field(4)
    public String providerId;
    @Field(5)
    public boolean isForExistingProvider;
    @Field(6)
    public StringList stringList = new StringList();
    @Field(7)
    public List<String> signInMethods = new ArrayList<>();
    public static final Creator<CreateAuthUriResponse> CREATOR = new AutoCreator<>(CreateAuthUriResponse.class);
}
