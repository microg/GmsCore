/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.panorama;

import android.os.RemoteException;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public class PanoramaService extends BaseService{

    public PanoramaService() {
        super("GmsPanoramaService", GmsService.PANORAMA);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, new PanoramaServiceImpl(), null);
    }
}
