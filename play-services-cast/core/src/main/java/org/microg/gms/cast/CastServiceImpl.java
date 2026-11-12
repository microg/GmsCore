/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.cast.RequestItem;
import com.google.android.gms.cast.internal.IBundleCallback;
import com.google.android.gms.cast.internal.ICastService;
import com.google.android.gms.common.api.internal.IStatusCallback;

import java.util.Arrays;
import java.util.List;

/**
 * Connection-less part of the Cast API. The client library mainly uses it to ask which optional behaviors (e.g.
 * handing the route chooser over to the system output switcher) are enabled.
 */
public class CastServiceImpl extends ICastService.Stub {
    private static final String TAG = "GmsCastService";

    @Override
    public void broadcastPrecacheMessageLegacy(IStatusCallback callback, String[] arg2, String precacheData) throws RemoteException {
        Log.d(TAG, "unimplemented Method: broadcastPrecacheMessageLegacy");
    }

    @Override
    public void broadcastPrecacheMessage(IStatusCallback callback, String[] arg2, String precacheData, List<RequestItem> requestItems) throws RemoteException {
        Log.d(TAG, "unimplemented Method: broadcastPrecacheMessage");
    }

    @Override
    public void getCxLessStatus(IStatusCallback callback) throws RemoteException {
        Log.d(TAG, "unimplemented Method: getCxLessStatus");
    }

    @Override
    public void getFeatureFlags(IBundleCallback callback, String[] flags) throws RemoteException {
        Log.d(TAG, "getFeatureFlags: " + Arrays.toString(flags));
        // None of the optional behaviors is supported, so explicitly report all requested flags as disabled
        Bundle bundle = new Bundle();
        if (flags != null) {
            for (String flag : flags) {
                bundle.putBoolean(flag, false);
            }
        }
        if (callback != null) callback.onBundle(bundle);
    }

    @Override
    public void getCastStatusCodeDictionary(IBundleCallback callback, String[] dictionaries) throws RemoteException {
        Log.d(TAG, "getCastStatusCodeDictionary: " + Arrays.toString(dictionaries));
        if (callback != null) callback.onBundle(new Bundle());
    }
}
