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

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.ApiMetadata;
import com.google.android.gms.constellation.GetIidTokenRequest;
import com.google.android.gms.constellation.GetIidTokenResponse;
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest;
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse;
import com.google.android.gms.constellation.VerifyPhoneNumberRequest;
import com.google.android.gms.constellation.internal.IConstellationApiService;
import com.google.android.gms.constellation.internal.IConstellationCallbacks;

import org.microg.gms.constellation.iid.IidTokenProvider;
import org.microg.gms.constellation.pnv.PnvCapabilityProvider;
import org.microg.gms.constellation.verification.VerificationCoordinator;

/**
 * Implementation of the Constellation AIDL contract.
 *
 * Phone-number verification, IID token generation, and PNV capability mapping
 * are delegated to pluggable providers. The concrete providers are stubs while
 * the server-side protocol details remain TODO_PROTOCOL_DISCOVERY.
 */
public class ConstellationApiServiceImpl extends IConstellationApiService.Stub {
    private final Context context;
    private final VerificationCoordinator verificationCoordinator;
    private final IidTokenProvider iidTokenProvider;
    private final PnvCapabilityProvider pnvCapabilityProvider;

    public ConstellationApiServiceImpl(Context context,
                                       VerificationCoordinator verificationCoordinator,
                                       IidTokenProvider iidTokenProvider,
                                       PnvCapabilityProvider pnvCapabilityProvider) {
        this.context = context;
        this.verificationCoordinator = verificationCoordinator;
        this.iidTokenProvider = iidTokenProvider;
        this.pnvCapabilityProvider = pnvCapabilityProvider;
    }

    @Override
    public void verifyPhoneNumberV1(IConstellationCallbacks cb, Bundle params, ApiMetadata apiMetadata) throws RemoteException {
        verificationCoordinator.verifyPhoneNumberV1(cb, params, apiMetadata);
    }

    @Override
    public void verifyPhoneNumberSingleUse(IConstellationCallbacks cb, Bundle params, ApiMetadata apiMetadata) throws RemoteException {
        verificationCoordinator.verifyPhoneNumberSingleUse(cb, params, apiMetadata);
    }

    @Override
    public void verifyPhoneNumber(IConstellationCallbacks cb, VerifyPhoneNumberRequest request, ApiMetadata apiMetadata) throws RemoteException {
        verificationCoordinator.verifyPhoneNumber(cb, request, apiMetadata);
    }

    @Override
    public void getIidToken(IConstellationCallbacks cb, GetIidTokenRequest request, ApiMetadata apiMetadata) throws RemoteException {
        GetIidTokenResponse response = iidTokenProvider.getIidToken(request.projectNumber);
        cb.onIidTokenGenerated(Status.INTERNAL_ERROR, response, apiMetadata);
    }

    @Override
    public void getPnvCapabilities(IConstellationCallbacks cb, GetPnvCapabilitiesRequest request, ApiMetadata apiMetadata) throws RemoteException {
        GetPnvCapabilitiesResponse response = pnvCapabilityProvider.getCapabilities(request);
        cb.onGetPnvCapabilitiesCompleted(Status.INTERNAL_ERROR, response, apiMetadata);
    }
}
