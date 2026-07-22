/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.audit.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.audit.LogAuditRecordsRequest;

interface IAuditService {
    void logAuditRecords(in LogAuditRecordsRequest request, IStatusCallback callback);
}