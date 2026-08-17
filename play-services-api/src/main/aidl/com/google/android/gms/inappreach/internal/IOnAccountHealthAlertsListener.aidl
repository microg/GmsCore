/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.inappreach.internal;

interface IOnAccountHealthAlertsListener {
    oneway void onAccountHealthAlerts(in byte[] response) = 0;
}
