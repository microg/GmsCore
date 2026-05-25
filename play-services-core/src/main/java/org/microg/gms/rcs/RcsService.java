/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;

/**
 * RCS Service implementation.
 *
 * Handles the "com.google.android.gms.rcs.START" intent.
 * Returns a success code to allow Google Messages to proceed with RCS setup.
 */
public class RcsService extends BaseService {
    private static final String TAG = "GmsRcsService";

    public RcsService() {
        super(TAG, GmsService.RCS);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        String packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName);
        if (packageName == null) {
            Log.w(TAG, "Missing or invalid calling package");
            return;
        }
        String callingVersion = RcsCallerPolicy.getPackageVersionSummary(this, packageName);

        Log.d(TAG, "handleServiceRequest from: " + packageName);
        Log.i("MicroGRcs", "svc189 bind caller=" + packageName + " version=" + callingVersion + " supportsConnectionInfo=" + request.supportsConnectionInfo);
        if (request.extras != null) {
            Log.d(TAG, "Request extras: " + request.extras);
        }

        // Return SUCCESS and our binder stub
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            new RcsServiceImpl(packageName).asBinder(),
            new ConnectionInfo()
        );
    }

    private static class RcsServiceImpl extends IRcsService.Stub {
        private final String packageName;

        public RcsServiceImpl(String packageName) {
            this.packageName = packageName;
        }
    }
}
