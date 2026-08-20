/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.carrierauth;

import android.os.RemoteException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.rcs.CallerVerifier;

public final class CarrierAuthService extends BaseService {
    private final CallerVerifier callerVerifier;

    public CarrierAuthService() {
        super("GmsCarrierAuth", GmsService.CARRIER_AUTH);
        callerVerifier = new CallerVerifier(this);
    }

    @Override
    public void handleServiceRequest(
            IGmsCallbacks callback,
            GetServiceRequest request,
            GmsService service) throws RemoteException {
        if (request == null || !callerVerifier.isAllowedClient(request.packageName)) {
            callback.onPostInitComplete(
                    ConnectionResult.DEVELOPER_ERROR,
                    null,
                    null);
            return;
        }

        callback.onPostInitComplete(
                ConnectionResult.API_UNAVAILABLE,
                null,
                null);
    }
}