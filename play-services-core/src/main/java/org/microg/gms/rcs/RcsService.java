/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import android.os.Binder;
import android.os.RemoteException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public final class RcsService extends BaseService {
    private CallerVerifier callerVerifier;
    private ProvisioningCoordinator provisioningCoordinator;
    private RcsServiceStub serviceStub;

    public RcsService() {
        super("GmsRcs", GmsService.RCS);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        callerVerifier = new CallerVerifier(this);
        provisioningCoordinator = new ProvisioningCoordinator(this);
        serviceStub = new RcsServiceStub();
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

    public static final class RcsServiceStub extends Binder {
        private RcsServiceStub() {
        }

        @Override
        public String toString() {
            return "RcsServiceStub{" + "className='org.microg.gms.rcs.RcsService$RcsServiceStub'" + '}';
        }
    }
}