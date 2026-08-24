/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class ReloadAidlRequest extends AutoSafeParcelable {
    @Field(1)
    public String cachedState;
    public static final Creator<ReloadAidlRequest> CREATOR = new AutoCreator<>(ReloadAidlRequest.class);
}
