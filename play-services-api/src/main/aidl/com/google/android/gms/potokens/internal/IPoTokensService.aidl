/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.potokens.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.potokens.internal.ITokenCallbacks;

interface IPoTokensService {
    void responseStatus(IStatusCallback call, int code) = 1;
    // The direction of 'bArr' is not specified. array can be an in, out, or inout parameter.
    void responseStatusToken(ITokenCallbacks call, int i, in byte[] bArr) = 2;
}