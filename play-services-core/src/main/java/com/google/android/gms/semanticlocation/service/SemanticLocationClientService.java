/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.semanticlocation.service;

import android.os.RemoteException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Feature;
import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public class SemanticLocationClientService extends BaseService {
    public SemanticLocationClientService() {
        super("SemanticLocationClientService", GmsService.SEMANTIC_LOCATION);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.features = new Feature[]{
                new Feature("semanticlocation_events", 1L),
        };
        callback.onPostInitCompleteWithConnectionInfo(ConnectionResult.SUCCESS,
                new SemanticLocationClientImpl().asBinder(),
                connectionInfo);
    }
}
