/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import com.google.firebase.auth.UserProfileChangeRequest;

import org.microg.safeparcel.AutoSafeParcelable;

public class UpdateProfileAidlRequest extends AutoSafeParcelable {
    @Field(1)
    public UserProfileChangeRequest request;
    @Field(2)
    public String cachedState;
    public static final Creator<UpdateProfileAidlRequest> CREATOR = new AutoCreator<>(UpdateProfileAidlRequest.class);
}
