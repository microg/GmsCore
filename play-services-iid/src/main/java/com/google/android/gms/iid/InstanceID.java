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

package com.google.android.gms.iid;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.microg.gms.common.PublicApi;
import org.microg.gms.gcm.GcmConstants;
import org.microg.gms.iid.InstanceIdRpc;
import org.microg.gms.iid.InstanceIdStore;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static org.microg.gms.gcm.GcmConstants.EXTRA_DELETE;
import static org.microg.gms.gcm.GcmConstants.EXTRA_SCOPE;
import static org.microg.gms.gcm.GcmConstants.EXTRA_SENDER;
import static org.microg.gms.gcm.GcmConstants.EXTRA_SUBSCIPTION;
import static org.microg.gms.gcm.GcmConstants.EXTRA_SUBTYPE;

/**
 * Instance ID provides a unique identifier for each app instance and a mechanism
 * to authenticate and authorize actions (for example, sending a GCM message).
 * <p/>
 * Instance ID is stable but may become invalid, if:
 * <ul>
 * <li>App deletes Instance ID</li>
 * <li>Device is factory reset</li>
 * <li>User uninstalls the app</li>
 * <li>User clears app data</li>
 * </ul>
 * If Instance ID has become invalid, the app can call {@link com.google.android.gms.iid.InstanceID#getId()}
 * to request a new Instance ID.
 * To prove ownership of Instance ID and to allow servers to access data or
 * services associated with the app, call {@link com.google.android.gms.iid.InstanceID#getToken(java.lang.String, java.lang.String)}.
 */
@PublicApi
public class InstanceID {
    /**
     * Error returned when failed requests are retried too often.  Use
     * exponential backoff when retrying requests
     */
    public static final String ERROR_BACKOFF = "RETRY_LATER";

    /**
     * Blocking methods must not be called on the main thread.
     */
    public static final String ERROR_MAIN_THREAD = "MAIN_THREAD";

    /**
     * Tokens can't be generated. Only devices with Google Play are supported.
     */
    public static final String ERROR_MISSING_INSTANCEID_SERVICE = "MISSING_INSTANCEID_SERVICE";

    /**
     * The device cannot read the response, or there was a server error.
     * Application should retry the request later using exponential backoff
     * and retry (on each subsequent failure increase delay before retrying).
     */
    public static final String ERROR_SERVICE_NOT_AVAILABLE = GcmConstants.ERROR_SERVICE_NOT_AVAILABLE;

    /**
     * Timeout waiting for a response.
     */
    public static final String ERROR_TIMEOUT = "TIMEOUT";

    private static final int RSA_KEY_SIZE = 2048;
    private static final String TAG = "InstanceID";

    private static InstanceIdStore storeInstance;
    private static InstanceIdRpc rpc;
    private static Map<String, InstanceID> instances = new HashMap<String, InstanceID>();

    private final String subtype;
    private KeyPair keyPair;
    private long creationTime;

    private InstanceID(String subtype) {
        this.subtype = subtype == null ? "" : subtype;
    }

    /**
     * Resets Instance ID and revokes all tokens.
     *
     * @throws IOException
     */
    public void deleteInstanceID() throws IOException {
        deleteToken("*", "*");
        creationTime = 0;
        storeInstance.delete(subtype + "|");
        keyPair = null;
    }

    /**
     * Revokes access to a scope (action) for an entity previously
     * authorized by {@link com.google.android.gms.iid.InstanceID#getToken(java.lang.String, java.lang.String)}.
     * <p/>
     * Do not call this function on the main thread.
     *
     * @param authorizedEntity Entity that must no longer have access.
     * @param scope            Action that entity is no longer authorized to perform.
     * @throws IOException if the request fails.
     */
    public void deleteToken(String authorizedEntity, String scope) throws IOException {
        deleteToken(authorizedEntity, scope, null);
    }

    @PublicApi(exclude = true)
    public void deleteToken(String authorizedEntity, String scope, Bundle extras) throws IOException {
        if (Looper.getMainLooper() == Looper.myLooper()) throw new IOException(ERROR_MAIN_THREAD);

        storeInstance.delete(subtype, authorizedEntity, scope);

        if (extras == null) extras = new Bundle();
        extras.putString(EXTRA_SENDER, authorizedEntity);
        extras.putString(EXTRA_SUBSCIPTION, authorizedEntity);
        extras.putString(EXTRA_DELETE, "1");
        extras.putString("X-" + EXTRA_DELETE, "1");
        extras.putString(EXTRA_SUBTYPE, TextUtils.isEmpty(subtype) ? authorizedEntity : subtype);
        extras.putString("X-" + EXTRA_SUBTYPE, TextUtils.isEmpty(subtype) ? authorizedEntity : subtype);
        if (scope != null) extras.putString(EXTRA_SCOPE, scope);

        rpc.handleRegisterMessageResult(rpc.sendRegisterMessageBlocking(extras, getKeyPair()));
    }

    /**
     * Returns time when instance ID was created.
     *
     * @return Time when instance ID was created (milliseconds since Epoch).
     */
    public long getCreationTime() {
        if (creationTime == 0) {
            String s = storeInstance.getSecret(subtype, "cre");
            if (s != null) {
                creationTime = Long.parseLong(s);
            }
        }
        return creationTime;
    }

    /**
     * Returns a stable identifier that uniquely identifies the app instance.
     *
     * @return The identifier for the application instance.
     */
    public String getId() {
        return sha1KeyPair(getKeyPair());
    }

    /**
     * Returns an instance of this class.
     *
     * @return InstanceID instance.
     */
    public static InstanceID getInstance(Context context) {
        String subtype = "";
        if (storeInstance == null) {
            storeInstance = new InstanceIdStore(context.getApplicationContext());
            rpc = new InstanceIdRpc(context.getApplicationContext());
        }
        InstanceID instance = instances.get(subtype);
        if (instance == null) {
            instance = new InstanceID(subtype);
            instances.put(subtype, instance);
        }
        return instance;
    }

    /**
     * Returns a token that authorizes an Entity (example: cloud service) to perform
     * an action on behalf of the application identified by Instance ID.
     * <p/>
     * This is similar to an OAuth2 token except, it applies to the
     * application instance instead of a user.
     * <p/>
     * Do not call this function on the main thread.
     *
     * @param authorizedEntity Entity authorized by the token.
     * @param scope            Action authorized for authorizedEntity.
     * @param extras           additional parameters specific to each token scope.
     *                         Bundle keys starting with 'GCM.' and 'GOOGLE.' are
     *                         reserved.
     * @return a token that can identify and authorize the instance of the
     * application on the device.
     * @throws IOException if the request fails.
     */
    public String getToken(String authorizedEntity, String scope, Bundle extras) throws IOException {
        if (Looper.getMainLooper() == Looper.myLooper()) throw new IOException(ERROR_MAIN_THREAD);

        long tokenTimestamp = storeInstance.getTokenTimestamp(subtype, authorizedEntity, scope);
        if (tokenTimestamp > System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L) {
            String token = storeInstance.getToken(subtype, authorizedEntity, scope);
            if (token != null) return token;
        }
        String token = requestToken(authorizedEntity, scope, extras);
        storeInstance.putToken(subtype, authorizedEntity, scope, token);
        return token;
    }

    /**
     * Returns a token that authorizes an Entity (example: cloud service) to perform
     * an action on behalf of the application identified by Instance ID.
     * <p/>
     * This is similar to an OAuth2 token except, it applies to the
     * application instance instead of a user.
     * <p/>
     * Do not call this function on the main thread.
     *
     * @param authorizedEntity Entity authorized by the token.
     * @param scope            Action authorized for authorizedEntity.
     * @return a token that can identify and authorize the instance of the
     * application on the device.
     * @throws IOException if the request fails.
     */
    public String getToken(String authorizedEntity, String scope) throws IOException {
        return getToken(authorizedEntity, scope, null);
    }

    @PublicApi(exclude = true)
    public InstanceIdStore getStore() {
        return storeInstance;
    }

    @PublicApi(exclude = true)
    public String requestToken(String authorizedEntity, String scope, Bundle extras) {
        throw new UnsupportedOperationException();
    }

    private synchronized KeyPair getKeyPair() {
        if (keyPair == null) {
            keyPair = storeInstance.getKeyPair(subtype);
            if (keyPair == null) {
                try {
                    KeyPairGenerator rsaGenerator = KeyPairGenerator.getInstance("RSA");
                    rsaGenerator.initialize(RSA_KEY_SIZE);
                    keyPair = rsaGenerator.generateKeyPair();
                    creationTime = System.currentTimeMillis();
                    storeInstance.putKeyPair(subtype, keyPair, creationTime);
                } catch (NoSuchAlgorithmException e) {
                    Log.w(TAG, e);
                }
            }
        }
        return keyPair;
    }

    @PublicApi(exclude = true)
    public static String sha1KeyPair(KeyPair keyPair) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA1").digest(keyPair.getPublic().getEncoded());
            digest[0] = (byte) (112 + (0xF & digest[0]) & 0xFF);
            return Base64.encodeToString(digest, 0, 8, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, e);
            return null;
        }
    }
}
