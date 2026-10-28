/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection.internal;

import android.os.ParcelFileDescriptor;

/**
 * Service-side counterpart of PayloadCallback. Delivered on a Binder thread;
 * the client forwards to the app callback directly. For FILE payloads the
 * ParcelFileDescriptor refers to a fully received file; for BYTES payloads
 * the bytes are carried inline and pfd is null.
 */
interface IPayloadCallback {
    void onPayloadReceived(String endpointId, long payloadId, int payloadType, in byte[] bytes, in ParcelFileDescriptor pfd);
    void onPayloadTransferUpdate(String endpointId, long payloadId, int status, long bytesTransferred, long totalBytes);
}
