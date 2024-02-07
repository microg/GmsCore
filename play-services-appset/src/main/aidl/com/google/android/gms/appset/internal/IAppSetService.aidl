/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.appset.internal;

import com.google.android.gms.appset.AppSetIdRequestParams;
import com.google.android.gms.appset.internal.IAppSetIdCallback;

interface IAppSetService {
    void doRequest(in AppSetIdRequestParams appSetIdRequestParams, in IAppSetIdCallback callback) = 0;
}