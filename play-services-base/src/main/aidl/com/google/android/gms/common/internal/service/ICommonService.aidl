/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal.service;

import com.google.android.gms.common.internal.service.ICommonCallbacks;

interface ICommonService {
    void clearDefaultAccount(in ICommonCallbacks callbacks) = 0;
}