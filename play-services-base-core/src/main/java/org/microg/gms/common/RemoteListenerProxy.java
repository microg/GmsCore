/*
 * Copyright (C) 2013-2019 microG Project Team
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

package org.microg.gms.common;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class RemoteListenerProxy<T extends IInterface> implements ServiceConnection, InvocationHandler {
    private static final String TAG = "GmsRemoteListener";
    private final Context context;
    private final Intent searchIntent;
    private final String bindAction;
    private IBinder remote;
    private boolean connecting;
    private List<Runnable> waiting = new ArrayList<Runnable>();
    private Class<T> tClass;

    public static <T extends IInterface> T get(Context context, Intent intent, Class<T> tClass, String bindAction) {
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), new Class[]{tClass},
                new RemoteListenerProxy<T>(context, intent, tClass, bindAction));
    }

    private RemoteListenerProxy(Context context, Intent intent, Class<T> tClass, String bindAction) {
        this.context = context;
        this.searchIntent = intent;
        this.tClass = tClass;
        this.bindAction = bindAction;
    }

    private boolean connect() {
        synchronized (this) {
            if (!connecting) {
                try {
                    ResolveInfo resolveInfo = context.getPackageManager().resolveService(searchIntent, 0);
                    if (resolveInfo != null) {
                        Intent intent = new Intent(bindAction);
                        intent.setPackage(resolveInfo.serviceInfo.packageName);
                        intent.setClassName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
                        connecting = context.bindService(intent, this, Context.BIND_AUTO_CREATE);
                        if (!connecting) Log.d(TAG, "Could not connect to: " + intent);
                        return connecting;
                    }
                    return false;
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            }
            return true;
        }
    }

    private void runOncePossible(Runnable runnable) {
        synchronized (this) {
            if (remote == null) {
                waiting.add(runnable);
            } else {
                runnable.run();
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        synchronized (this) {
            remote = service;
            if (!waiting.isEmpty()) {
                try {
                    for (Runnable runnable : waiting) {
                        runnable.run();
                    }
                } catch (Exception e) {
                }
                waiting.clear();
                try {
                    context.unbindService(RemoteListenerProxy.this);
                } catch (Exception e) {
                }
                connecting = false;
                remote = null;
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        synchronized (this) {
            remote = null;
        }
    }

    @Override
    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
        if (method.getDeclaringClass().equals(tClass)) {
            runOncePossible(new Runnable() {
                @Override
                public void run() {
                    try {
                        Object asInterface = Class.forName(tClass.getName() + "$Stub").getMethod("asInterface", IBinder.class).invoke(null, remote);
                        method.invoke(asInterface, args);
                    } catch (Exception e) {
                        Log.w(TAG, e);
                    }
                }
            });
            connect();
            return null;
        } else if (method.getDeclaringClass().equals(Object.class)) {
            return method.invoke(this, args);
        }
        return null;
    }
}
