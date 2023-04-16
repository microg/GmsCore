/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.droidguard;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.framework.tracing.wrapper.TracingIntentService;

import org.microg.gms.droidguard.core.DroidGuardServiceBroker;
import org.microg.gms.droidguard.GuardCallback;
import org.microg.gms.droidguard.core.HandleProxyFactory;
import org.microg.gms.droidguard.PingData;
import org.microg.gms.droidguard.Request;

import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DroidGuardChimeraService extends TracingIntentService {
    public static final Object a = new Object();
    // factory
    public HandleProxyFactory b;
    // widevine
    public Object c;
    // executor
    public Executor d;
    // log
    public Object e;

    private static final Object f = new Object();

    // ping
    private Object g;
    // handler
    private Handler h;


    public DroidGuardChimeraService() {
        super("DG");
        setIntentRedelivery(true);
    }

    public DroidGuardChimeraService(HandleProxyFactory factory, Object ping, Object database) {
        super("DG");
        setIntentRedelivery(true);
        this.b = factory;
        this.g = ping;
        this.h = new Handler();
    }

    // fsc
    private final void c(byte[] data) {
        PingData ping = null;
        if (data != null) {
            Log.d("GmsGuardChimera", "c(" + Base64.encodeToString(data, Base64.NO_WRAP) + ")", new RuntimeException().fillInStackTrace());
            try {
                ping = PingData.ADAPTER.decode(data);
            } catch (Exception e) {
                Log.w("GmsGuardChimera", e);
            }
        } else {
            Log.d("GmsGuardChimera", "c(null)", new RuntimeException().fillInStackTrace());
        }
        byte[] bytes = b.createPingHandle(getPackageName(), "full", b(""), ping).run(Collections.emptyMap());
        Log.d("GmsGuardChimera", "c.bytes = " + Base64.encodeToString(bytes, Base64.NO_WRAP));
        Request fastRequest = b.createRequest("fast", getPackageName(), null, bytes);
        b.fetchFromServer("fast", fastRequest);
    }

    // handle intent
    public final void a(@Nullable Intent intent) {
        Log.d("GmsGuardChimera", "a(" + intent + ")");
        if (intent != null && intent.getAction() != null && intent.getAction().equals("com.google.android.gms.droidguard.service.PING")) {
            byte[] byteData = intent.getByteArrayExtra("data");
            if (byteData == null) {
                int[] intData = intent.getIntArrayExtra("data");
                if (intData == null) {
                    c(null);
                    return;
                }
                byteData = new byte[intData.length];
                for (int i = 0; i < intData.length; i++) {
                    byteData[i] = (byte) intData[i];
                }
            }
            c(byteData);
        }
    }

    // getCallback
    public final GuardCallback b(String packageName) {
        Log.d("GmsGuardChimera", "b[getCallback](" + packageName + ")");
        return new GuardCallback(this, packageName);
    }

    @Nullable
    @Override
    public final IBinder onBind(Intent intent) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals("com.google.android.gms.droidguard.service.START")) {
            return new DroidGuardServiceBroker(this);
        }
        return null;
    }

    @Override
    public void onCreate() {
        this.e = new Object();
        this.b = new HandleProxyFactory(this);
        this.g = new Object();
        this.h = new Handler();
        this.c = new Object();
        this.d = new ThreadPoolExecutor(1, 1, 0, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<>(1), new ThreadPoolExecutor.DiscardPolicy());
        super.onCreate();
    }
}
