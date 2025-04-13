/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.threadnetwork.internal;

import com.google.android.gms.common.api.Status;

interface IIsPreferredCredentialsCallback {
    void onIsPreferredCredentials(in Status status, boolean isPreferred) = 0;
}