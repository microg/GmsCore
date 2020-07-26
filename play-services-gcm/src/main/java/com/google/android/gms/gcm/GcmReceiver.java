/*
 * Copyright (C) 2013-2017 microG Project Team
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

package com.google.android.gms.gcm;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import androidx.legacy.content.WakefulBroadcastReceiver;

import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_REGISTRATION;
import static org.microg.gms.gcm.GcmConstants.ACTION_INSTANCE_ID;
import static org.microg.gms.gcm.GcmConstants.EXTRA_FROM;
import static org.microg.gms.gcm.GcmConstants.EXTRA_RAWDATA;
import static org.microg.gms.gcm.GcmConstants.EXTRA_RAWDATA_BASE64;
import static org.microg.gms.gcm.GcmConstants.GCMID_INSTANCE_ID;
import static org.microg.gms.gcm.GcmConstants.GCMID_REFRESH;

/**
 * <code>WakefulBroadcastReceiver</code> that receives GCM messages and delivers them to an
 * application-specific {@link com.google.android.gms.gcm.GcmListenerService} subclass.
 * <p/>
 * This receiver should be declared in your application's manifest file as follows:
 * <p/>
 * <pre>
 * <receiver
 *     android:name="com.google.android.gms.gcm.GcmReceiver"
 *     android:exported="true"
 *     android:permission="com.google.android.c2dm.permission.SEND" >
 *     <intent-filter>
 *         <action android:name="com.google.android.c2dm.intent.RECEIVE" />
 *         <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
 *         <category android:name="YOUR_PACKAGE_NAME" />
 *     </intent-filter>
 * </receiver></pre>
 * The <code>com.google.android.c2dm.permission.SEND</code> permission is held by Google Play
 * services. This prevents other apps from invoking the broadcast receiver.
 */
public class GcmReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "GcmReceiver";

    public void onReceive(Context context, Intent intent) {
        sanitizeIntent(context, intent);
        enforceIntentClassName(context, intent);
        sendIntent(context, intent);
        if (getResultCode() == 0) setResultCodeIfOrdered(-1);
    }

    private void sanitizeIntent(Context context, Intent intent) {
        intent.setComponent(null);
        intent.setPackage(context.getPackageName());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent.removeCategory(context.getPackageName());
        }
        String from = intent.getStringExtra(EXTRA_FROM);
        if (ACTION_C2DM_REGISTRATION.equals(intent.getAction()) || GCMID_INSTANCE_ID.equals(from) || GCMID_REFRESH.equals(from)) {
            intent.setAction(ACTION_INSTANCE_ID);
        }
        String base64encoded = intent.getStringExtra(EXTRA_RAWDATA_BASE64);
        if (base64encoded != null) {
            intent.putExtra(EXTRA_RAWDATA, Base64.decode(base64encoded, Base64.DEFAULT));
            intent.removeExtra(EXTRA_RAWDATA_BASE64);
        }
    }

    private void enforceIntentClassName(Context context, Intent intent) {
        ResolveInfo resolveInfo = context.getPackageManager().resolveService(intent, 0);
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            Log.e(TAG, "Failed to resolve target intent service, skipping classname enforcement");
            return;
        }
        ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        if (!context.getPackageName().equals(serviceInfo.packageName) || serviceInfo.name == null) {
            Log.e(TAG, "Error resolving target intent service, skipping classname enforcement. Resolved service was: " + serviceInfo.packageName + "/" + serviceInfo.name);
            return;
        }
        intent.setClassName(context, serviceInfo.name.startsWith(".") ? (context.getPackageName() + serviceInfo.name) : serviceInfo.name);
    }

    private void sendIntent(Context context, Intent intent) {
        setResultCodeIfOrdered(500);
        try {
            ComponentName startedComponent;
            if (context.checkCallingOrSelfPermission(Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED) {
                startedComponent = startWakefulService(context, intent);
            } else {
                Log.d(TAG, "Missing wake lock permission, service start may be delayed");
                startedComponent = context.startService(intent);
            }
            if (startedComponent == null) {
                Log.e(TAG, "Error while delivering the message: ServiceIntent not found.");
                setResultCodeIfOrdered(404);
            } else {
                setResultCodeIfOrdered(-1);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Error while delivering the message to the serviceIntent", e);
            setResultCodeIfOrdered(401);
        }
    }

    private void setResultCodeIfOrdered(int code) {
        if (isOrderedBroadcast()) {
            setResultCode(code);
        }
    }
}
