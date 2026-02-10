/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation.internal;

import android.os.Bundle;
import com.google.android.gms.constellation.VerifyPhoneNumberRequest;
import com.google.android.gms.constellation.GetIidTokenRequest;
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest;
import com.google.android.gms.constellation.internal.IConstellationCallbacks;

interface IConstellationApiService {
    // Transaction IDs aligned with recent Google Messages Constellation clients.
    void verifyPhoneNumber(IConstellationCallbacks callbacks, in VerifyPhoneNumberRequest request, in Bundle apiMetadata) = 2;
    void getIidToken(IConstellationCallbacks callbacks, in GetIidTokenRequest request, in Bundle apiMetadata) = 3;
    void getPnvCapabilities(IConstellationCallbacks callbacks, in GetPnvCapabilitiesRequest request, in Bundle apiMetadata) = 4;
}
