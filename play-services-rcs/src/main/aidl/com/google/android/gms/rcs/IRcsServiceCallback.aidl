/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.rcs;

/**
 * Generic RCS Service Callback
 */
interface IRcsServiceCallback {
    void onSuccess();
    void onError(int errorCode, String errorMessage);
}
