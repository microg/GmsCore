/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.integrity.protocol;

import com.google.android.play.core.integrity.protocol.IIntegrityServiceCallback;
import com.google.android.play.core.integrity.protocol.IRequestDialogCallback;

interface IIntegrityService {
    void requestDialog(in Bundle bundle, in IRequestDialogCallback callback) = 0;
    void requestIntegrityToken(in Bundle request, in IIntegrityServiceCallback callback) = 1;
}