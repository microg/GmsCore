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

package org.microg.gms.asterism;

import android.os.RemoteException;

import com.google.android.gms.common.Feature;
import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.asterism.consent.AsterismConsentRepository;
import org.microg.gms.asterism.consent.InMemoryAsterismConsentRepository;

/**
 * Entry point for the Asterism RCS consent service.
 *
 * Binds the {@code com.google.android.gms.asterism.service.START} action and
 * returns an {@link com.google.android.gms.asterism.internal.IAsterismApiService}
 * binder to callers that are signed with a Google platform/app key.
 */
public class AsterismApiService extends BaseService {
    private static final String TAG = "AsterismApiService";

    private final AsterismConsentRepository consentRepository = new InMemoryAsterismConsentRepository();

    public AsterismApiService() {
        super(TAG, GmsService.ASTERISM);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        String packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName);
        if (!PackageUtils.isGooglePackage(this, packageName)) {
            throw new SecurityException(packageName + " is not a Google package");
        }

        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.features = FeatureRegistry.ASTERISM_FEATURES;

        callback.onPostInitCompleteWithConnectionInfo(0,
                new AsterismApiServiceImpl(this, consentRepository).asBinder(),
                connectionInfo);
    }
}
