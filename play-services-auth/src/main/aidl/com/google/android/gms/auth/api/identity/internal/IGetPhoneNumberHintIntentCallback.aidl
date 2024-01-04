/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity.internal;

import com.google.android.gms.common.api.Status;

interface IGetPhoneNumberHintIntentCallback {
    void onResult(in Status status, in PendingIntent pendingIntent);
}