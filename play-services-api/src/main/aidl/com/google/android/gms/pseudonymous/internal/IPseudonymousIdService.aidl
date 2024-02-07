/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.pseudonymous.internal;

import com.google.android.gms.pseudonymous.internal.IPseudonymousIdCallbacks;
import com.google.android.gms.pseudonymous.PseudonymousIdToken;

interface IPseudonymousIdService {
   void onResponse(IPseudonymousIdCallbacks call) = 0;
   void onResponseToken(IPseudonymousIdCallbacks call, in PseudonymousIdToken token) = 1;
}