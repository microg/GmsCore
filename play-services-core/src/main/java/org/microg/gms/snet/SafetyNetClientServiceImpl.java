/*
 * Copyright 2013-2016 microG Project Team
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

package org.microg.gms.snet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.AttestationData;
import com.google.android.gms.safetynet.HarmfulAppsData;
import com.google.android.gms.safetynet.internal.ISafetyNetCallbacks;
import com.google.android.gms.safetynet.internal.ISafetyNetService;
import com.squareup.wire.Wire;

import org.microg.gms.common.Constants;
import org.microg.gms.common.PackageUtils;
import org.microg.gms.common.Utils;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import okio.ByteString;

public class SafetyNetClientServiceImpl extends ISafetyNetService.Stub {
    private static final String TAG = "GmsSafetyNetClientImpl";
    public static final String ATTEST_URL = "https://www.googleapis.com/androidcheck/v1/attestations/attest?alt=PROTO&key=AIzaSyDqVnJBjE5ymo--oBJt3On7HQx9xNm1RHA";

    private Context context;
    private String packageName;

    public SafetyNetClientServiceImpl(Context context, String packageName) {
        this.context = context;
        this.packageName = packageName;
    }

    private ByteString getPackageFileDigest() {
        try {
            FileInputStream is = new FileInputStream(new File(context.getPackageManager().getApplicationInfo(packageName, 0).sourceDir));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] data = new byte[16384];
            while (true) {
                int read = is.read(data);
                if (read < 0) break;
                digest.update(data, 0, read);
            }
            return ByteString.of(digest.digest());
        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        }
    }

    @SuppressLint("PackageManagerGetSignatures")
    private List<ByteString> getPackageSignatures() {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            ArrayList<ByteString> res = new ArrayList<>();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Signature signature : pi.signatures) {
                res.add(ByteString.of(digest.digest(signature.toByteArray())));
            }
            return res;
        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        }
    }

    @Override
    public void attest(final ISafetyNetCallbacks callbacks, final byte[] nonce) throws RemoteException {
        if (nonce == null) {
            callbacks.onAttestationData(new Status(10), null);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                SafetyNetData payload = new SafetyNetData.Builder()
                        .nonce(ByteString.of(nonce))
                        .currentTimeMs(System.currentTimeMillis())
                        .packageName(packageName)
                        .fileDigest(getPackageFileDigest())
                        .signatureDigest(getPackageSignatures())
                        .gmsVersionCode(Constants.MAX_REFERENCE_VERSION)
                        .googleCn(false)
                        .seLinuxState(new SELinuxState(true, true))
                        .suCandidates(Collections.<FileState>emptyList())
                        .build();

                AttestRequest request = new AttestRequest(ByteString.of(payload.toByteArray()), "");

                Log.d(TAG, "attest: " + payload);

                try {
                    try {
                        AttestResponse response = attest(request);
                        callbacks.onAttestationData(Status.SUCCESS, new AttestationData(response.result));
                    } catch (IOException e) {
                        Log.w(TAG, e);
                        callbacks.onAttestationData(Status.INTERNAL_ERROR, null);
                    }
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                }
            }
        }).start();
    }

    private AttestResponse attest(AttestRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(ATTEST_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-type", "application/x-protobuf");
        connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setRequestProperty("User-Agent", "SafetyNet/" + Constants.MAX_REFERENCE_VERSION);

        Log.d(TAG, "-- Request --\n" + request);
        OutputStream os = new GZIPOutputStream(connection.getOutputStream());
        os.write(request.toByteArray());
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
        AttestResponse response = new Wire().parseFrom(new GZIPInputStream(is), AttestResponse.class);
        is.close();
        return response;
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

    @Override
    public void getSharedUuid(ISafetyNetCallbacks callbacks) throws RemoteException {
        PackageUtils.checkPackageUid(context, packageName, getCallingUid());
        PackageUtils.assertExtendedAccess(context);

        // TODO
        Log.d(TAG, "dummy Method: getSharedUuid");
        callbacks.onString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    }

    @Override
    public void lookupUri(ISafetyNetCallbacks callbacks, String s1, int[] threatTypes, int i, String s2) throws RemoteException {
        Log.d(TAG, "unimplemented Method: lookupUri");

    }

    @Override
    public void init(ISafetyNetCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "dummy Method: init");
        callbacks.onBoolean(Status.SUCCESS, true);
    }

    @Override
    public void unknown4(ISafetyNetCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "dummy Method: unknown4");
        callbacks.onHarmfulAppsData(Status.SUCCESS, new ArrayList<HarmfulAppsData>());
    }
}
