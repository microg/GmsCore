/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.safetynet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import org.microg.gms.common.Build;
import org.microg.gms.common.Constants;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.Utils;
import org.microg.gms.snet.AttestRequest;
import org.microg.gms.snet.AttestResponse;
import org.microg.gms.snet.FileState;
import org.microg.gms.snet.SELinuxState;
import org.microg.gms.snet.SafetyNetData;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import okio.ByteString;

public class Attestation {
    private static final String TAG = "GmsSafetyNetAttest";

    private Context context;
    private String packageName;
    private byte[] payload;
    private String droidGaurdResult;

    public Attestation(Context context, String packageName) {
        this.context = context;
        this.packageName = packageName;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public byte[] buildPayload(byte[] nonce) {
        this.droidGaurdResult = null;
        SafetyNetData payload = new SafetyNetData.Builder()
                .nonce(ByteString.of(nonce))
                .currentTimeMs(System.currentTimeMillis())
                .packageName(packageName)
                .fileDigest(getPackageFileDigest())
                .signatureDigest(getPackageSignatures())
                .gmsVersionCode(Constants.GMS_VERSION_CODE)
                //.googleCn(false)
                .seLinuxState(new SELinuxState.Builder().enabled(true).supported(true).build())
                .suCandidates(Collections.<FileState>emptyList())
                .build();
        Log.d(TAG, "Payload: "+payload.toString());
        return this.payload = payload.encode();
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getPayloadHashBase64() {
        try {
            MessageDigest digest = getSha256Digest();
            return Base64.encodeToString(digest.digest(payload), Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private static MessageDigest getSha256Digest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    public void setDroidGaurdResult(String droidGaurdResult) {
        this.droidGaurdResult = droidGaurdResult;
    }

    private ByteString getPackageFileDigest() {
        try {
            return ByteString.of(getPackageFileDigest(context, packageName));
        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        }
    }

    public static byte[] getPackageFileDigest(Context context, String packageName) throws Exception {
        FileInputStream is = new FileInputStream(new File(context.getPackageManager().getApplicationInfo(packageName, 0).sourceDir));
        MessageDigest digest = getSha256Digest();
        byte[] data = new byte[4096];
        while (true) {
            int read = is.read(data);
            if (read < 0) break;
            digest.update(data, 0, read);
        }
        is.close();
        return digest.digest();
    }

    @SuppressLint("PackageManagerGetSignatures")
    private List<ByteString> getPackageSignatures() {
        try {
            ArrayList<ByteString> res = new ArrayList<>();
            for (byte[] bytes : getPackageSignatures(context, packageName)) {
                res.add(ByteString.of(bytes));
            }
            return res;
        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        }
    }

    public static byte[][] getPackageSignatures(Context context, String packageName) throws Exception {
        PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        ArrayList<byte[]> res = new ArrayList<>();
        MessageDigest digest = getSha256Digest();
        for (Signature signature : pi.signatures) {
            res.add(digest.digest(signature.toByteArray()));
        }
        return res.toArray(new byte[][]{});
    }

    public String attest(String apiKey) throws IOException {
        if (payload == null) {
            throw new IllegalStateException("missing payload");
        }
        return attest(new AttestRequest.Builder().safetyNetData(ByteString.of(payload)).droidGuardResult(droidGaurdResult).build(), apiKey).result;
    }

    private AttestResponse attest(AttestRequest request, String apiKey) throws IOException {
        String requestUrl = SafetyNetPrefs.get(context).getServiceUrl() + "?alt=PROTO&key=" + apiKey;
        HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("content-type", "application/x-protobuf");
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setRequestProperty("X-Android-Package", packageName);
        connection.setRequestProperty("X-Android-Cert", PackageUtils.firstSignatureDigest(context, packageName));
        Build build = Utils.getBuild(context);
        connection.setRequestProperty("User-Agent", "SafetyNet/" + Constants.GMS_VERSION_CODE + " (" + build.device + " " + build.id + "); gzip");

        OutputStream os = connection.getOutputStream();
        os.write(request.encode());
        os.close();

        if (connection.getResponseCode() != 200) {
            byte[] bytes = null;
            String ex = null;
            try {
                bytes = Utils.readStreamToEnd(connection.getErrorStream());
                ex = new String(Utils.readStreamToEnd(new GZIPInputStream(new ByteArrayInputStream(bytes))));
            } catch (Exception e) {
                if (bytes != null) {
                    throw new IOException(getBytesAsString(bytes), e);
                }
                throw new IOException(connection.getResponseMessage(), e);
            }
            throw new IOException(ex);
        }

        InputStream is = connection.getInputStream();
        byte[] bytes = Utils.readStreamToEnd(new GZIPInputStream(is));
        try {
            return AttestResponse.ADAPTER.decode(bytes);
        } catch (IOException e) {
            Log.d(TAG, Base64.encodeToString(bytes, 0));
            throw e;
        }
    }


    private String getBytesAsString(byte[] bytes) {
        if (bytes == null) return "null";
        try {
            CharsetDecoder d = Charset.forName("US-ASCII").newDecoder();
            CharBuffer r = d.decode(ByteBuffer.wrap(bytes));
            return r.toString();
        } catch (Exception e) {
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        }
    }
}
