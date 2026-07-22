/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location;

import androidx.annotation.Nullable;
import com.google.android.gms.location.internal.ClientIdentity;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class LocationAvailabilityRequest extends AutoSafeParcelable {
    @Field(1)
    public boolean bypass;
    @Field(2)
    @Nullable
    public ClientIdentity impersonation;
    public static final Creator<LocationAvailabilityRequest> CREATOR = new AutoCreator<>(LocationAvailabilityRequest.class);
}
