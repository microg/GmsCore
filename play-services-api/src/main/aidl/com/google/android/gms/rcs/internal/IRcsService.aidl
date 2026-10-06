/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.rcs.internal;

import com.google.android.gms.rcs.internal.IRcsServiceCallbacks;

/**
 * The RCS service interface exposed by GMS to Google Messages
 * (com.google.android.apps.messaging) via com.google.android.gms.rcs.START.
 *
 * This is a stub implementation. The transaction IDs are aligned with those
 * observed in Play Services. Google Messages binds this service during RCS
 * provisioning and calls these methods to determine device RCS capability
 * and to initiate phone-number registration.
 *
 * Most carriers only require that GMS holds RECEIVE_SMS / READ_SMS /
 * SEND_SMS permissions and returns SUCCESS here; the actual provisioning
 * handshake is done by Google Messages talking directly to Jibe (Google's
 * RCS backend) over HTTPS.
 * Some carriers (T-Mobile, AT&T) additionally require ICC (SIM card)
 * authentication via TelephonyManager.getIccAuthentication(), which needs
 * READ_PRIVILEGED_PHONE_STATE or carrier-granted access. That path is not
 * implemented here but is noted for future work.
 */
interface IRcsService {
    /**
     * Called by Google Messages to check whether this device / GMS installation
     * supports RCS. A SUCCESS response allows provisioning to proceed.
     *
     * @param callbacks  callback interface to deliver the result on
     * @param packageName  caller's package name (e.g. "com.google.android.apps.messaging")
     */
    void getCapabilities(IRcsServiceCallbacks callbacks, String packageName) = 1;

    /**
     * Called to retrieve device-level RCS configuration (proto-encoded).
     * Returning an empty byte array with SUCCESS status is acceptable for
     * the initial provisioning flow.
     */
    void getConfiguration(IRcsServiceCallbacks callbacks, String packageName) = 2;

    /**
     * Generic request method used by some versions of Google Messages.
     * The payload is a serialised proto request; we ignore it and return SUCCESS.
     */
    void sendRequest(IRcsServiceCallbacks callbacks, String packageName, in byte[] request) = 3;
}
