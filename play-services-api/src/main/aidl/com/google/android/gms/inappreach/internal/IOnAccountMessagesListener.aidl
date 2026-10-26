/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.inappreach.internal;

interface IOnAccountMessagesListener {
    oneway void onAccountMessages(in byte[] response) = 0;
}
