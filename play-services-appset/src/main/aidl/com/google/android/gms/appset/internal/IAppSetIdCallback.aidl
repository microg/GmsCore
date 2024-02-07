/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.appset.internal;

import com.google.android.gms.appset.AppSetInfoParcel;
import com.google.android.gms.common.api.Status;

interface IAppSetIdCallback {
    void onResult(in Status status, in AppSetInfoParcel appSetInfoParcel) = 0;
}