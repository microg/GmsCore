/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism;

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.constellation.RcsFeatures;

/**
 * Asterism Service - Consent Management for RCS.
 *
 * Handles Google Terms of Service consent. Messages calls setConsent() to record
 * ToS agreement before provisioning.
 *
 * Service ID: 199 (ASTERISM)
 * Action: com.google.android.gms.asterism.service.START
 */
public class AsterismService extends BaseService {
    private static final String TAG = "GmsAsterismSvc";

    private AsterismServiceImpl impl;

    public AsterismService() {
        super(TAG, GmsService.ASTERISM);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        Log.d(TAG, "handleServiceRequest: supportsConnectionInfo=" + request.supportsConnectionInfo);

        if (impl == null) {
            impl = new AsterismServiceImpl(this);
        }

        if (request.supportsConnectionInfo) {
            ConnectionInfo info = new ConnectionInfo();
            info.features = RcsFeatures.SUPPORTED;
            Log.d(TAG, "Returning ConnectionInfo with " + RcsFeatures.SUPPORTED.length + " features");
            callback.onPostInitCompleteWithConnectionInfo(0, impl.asBinder(), info);
        } else {
            callback.onPostInitComplete(0, impl.asBinder(), null);
        }
    }
}
