/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.threadnetwork.internal;

import com.google.android.gms.common.api.Status;

import com.google.android.gms.threadnetwork.ThreadNetworkCredentials;

interface IThreadNetworkServiceCallbacks {
    void onCredentials(in Status status, in List<ThreadNetworkCredentials> credentials) = 0;
}