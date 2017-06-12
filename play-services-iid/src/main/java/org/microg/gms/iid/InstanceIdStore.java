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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class InstanceIdStore {
    private static final String TAG = "InstanceID/Store";
    private static final String PREF_NAME = "com.google.android.gms.appid";

    private Context context;
    private SharedPreferences sharedPreferences;

    public InstanceIdStore(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized String get(String key) {
        return sharedPreferences.getString(key, null);
    }

    public String get(String subtype, String key) {
        return get(subtype + "|S|" + key);
    }

    public String get(String subtype, String authorizedEntity, String scope) {
        return get(subtype + "|T|" + authorizedEntity + "|" + scope);
    }

    public KeyPair getKeyPair(String subtype) {
        String pub = get(subtype, "|P|");
        String priv = get(subtype, "|K|");
        if (pub == null || priv == null) {
            return null;
        }
        try {
            byte[] pubKey = Base64.decode(pub, Base64.URL_SAFE);
            byte[] privKey = Base64.decode(priv, Base64.URL_SAFE);
            KeyFactory rsaFactory = KeyFactory.getInstance("RSA");
            return new KeyPair(rsaFactory.generatePublic(new X509EncodedKeySpec(pubKey)), rsaFactory.generatePrivate(new PKCS8EncodedKeySpec(privKey)));
        } catch (Exception e) {
            Log.w(TAG, "Invalid key stored " + e);
            return null;
        }
    }

    public synchronized void put(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void put(String subtype, String key, String value) {
        put(subtype + "|S|" + key, value);
    }

    public void put(String subtype, String authorizedEntity, String scope, String value) {
        put(subtype + "|T|" + authorizedEntity + "|" + scope, value);
    }

    public synchronized void put(String subtype, KeyPair keyPair, long timestamp) {
        put(subtype, "|P|", Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING));
        put(subtype, "|K|", Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING));
        put(subtype, "cre", Long.toString(timestamp));
    }

    public synchronized void delete() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public synchronized void delete(String prefix) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String key : sharedPreferences.getAll().keySet()) {
            if (key.startsWith(prefix)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    public synchronized void delete(String subtype, String authorizedEntity, String scope) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(subtype + "|T|" + authorizedEntity + "|" + scope);
        editor.apply();
    }
}
