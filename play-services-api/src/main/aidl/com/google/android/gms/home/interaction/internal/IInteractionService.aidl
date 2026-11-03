/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.home.interaction.internal;

import com.google.android.gms.home.interaction.OnRequestParams;

interface IInteractionService {
    void onRequest(in OnRequestParams params) = 17;
}
