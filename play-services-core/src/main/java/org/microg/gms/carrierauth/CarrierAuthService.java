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
import org.microg.gms.rcs.ProvisioningCoordinator;
import org.microg.gms.rcs.ProvisioningState;

public final class CarrierAuthService extends BaseService {
    private CallerVerifier callerVerifier;
    private ProvisioningCoordinator provisioningCoordinator;
    private CarrierAuthServiceImpl serviceStub;

    public CarrierAuthService() {
        super("GmsCarrierAuth", GmsService.CARRIER_AUTH);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        callerVerifier = new CallerVerifier(this);
        provisioningCoordinator = new ProvisioningCoordinator(this);
        serviceStub = new CarrierAuthServiceImpl(this);
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

        if (provisioningCoordinator != null && provisioningCoordinator.getState() == ProvisioningState.COMPLETE) {
            callback.onPostInitComplete(
                    ConnectionResult.SUCCESS,
                    serviceStub,
                    null);
            return;
        }

        callback.onPostInitComplete(
                ConnectionResult.API_UNAVAILABLE,
                null,
                null);
    }

}