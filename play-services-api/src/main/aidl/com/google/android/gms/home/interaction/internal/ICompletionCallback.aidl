/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.home.interaction.internal;

import com.google.android.gms.common.data.DataHolder;

oneway interface ICompletionCallback {
    void onComplete(in DataHolder dataHolder) = 0;
}
