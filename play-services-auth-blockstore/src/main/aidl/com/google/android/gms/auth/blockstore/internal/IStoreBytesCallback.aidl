/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.blockstore.internal;

import com.google.android.gms.common.api.Status;

interface IStoreBytesCallback {
    void onStoreBytesResult(in Status status, int result);
}