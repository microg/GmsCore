/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.rcs.RcsCallerPolicy;

import java.util.HashMap;
import java.util.Map;

/**
 * Constellation Service - Phone Number Verification for RCS.
 *
 * Called by Google Messages to verify phone numbers for RCS activation.
 *
 * Service ID: 155 (CONSTELLATION)
 * Action: com.google.android.gms.constellation.service.START
 */
public class ConstellationService extends BaseService {
    private static final String TAG = "GmsConstellationSvc";

    private final Map<String, ConstellationServiceImpl> implByPackage = new HashMap<>();

    public ConstellationService() {
        super(TAG, GmsService.CONSTELLATION);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        String callingPackage = RcsCallerPolicy.checkConstellationCaller(this, request);
        String callingVersion = RcsCallerPolicy.getPackageVersionSummary(this, callingPackage);
        Log.d(TAG, "handleServiceRequest: supportsConnectionInfo=" + request.supportsConnectionInfo);
        Log.d(TAG, "handleServiceRequest from: " + callingPackage);
        Log.i("MicroGRcs", "svc155 bind caller=" + callingPackage + " version=" + callingVersion + " supportsConnectionInfo=" + request.supportsConnectionInfo);
        
        ConstellationServiceImpl impl;
        synchronized (implByPackage) {
            impl = implByPackage.get(callingPackage);
            if (impl == null) {
                impl = new ConstellationServiceImpl(this, callingPackage);
                implByPackage.put(callingPackage, impl);
            }
        }
        
        if (request.supportsConnectionInfo) {
            // Return ConnectionInfo with supported features
            ConnectionInfo info = new ConnectionInfo();
            info.features = RcsFeatures.SUPPORTED;
            Log.d(TAG, "Returning ConnectionInfo with " + RcsFeatures.SUPPORTED.length + " features");
            callback.onPostInitCompleteWithConnectionInfo(0, impl.asBinder(), info);
        } else {
            callback.onPostInitComplete(0, impl.asBinder(), null);
        }
    }
}
