/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

interface IAppEventListener {
    void onAppEvent(String name, String info) = 0;
}
