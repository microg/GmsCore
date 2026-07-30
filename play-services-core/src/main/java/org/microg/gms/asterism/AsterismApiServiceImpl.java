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

import android.content.Context;
import android.os.RemoteException;

import com.google.android.gms.asterism.GetAsterismConsentRequest;
import com.google.android.gms.asterism.GetAsterismConsentResponse;
import com.google.android.gms.asterism.SetAsterismConsentRequest;
import com.google.android.gms.asterism.SetAsterismConsentResponse;
import com.google.android.gms.asterism.internal.IAsterismApiService;
import com.google.android.gms.asterism.internal.IAsterismCallbacks;
import com.google.android.gms.common.api.Status;

import org.microg.gms.asterism.consent.AsterismConsent;
import org.microg.gms.asterism.consent.AsterismConsentRepository;

/**
 * Implementation of the Asterism AIDL contract.
 *
 * Consent records are backed by the supplied {@link AsterismConsentRepository}.
 * Server synchronization and the exact consent-state mapping are still
 * TODO_PROTOCOL_DISCOVERY and are tracked in Implementation_Delta_Report.md.
 */
public class AsterismApiServiceImpl extends IAsterismApiService.Stub {
    private final Context context;
    private final AsterismConsentRepository consentRepository;

    public AsterismApiServiceImpl(Context context, AsterismConsentRepository consentRepository) {
        this.context = context;
        this.consentRepository = consentRepository;
    }

    @Override
    public void getAsterismConsent(IAsterismCallbacks cb, GetAsterismConsentRequest request) throws RemoteException {
        AsterismConsent consent = consentRepository.get(request.requestCode);
        GetAsterismConsentResponse response;
        if (consent == null) {
            // No local consent record yet; return UNKNOWN state rather than guessing.
            response = new GetAsterismConsentResponse(
                    request.requestCode, 0, null, null, 0);
            cb.onConsentFetched(Status.INTERNAL_ERROR, response);
            return;
        }

        // Local best-effort mapping. The server-backed semantics for these fields
        // are still under protocol discovery.
        response = new GetAsterismConsentResponse(
                request.requestCode,
                consent.consentValue,
                null,
                null,
                0);
        cb.onConsentFetched(Status.SUCCESS, response);
    }

    @Override
    public void setAsterismConsent(IAsterismCallbacks cb, SetAsterismConsentRequest request) throws RemoteException {
        AsterismConsent consent = new AsterismConsent(
                request.consentValue,
                request.consentVersionValue,
                request.deviceConsentSourceValue,
                request.deviceConsentVersionValue);
        consentRepository.set(request.requestCode, consent);

        SetAsterismConsentResponse response = new SetAsterismConsentResponse(
                request.requestCode, null, null);
        cb.onConsentRegistered(Status.SUCCESS, response);
    }

    @Override
    public void getIsPnvrConstellationDevice(IAsterismCallbacks cb) throws RemoteException {
        // PNVR detection requires protocol discovery (TODO_PROTOCOL_DISCOVERY).
        cb.onIsPnvrConstellationDevice(Status.INTERNAL_ERROR, false);
    }
}
