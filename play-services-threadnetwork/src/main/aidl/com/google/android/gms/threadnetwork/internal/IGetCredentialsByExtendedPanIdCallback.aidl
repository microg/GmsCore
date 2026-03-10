/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.threadnetwork.internal;

import android.content.IntentSender;
import com.google.android.gms.common.api.Status;

interface IGetCredentialsByExtendedPanIdCallback {
    void onCredentials(in Status status, in @nullable IntentSender intentSender) = 0;
}