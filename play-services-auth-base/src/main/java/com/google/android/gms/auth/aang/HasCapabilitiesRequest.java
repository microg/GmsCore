/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.aang;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

@Hide
public class HasCapabilitiesRequest extends AutoSafeParcelable {
    @Field(1)
    public GoogleAccount account;
    @Field(2)
    public List<String> capabilities;

    public static final Creator<HasCapabilitiesRequest> CREATOR = new AutoCreator<>(HasCapabilitiesRequest.class);
}
