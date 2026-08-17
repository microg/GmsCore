/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.gass.internal;

import android.os.Bundle;
import android.os.IInterface;
import com.google.android.gms.gass.internal.GassRequestParcel;
import com.google.android.gms.gass.internal.GassResponseParcel;

interface IGassService {
    GassResponseParcel getGassResponse(in GassRequestParcel gassRequestParcel) = 0;
    Bundle getGassBundle(in Bundle bundle, int code) = 3;
}
