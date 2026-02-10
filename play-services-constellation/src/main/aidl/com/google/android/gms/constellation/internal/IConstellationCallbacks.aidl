/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.PhoneNumberInfo;
import com.google.android.gms.constellation.VerifyPhoneNumberResponse;
import com.google.android.gms.constellation.GetIidTokenResponse;
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse;

interface IConstellationCallbacks {
    oneway void onPhoneNumberVerified(in Status status, in List<PhoneNumberInfo> phoneNumbers, in Bundle apiMetadata) = 1;
    oneway void onPhoneNumberVerificationsCompleted(in Status status, in VerifyPhoneNumberResponse response, in Bundle apiMetadata) = 2;
    oneway void onIidTokenGenerated(in Status status, in GetIidTokenResponse response, in Bundle apiMetadata) = 3;
    oneway void onGetPnvCapabilitiesCompleted(in Status status, in GetPnvCapabilitiesResponse response, in Bundle apiMetadata) = 4;
}
