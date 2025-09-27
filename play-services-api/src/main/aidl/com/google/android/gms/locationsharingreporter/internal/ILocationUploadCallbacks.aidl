/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter.internal;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.locationsharingreporter.LocationUploadResponse;

interface ILocationUploadCallbacks {
    void callback(in Status status, in LocationUploadResponse locationUploadResponse) = 0;
}