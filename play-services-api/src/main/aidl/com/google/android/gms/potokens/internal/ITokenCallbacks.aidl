/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.potokens.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.potokens.PoToken;

interface ITokenCallbacks {
    void responseToken(in Status status, in PoToken token) = 1;
}