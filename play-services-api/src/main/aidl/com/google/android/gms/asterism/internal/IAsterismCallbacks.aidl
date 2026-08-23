/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.asterism.AsterismConsent;

interface IAsterismCallbacks {
    void onConsent(in Status status, in AsterismConsent consent);
    void onConsentSet(in Status status);
}
