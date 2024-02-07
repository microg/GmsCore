/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.panorama;

import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.panorama.internal.IPanoramaCallbacks;
import com.google.android.gms.panorama.internal.IPanoramaService;

public class PanoramaServiceImpl extends IPanoramaService.Stub{

    public PanoramaServiceImpl() {
    }

    @Override
    public void loadPanoramaInfoAndGrantAccess(IPanoramaCallbacks callback, Uri uri, Bundle bundle, boolean needGrantReadUriPermissions) throws RemoteException {
        Log.d("GmsPanoramaService", "ERROR:PanoramaService not implement! Print by GMS..." + uri + " bundle:" + bundle);
    }
}
