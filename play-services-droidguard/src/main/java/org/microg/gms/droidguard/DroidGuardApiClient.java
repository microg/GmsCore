/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.droidguard.DroidGuardHandle;
import com.google.android.gms.droidguard.internal.DroidGuardInitReply;
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;
import com.google.android.gms.droidguard.internal.IDroidGuardHandle;
import com.google.android.gms.droidguard.internal.IDroidGuardService;

import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.OnConnectionFailedListener;

public class DroidGuardApiClient extends GmsClient<IDroidGuardService> {
    private static final String TAG = "DroidGuardApiClient";
    private final Context context;
    private int openHandles = 0;
    private Handler handler;

    public DroidGuardApiClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.DROIDGUARD.ACTION);
        this.context = context;
        serviceId = GmsService.DROIDGUARD.SERVICE_ID;

        HandlerThread thread = new HandlerThread("DG");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    public DroidGuardHandle openHandle(String flow, DroidGuardResultsRequest request) {
        try {
            IDroidGuardHandle handle = getServiceInterface().getHandle();
            request.setOpenHandles(openHandles);
            DroidGuardInitReply reply = handle.initWithRequest(flow, request);
            if (reply == null) {
                handle.init(flow);
            }
            if (reply != null) {
                if (reply.pfd != null && reply.object != null) {
                    Log.w(TAG, "DroidGuardInitReply suggests additional actions in main thread");
                    Bundle bundle = (Bundle) reply.object;
                    if (bundle != null) {
                        for (String key : bundle.keySet()) {
                            Log.d(TAG, "reply.object[" + key + "] = " + bundle.get(key));
                        }
                    }
                }
            }
            openHandles++;
            return new DroidGuardHandleImpl(this, request, handle);
        } catch (Exception e) {
            return new DroidGuardHandleImpl(this, request, "Initialization failed: " + e);
        }
    }

    public void runOnHandler(Runnable runnable) {
        if (Looper.myLooper() == handler.getLooper()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    @Override
    protected IDroidGuardService interfaceFromBinder(IBinder binder) {
        return IDroidGuardService.Stub.asInterface(binder);
    }
}
