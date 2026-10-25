/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class SignInWithEmailLinkAidlRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public String email;
    @SafeParceled(2)
    public String emailLink;

    public static final Creator<SignInWithEmailLinkAidlRequest> CREATOR = new AutoCreator<>(SignInWithEmailLinkAidlRequest.class);
}
