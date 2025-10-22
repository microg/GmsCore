/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.appcheck.interop;

import com.google.firebase.appcheck.AppCheckToken;

interface IAppCheckInteropService {
    void getToken(boolean forceRefresh, IAppCheckTokenCallback callback) = 0;
}