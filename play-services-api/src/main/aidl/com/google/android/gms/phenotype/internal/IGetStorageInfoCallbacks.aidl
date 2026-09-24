/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.phenotype.internal;

import com.google.android.gms.common.api.Status;

interface IGetStorageInfoCallbacks {
    oneway void onGetStorageInfoed(in Status status, in byte[] bytes) = 1;
}
