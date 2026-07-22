/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.integrity.protocol;

import com.google.android.play.core.integrity.protocol.IExpressIntegrityServiceCallback;
import com.google.android.play.core.integrity.protocol.IRequestDialogCallback;

interface IExpressIntegrityService {
    void warmUpIntegrityToken(in Bundle bundle, in IExpressIntegrityServiceCallback callback) = 1;
    void requestExpressIntegrityToken(in Bundle bundle, in IExpressIntegrityServiceCallback callback) = 2;
    void requestAndShowDialog(in Bundle bundle, in IRequestDialogCallback callback) = 5;
}