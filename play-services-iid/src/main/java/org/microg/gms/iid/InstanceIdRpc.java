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

package org.microg.gms.iid;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.iid.MessengerCompat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.google.android.gms.iid.InstanceID.ERROR_BACKOFF;
import static com.google.android.gms.iid.InstanceID.ERROR_MISSING_INSTANCEID_SERVICE;
import static com.google.android.gms.iid.InstanceID.ERROR_SERVICE_NOT_AVAILABLE;
import static com.google.android.gms.iid.InstanceID.ERROR_TIMEOUT;
import static org.microg.gms.common.Constants.GMS_PACKAGE_NAME;
import static org.microg.gms.common.Constants.GSF_PACKAGE_NAME;
import static org.microg.gms.common.Constants.GMS_VERSION_CODE;
import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_REGISTER;
import static org.microg.gms.gcm.GcmConstants.ACTION_C2DM_REGISTRATION;
import static org.microg.gms.gcm.GcmConstants.ACTION_INSTANCE_ID;
import static org.microg.gms.gcm.GcmConstants.EXTRA_APP;
import static org.microg.gms.gcm.GcmConstants.EXTRA_APP_ID;
import static org.microg.gms.gcm.GcmConstants.EXTRA_APP_VERSION_CODE;
import static org.microg.gms.gcm.GcmConstants.EXTRA_APP_VERSION_NAME;
import static org.microg.gms.gcm.GcmConstants.EXTRA_CLIENT_VERSION;
import static org.microg.gms.gcm.GcmConstants.EXTRA_ERROR;
import static org.microg.gms.gcm.GcmConstants.EXTRA_GMS_VERSION;
import static org.microg.gms.gcm.GcmConstants.EXTRA_GSF_INTENT;
import static org.microg.gms.gcm.GcmConstants.EXTRA_IS_MESSENGER2;
import static org.microg.gms.gcm.GcmConstants.EXTRA_KID;
import static org.microg.gms.gcm.GcmConstants.EXTRA_MESSENGER;
import static org.microg.gms.gcm.GcmConstants.EXTRA_OS_VERSION;
import static org.microg.gms.gcm.GcmConstants.EXTRA_PUBLIC_KEY;
import static org.microg.gms.gcm.GcmConstants.EXTRA_REGISTRATION_ID;
import static org.microg.gms.gcm.GcmConstants.EXTRA_SIGNATURE;
import static org.microg.gms.gcm.GcmConstants.EXTRA_UNREGISTERED;
import static org.microg.gms.gcm.GcmConstants.EXTRA_USE_GSF;
import static org.microg.gms.gcm.GcmConstants.PERMISSION_RECEIVE;

public class InstanceIdRpc {
    private static final String TAG = "InstanceID/Rpc";

    private static final int BLOCKING_WAIT_TIME = 30000;

    private static String iidPackageName;
    private static int lastRequestId;
    private static int retryCount;
    private static Map<String, Object> blockingResponses = new HashMap<String, Object>();

    private long nextAttempt;
    private int interval;
    private Context context;
    private PendingIntent selfAuthToken;
    private Messenger messenger;
    private Messenger myMessenger;
    private MessengerCompat messengerCompat;

    public InstanceIdRpc(Context context) {
        this.context = context;
    }

    public static String getIidPackageName(Context context) {
        if (iidPackageName != null) {
            return iidPackageName;
        }
        PackageManager packageManager = context.getPackageManager();
        for (ResolveInfo resolveInfo : packageManager.queryIntentServices(new Intent(ACTION_C2DM_REGISTER), 0)) {
            if (packageManager.checkPermission(PERMISSION_RECEIVE, resolveInfo.serviceInfo.packageName) == PERMISSION_GRANTED) {
                return iidPackageName = resolveInfo.serviceInfo.packageName;
            }
        }
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(GMS_PACKAGE_NAME, 0);
            return iidPackageName = appInfo.packageName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(GSF_PACKAGE_NAME, 0);
            return iidPackageName = appInfo.packageName;
        } catch (PackageManager.NameNotFoundException ex3) {
            Log.w(TAG, "Both Google Play Services and legacy GSF package are missing");
            return null;
        }
    }

    private static int getGmsVersionCode(final Context context) {
        final PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getPackageInfo(getIidPackageName(context), 0).versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            return -1;
        }
    }

    private static int getSelfVersionCode(final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException neverHappens) {
            return 0;
        }
    }

    private static String getSelfVersionName(final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException neverHappens) {
            return null;
        }
    }

    void initialize() {
        if (myMessenger != null) return;
        getIidPackageName(context);
        myMessenger = new Messenger(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg == null) {
                    return;
                }
                if (msg.obj instanceof Intent) {
                    Intent intent = (Intent) msg.obj;
                    intent.setExtrasClassLoader(MessengerCompat.class.getClassLoader());
                    if (intent.hasExtra(EXTRA_MESSENGER)) {
                        Parcelable messengerCandidate = intent.getParcelableExtra(EXTRA_MESSENGER);
                        if (messengerCandidate instanceof MessengerCompat) {
                            messengerCompat = (MessengerCompat) messengerCandidate;
                        } else if (messengerCandidate instanceof Messenger) {
                            messenger = (Messenger) messengerCandidate;
                        }
                    }
                    handleResponseInternal(intent);
                } else {
                    Log.w(TAG, "Dropping invalid message");
                }
            }
        });
    }

    public void handleResponseInternal(Intent resultIntent) {
        if (resultIntent == null) return;
        if (!ACTION_C2DM_REGISTRATION.equals(resultIntent.getAction()) && !ACTION_INSTANCE_ID.equals(resultIntent.getAction()))
            return;
        String result = resultIntent.getStringExtra(EXTRA_REGISTRATION_ID);
        if (result == null) result = resultIntent.getStringExtra(EXTRA_UNREGISTERED);
        if (result == null) {
            handleError(resultIntent);
            return;
        }
        retryCount = 0;
        nextAttempt = 0;
        interval = 0;

        String requestId = null;
        if (result.startsWith("|")) {
            // parse structured response
            String[] split = result.split("\\|");
            if (!"ID".equals(split[1])) {
                Log.w(TAG, "Unexpected structured response " + result);
            }
            requestId = split[2];
            if (split.length > 4) {
                if ("SYNC".equals(split[3])) {
                    // TODO: sync
                } else if("RST".equals(split[3])) {
                    // TODO: rst
                    resultIntent.removeExtra(EXTRA_REGISTRATION_ID);
                    return;
                }
            }
            result = split[split.length-1];
            if (result.startsWith(":"))
                result = result.substring(1);
            resultIntent.putExtra(EXTRA_REGISTRATION_ID, result);
        }
        setResponse(requestId, resultIntent);
    }

    private void handleError(Intent resultIntent) {
        String error = resultIntent.getStringExtra("error");
        if (error == null) return;
        String requestId = null;
        if (error.startsWith("|")) {
            // parse structured error message
            String[] split = error.split("\\|");
            if (!"ID".equals(split[1])) {
                Log.w(TAG, "Unexpected structured response " + error);
            }
            if (split.length > 2) {
                requestId = split[2];
                error = split[3];
                if (error.startsWith(":"))
                    error = error.substring(1);
            } else {
                error = "UNKNOWN";
            }
            resultIntent.putExtra("error", error);
        }
        setResponse(requestId, resultIntent);
        long retryAfter = resultIntent.getLongExtra("Retry-After", 0);
        if (retryAfter > 0) {
            interval = (int) (retryAfter * 1000);
            nextAttempt = SystemClock.elapsedRealtime() + interval;
            Log.d(TAG, "Server requested retry delay: " + interval);
        } else if (ERROR_SERVICE_NOT_AVAILABLE.equals(error) || "AUTHENTICATION_FAILED".equals(error)
                && GSF_PACKAGE_NAME.equals(getIidPackageName(context))) {
            retryCount++;
            if (retryCount < 3) return;
            if (retryCount == 3) interval = 1000 + new Random().nextInt(1000);
            interval = interval * 2;
            nextAttempt = SystemClock.elapsedRealtime() + interval;
            Log.d(TAG, "Setting retry delay to " + interval);
        }
    }

    private synchronized PendingIntent getSelfAuthToken() {
        if (selfAuthToken == null) {
            Intent intent = new Intent();
            intent.setPackage("com.google.example.invalidpackage");
            selfAuthToken = PendingIntent.getBroadcast(context, 0, intent, 0);
        }
        return selfAuthToken;
    }

    private static synchronized String getRequestId() {
        return Integer.toString(lastRequestId++);
    }

    private void sendRegisterMessage(Bundle data, KeyPair keyPair, String requestId) throws IOException {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        if (nextAttempt != 0 && elapsedRealtime <= nextAttempt) {
            Log.w(TAG, "Had to wait for " + interval + ", that's still " + (nextAttempt - elapsedRealtime));
            throw new IOException(ERROR_BACKOFF);
        }
        initialize();
        if (iidPackageName == null) {
            throw new IOException(ERROR_MISSING_INSTANCEID_SERVICE);
        }
        Intent intent = new Intent(ACTION_C2DM_REGISTER);
        intent.setPackage(iidPackageName);
        data.putString(EXTRA_GMS_VERSION, Integer.toString(getGmsVersionCode(context)));
        data.putString(EXTRA_OS_VERSION, Integer.toString(Build.VERSION.SDK_INT));
        data.putString(EXTRA_APP_VERSION_CODE, Integer.toString(getSelfVersionCode(context)));
        data.putString(EXTRA_APP_VERSION_NAME, getSelfVersionName(context));
        data.putString(EXTRA_CLIENT_VERSION, "iid-" + GMS_VERSION_CODE);
        data.putString(EXTRA_APP_ID, InstanceID.sha1KeyPair(keyPair));
        String pub = base64encode(keyPair.getPublic().getEncoded());
        data.putString(EXTRA_PUBLIC_KEY, pub);
        data.putString(EXTRA_SIGNATURE, sign(keyPair, context.getPackageName(), pub));
        intent.putExtras(data);
        intent.putExtra(EXTRA_APP, getSelfAuthToken());
        sendRequest(intent, requestId);
    }

    private static String sign(KeyPair keyPair, String... payload) {
        byte[] bytes;
        try {
            bytes = TextUtils.join("\n", payload).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to encode", e);
            return null;
        }
        PrivateKey privateKey = keyPair.getPrivate();
        try {
            Signature signature = Signature.getInstance(privateKey instanceof RSAPrivateKey ? "SHA256withRSA" : "SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(bytes);
            return base64encode(signature.sign());
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Unable to sign", e);
            return null;
        }
    }

    private static String base64encode(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE + Base64.NO_PADDING + Base64.NO_WRAP);
    }

    private void sendRequest(Intent intent, String requestId) {
        intent.putExtra(EXTRA_KID, "|ID|" + requestId + "|");
        intent.putExtra("X-" + EXTRA_KID, "|ID|" + requestId + "|");
        Log.d(TAG, "Sending " + intent.getExtras());
        if (messenger != null) {
            intent.putExtra(EXTRA_MESSENGER, myMessenger);
            Message msg = Message.obtain();
            msg.obj = intent;
            try {
                messenger.send(msg);
                return;
            } catch (RemoteException e) {
                Log.d(TAG, "Messenger failed, falling back to service");
            }
        }

        boolean useGsf = iidPackageName.endsWith(".gsf");
        if (intent.hasExtra(EXTRA_USE_GSF))
            useGsf = "1".equals(intent.getStringExtra(EXTRA_USE_GSF));

        if (useGsf) {
            Intent holder = new Intent(ACTION_INSTANCE_ID);
            holder.setPackage(context.getPackageName());
            holder.putExtra(EXTRA_GSF_INTENT, intent);
            context.startService(holder);
        } else {
            intent.putExtra(EXTRA_MESSENGER, myMessenger);
            intent.putExtra(EXTRA_IS_MESSENGER2, "1");
            if (messengerCompat != null) {
                Message msg = Message.obtain();
                msg.obj = intent;
                try {
                    messengerCompat.send(msg);
                    return;
                } catch (RemoteException e) {
                    Log.d(TAG, "Messenger failed, falling back to service");
                }
            }
            context.startService(intent);
        }
    }

    public Intent sendRegisterMessageBlocking(Bundle data, KeyPair keyPair) throws IOException {
        Intent intent = sendRegisterMessageBlockingInternal(data, keyPair);
        if (intent != null && intent.hasExtra(EXTRA_MESSENGER)) {
            // Now with a messenger
            intent = sendRegisterMessageBlockingInternal(data, keyPair);
        }
        return intent;
    }

    private Intent sendRegisterMessageBlockingInternal(Bundle data, KeyPair keyPair) throws IOException {
        ConditionVariable cv = new ConditionVariable();
        String requestId = getRequestId();
        synchronized (InstanceIdRpc.class) {
            blockingResponses.put(requestId, cv);
        }

        sendRegisterMessage(data, keyPair, requestId);

        cv.block(BLOCKING_WAIT_TIME);
        synchronized (InstanceIdRpc.class) {
            Object res = blockingResponses.remove(requestId);
            if (res instanceof Intent) {
                return (Intent) res;
            } else if (res instanceof String) {
                throw new IOException((String) res);
            }
            Log.w(TAG, "No response " + res);
            throw new IOException(ERROR_TIMEOUT);
        }
    }

    public String handleRegisterMessageResult(Intent resultIntent) throws IOException {
        if (resultIntent == null) throw new IOException(ERROR_SERVICE_NOT_AVAILABLE);
        String result = resultIntent.getStringExtra(EXTRA_REGISTRATION_ID);
        if (result == null) result = resultIntent.getStringExtra(EXTRA_UNREGISTERED);
        if (result != null) return result;
        result = resultIntent.getStringExtra(EXTRA_ERROR);
        throw new IOException(result != null ? result : ERROR_SERVICE_NOT_AVAILABLE);
    }

    private void setResponse(String requestId, Object response) {
        if (requestId == null) {
            for (String r : blockingResponses.keySet()) {
                setResponse(r, response);
            }
        }
        Object old = blockingResponses.get(requestId);
        blockingResponses.put(requestId, response);
        if (old instanceof ConditionVariable) {
            ((ConditionVariable) old).open();
        } else if (old instanceof Messenger) {
            Message msg = Message.obtain();
            msg.obj = response;
            try {
                ((Messenger) old).send(msg);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to send response", e);
            }
        }
    }
}
