/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class DeleteAidlRequest extends AutoSafeParcelable {
    public static final Creator<DeleteAidlRequest> CREATOR = new AutoCreator<>(DeleteAidlRequest.class);
}
