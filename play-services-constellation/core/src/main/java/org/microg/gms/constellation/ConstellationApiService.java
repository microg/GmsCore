/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.Feature;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.constellation.GetIidTokenRequest;
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest;
import com.google.android.gms.constellation.VerifyPhoneNumberRequest;
import com.google.android.gms.constellation.internal.IConstellationApiService;
import com.google.android.gms.constellation.internal.IConstellationCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Constellation (phone-number verification) service for RCS provisioning (issue #2994).
 *
 * <p>Google Messages calls this service before it can register a phone number for RCS:
 * it asks what verification methods the device supports
 * ({@code getPnvCapabilities}), fetches an IID token ({@code getIidToken}), and then
 * asks to verify the number ({@code verifyPhoneNumber}). Capabilities and the IID
 * token are implemented for real; the verification itself is staged (see
 * {@link VerifyPhoneNumber}) until the server RPC lands.
 *
 * <p>Only Google packages may bind, matching the upstream security model. No phone
 * number, IMSI, or token is ever logged.
 */
public class ConstellationApiService extends BaseService {
    private static final String TAG = "GmsConstellationSvc";

    /** Features the calling app may query; names and versions are part of the contract. */
    public static final Feature[] FEATURES = new Feature[]{
            new Feature("asterism_consent", 3),
            new Feature("one_time_verification", 1),
            new Feature("carrier_auth", 1),
            new Feature("verify_phone_number", 2),
            new Feature("get_iid_token", 1),
            new Feature("get_pnv_capabilities", 1),
            new Feature("ts43", 1),
            new Feature("verify_phone_number_local_read", 1)
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ConstellationApiService() {
        super(TAG, GmsService.CONSTELLATION);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        String packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName);
        if (!PackageUtils.isGooglePackage(this, packageName)) {
            throw new SecurityException(packageName + " is not a Google package");
        }
        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.features = FEATURES;
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS,
                new ConstellationApiServiceImpl(this, packageName, executor), connectionInfo);
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(fd, writer, args);
        ConstellationStateStore store = new ConstellationStateStore(this);
        writer.println("Verified numbers: " + store.getVerified().size());
        writer.println("Recent verification attempts:");
        for (String attempt : store.getAttempts()) {
            writer.println("  " + attempt);
        }
    }

    static class ConstellationApiServiceImpl extends IConstellationApiService.Stub {
        private static final String TAG = "GmsConstellationImpl";

        private final ConstellationApiService service;
        private final String packageName;
        private final ExecutorService executor;

        ConstellationApiServiceImpl(ConstellationApiService service, String packageName, ExecutorService executor) {
            this.service = service;
            this.packageName = packageName;
            this.executor = executor;
        }

        @Override
        public void verifyPhoneNumberV1(IConstellationCallbacks cb, Bundle bundle, ApiMetadata apiMetadata) {
            Log.i(TAG, "verifyPhoneNumberV1(): policy=" + (bundle != null ? bundle.getString("policy_id") : null));
            if (cb == null || bundle == null) return;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
            final Bundle requestBundle = new Bundle(bundle);
            executor.execute(() -> VerifyPhoneNumber.handleV1(service, cb, requestBundle, packageName));
        }

        @Override
        public void verifyPhoneNumberSingleUse(IConstellationCallbacks cb, Bundle bundle, ApiMetadata apiMetadata) {
            Log.i(TAG, "verifyPhoneNumberSingleUse()");
            if (cb == null || bundle == null) return;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
            final Bundle requestBundle = new Bundle(bundle);
            executor.execute(() -> VerifyPhoneNumber.handleSingleUse(service, cb, requestBundle, packageName));
        }

        @Override
        public void verifyPhoneNumber(IConstellationCallbacks cb, VerifyPhoneNumberRequest request, ApiMetadata apiMetadata) {
            Log.i(TAG, "verifyPhoneNumber(): apiVersion=" + (request != null ? request.apiVersion : null)
                    + ", policy=" + (request != null ? request.policyId : null));
            if (cb == null || request == null) return;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
            executor.execute(() -> VerifyPhoneNumber.handleRequest(service, cb, request, packageName, false));
        }

        @Override
        public void getIidToken(IConstellationCallbacks cb, GetIidTokenRequest request, ApiMetadata apiMetadata) {
            Log.i(TAG, "getIidToken()");
            if (cb == null || request == null) return;
            executor.execute(() -> GetIidToken.handle(service, cb, request));
        }

        @Override
        public void getPnvCapabilities(IConstellationCallbacks cb, GetPnvCapabilitiesRequest request, ApiMetadata apiMetadata) {
            Log.i(TAG, "getPnvCapabilities()");
            if (cb == null || request == null) return;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return;
            executor.execute(() -> GetPnvCapabilities.handle(service, cb, request));
        }
    }
}
