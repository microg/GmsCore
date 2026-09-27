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
 * Handles {@code com.google.android.gms.rcs.START} so Google Messages can bind
 * GMS service 189 instead of hitting {@link org.microg.gms.DummyService}.
 *
 * Phone-number verification and consent remain in Constellation (155) and
 * Asterism (199); this service only unblocks the RCS binder handshake.
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

        Log.d(TAG, "handleServiceRequest from: " + packageName);
        callback.onPostInitCompleteWithConnectionInfo(
                CommonStatusCodes.SUCCESS,
                new RcsServiceImpl(packageName).asBinder(),
                new ConnectionInfo()
        );
    }

    private static class RcsServiceImpl extends IRcsService.Stub {
        private final String packageName;

        RcsServiceImpl(String packageName) {
            this.packageName = packageName;
        }
    }
}
