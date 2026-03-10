/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class CreateUserWithEmailAndPasswordAidlRequest extends AutoSafeParcelable {
    @Field(1)
    public String email;
    @Field(2)
    public String password;
    @Field(3)
    public String tenantId;

    public static final Creator<CreateUserWithEmailAndPasswordAidlRequest> CREATOR = new AutoCreator<>(CreateUserWithEmailAndPasswordAidlRequest.class);
}
