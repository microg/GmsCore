/*
 * Copyright (C) 2013-2026 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.constellation;

import android.os.RemoteException;

import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.constellation.iid.IidTokenProvider;
import org.microg.gms.constellation.iid.UnimplementedIidTokenProvider;
import org.microg.gms.constellation.pnv.PnvCapabilityProvider;
import org.microg.gms.constellation.pnv.UnimplementedPnvCapabilityProvider;
import org.microg.gms.constellation.verification.VerificationCoordinator;

/**
 * Entry point for the Constellation phone-number verification service.
 *
 * Binds the {@code com.google.android.gms.constellation.service.START} action
 * and returns an {@link com.google.android.gms.constellation.internal.IConstellationApiService}
 * binder to callers signed with a Google platform/app key.
 */
public class ConstellationApiService extends BaseService {
    private static final String TAG = "ConstellationApiService";

    private final VerificationCoordinator verificationCoordinator = new VerificationCoordinator();
    private final IidTokenProvider iidTokenProvider = new UnimplementedIidTokenProvider();
    private final PnvCapabilityProvider pnvCapabilityProvider = new UnimplementedPnvCapabilityProvider();

    public ConstellationApiService() {
        super(TAG, GmsService.CONSTELLATION);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        String packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName);
        if (!PackageUtils.isGooglePackage(this, packageName)) {
            throw new SecurityException(packageName + " is not a Google package");
        }

        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.features = FeatureRegistry.CONSTELLATION_FEATURES;

        callback.onPostInitCompleteWithConnectionInfo(0,
                new ConstellationApiServiceImpl(this, verificationCoordinator, iidTokenProvider, pnvCapabilityProvider).asBinder(),
                connectionInfo);
    }
}
