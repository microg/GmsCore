/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.iid.InstanceID;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Instance-ID based auth for Constellation server calls.
 *
 * <p>Provides the three things the Constellation protocol needs from the device
 * identity: an IID token for a Google project number (via microG's existing
 * {@link InstanceID} implementation), the Firebase installation id, and a signature
 * over the token in the format Google's server expects
 * ({@code {iidToken}:{epochSeconds}:{nanos}}, SHA256withECDSA with a per-device EC
 * P-256 key pair kept in SharedPreferences).
 *
 * <p>The key pair is device-local and never leaves the device except as signatures.
 */
public class ConstellationAuth {
    private static final String TAG = "GmsConstellationAuth";

    private static final String PREFS_NAME = "constellation_prefs";
    private static final String KEY_PRIVATE = "private_key";
    private static final String KEY_PUBLIC = "public_key";

    /** Google project number used when the caller does not supply one. */
    public static final String DEFAULT_PROJECT_NUMBER = "496232013492";

    private final Context context;
    private final SharedPreferences prefs;

    public ConstellationAuth(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns an IID token authorizing {@code projectNumber} (or the default project
     * number when null) for the "GCM" scope. May throw when no network or no Google
     * account identity is available; callers must handle that.
     */
    public String getIidToken(Long projectNumber) throws Exception {
        String sender = projectNumber != null ? projectNumber.toString() : DEFAULT_PROJECT_NUMBER;
        return InstanceID.getInstance(context).getToken(sender, "GCM");
    }

    /** Returns the Firebase installation id for this device. */
    public String getFid() {
        return InstanceID.getInstance(context).getId();
    }

    /**
     * Signs {@code iidToken} in Google's format and returns the signature plus the
     * timestamp the signature covers, in milliseconds since the epoch.
     */
    public SignResult signIidToken(String iidToken) {
        long currentTimeMillis = System.currentTimeMillis();
        long epochSecond = currentTimeMillis / 1000;
        long nanos = (currentTimeMillis % 1000) * 1_000_000;
        String content = buildSignedContent(iidToken, epochSecond, nanos);
        return new SignResult(sign(content), currentTimeMillis);
    }

    /** Builds the exact string that gets signed: {@code {token}:{seconds}:{nanos}}. */
    public static String buildSignedContent(String iidToken, long epochSecond, long nanos) {
        return iidToken + ":" + epochSecond + ":" + nanos;
    }

    private byte[] sign(String content) {
        try {
            KeyPair keyPair = getOrCreateKeyPair();
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.sign();
        } catch (Exception e) {
            Log.w(TAG, "signing failed", e);
            return new byte[0];
        }
    }

    private KeyPair getOrCreateKeyPair() throws Exception {
        String privateKeyStr = prefs.getString(KEY_PRIVATE, null);
        String publicKeyStr = prefs.getString(KEY_PUBLIC, null);
        if (privateKeyStr != null && publicKeyStr != null) {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("EC");
                return new KeyPair(
                        keyFactory.generatePublic(new X509EncodedKeySpec(Base64.decode(publicKeyStr, Base64.DEFAULT))),
                        keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(privateKeyStr, Base64.DEFAULT))));
            } catch (Exception e) {
                Log.w(TAG, "stored key pair unreadable, regenerating", e);
            }
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair keyPair = generator.generateKeyPair();
        prefs.edit()
                .putString(KEY_PRIVATE, Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.NO_WRAP))
                .putString(KEY_PUBLIC, Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.NO_WRAP))
                .apply();
        return keyPair;
    }

    /** A signature plus the timestamp it covers. */
    public static class SignResult {
        public final byte[] signature;
        public final long timestampMillis;

        public SignResult(byte[] signature, long timestampMillis) {
            this.signature = signature;
            this.timestampMillis = timestampMillis;
        }
    }
}
