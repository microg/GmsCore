/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads.internal.signals;

interface ISignalCallback {
    void onFailure(String error) = 1;
    void onSuccess(String signals, String data, in Bundle extras) = 2;
}
