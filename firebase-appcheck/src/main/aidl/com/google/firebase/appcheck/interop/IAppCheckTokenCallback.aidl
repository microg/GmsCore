/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.appcheck.interop;

import com.google.firebase.appcheck.AppCheckToken;

interface IAppCheckTokenCallback {
    void onSuccess(in AppCheckToken token) = 0;
    void onFailure(String errorMessage) = 1;
}