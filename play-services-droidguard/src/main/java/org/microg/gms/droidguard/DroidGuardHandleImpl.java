/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import android.util.Log;

import com.google.android.gms.droidguard.DroidGuardHandle;
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;
import com.google.android.gms.droidguard.internal.IDroidGuardHandle;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DroidGuardHandleImpl implements DroidGuardHandle {
    private static final String TAG = "DroidGuardHandle";
    private final DroidGuardApiClient apiClient;
    private final DroidGuardResultsRequest request;
    private IDroidGuardHandle handle;
    private byte[] error;

    public DroidGuardHandleImpl(DroidGuardApiClient apiClient, DroidGuardResultsRequest request, IDroidGuardHandle handle) {
        this.apiClient = apiClient;
        this.request = request;
        this.handle = handle;
    }

    public DroidGuardHandleImpl(DroidGuardApiClient apiClient, DroidGuardResultsRequest request, String error) {
        this.apiClient = apiClient;
        this.request = request;
        this.error = Utils.getErrorBytes(error);
    }

    @Override
    public String snapshot(Map<String, String> data) {
        byte[] result;
        if (error != null) {
            result = error;
        } else {
            ArrayBlockingQueue<byte[]> resultQueue = new ArrayBlockingQueue<>(1);
            apiClient.runOnHandler(() -> {
                byte[] innerResult;
                try {
                    innerResult = handle.snapshot(data);
                    if (innerResult == null) {
                        error = Utils.getErrorBytes("Received null");
                        innerResult = error;
                    }
                } catch (Exception e) {
                    error = Utils.getErrorBytes("Snapshot failed: " + e);
                    innerResult = error;
                }
                resultQueue.offer(innerResult);
            });
            try {
                result = resultQueue.poll(request.getTimeoutMillis(), TimeUnit.MILLISECONDS);
                if (result == null) {
                    result = Utils.getErrorBytes("Snapshot timeout: " + request.getTimeoutMillis() + " ms");
                }
            } catch (InterruptedException e) {
                result = Utils.getErrorBytes("Results transfer failed: " + e);
            }
        }
        return Utils.toBase64(result);
    }

    @Override
    public boolean isOpened() {
        return handle != null && error == null && handle.asBinder().pingBinder();
    }

    @Override
    public void close() {
        apiClient.runOnHandler(() -> {
            if (handle != null) {
                try {
                    handle.close();
                } catch (Exception e) {
                    Log.w(TAG, "Error while closing handle.");
                }
                apiClient.markHandleClosed();
                handle = null;
            }
        });
    }
}
