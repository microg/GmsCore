/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation.internal;

import com.google.android.gms.constellation.internal.IConstellationCallbacks;

interface IConstellationService {
    void getPhoneNumber(IConstellationCallbacks callbacks) = 0;
    void verifyPhoneNumber(IConstellationCallbacks callbacks, String phoneNumber) = 1;
}
