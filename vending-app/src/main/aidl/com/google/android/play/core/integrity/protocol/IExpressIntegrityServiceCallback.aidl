/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.integrity.protocol;

interface IExpressIntegrityServiceCallback {
    void OnWarmUpIntegrityTokenCallback(in Bundle bundle) = 1;
    void onRequestExpressIntegrityToken(in Bundle bundle) = 2;
    void onRequestIntegrityToken(in Bundle bundle) = 3;
}
