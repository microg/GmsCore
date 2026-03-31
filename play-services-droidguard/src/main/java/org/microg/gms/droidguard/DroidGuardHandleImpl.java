/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import android.util.Log;

import com.google.android.gms.droidguard.DroidGuardHandle;
import com.google.android.gms.droidguard.internal.DroidGuardInitReply;
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
    private long currentSessionId = -1;

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
        } else if (handle == null) {
            result = Utils.getErrorBytes("Handle not initialized");
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
    public long begin(String flow, DroidGuardResultsRequest request, Map<String, String> initialData) {
        if (error != null || handle == null) {
            return -1;
        }
        
        ArrayBlockingQueue<Long> resultQueue = new ArrayBlockingQueue<>(1);
        apiClient.runOnHandler(() -> {
            try {
                long sessionId = handle.begin(flow, request, initialData);
                currentSessionId = sessionId;
                resultQueue.offer(sessionId);
            } catch (Exception e) {
                error = Utils.getErrorBytes("Begin multi-step failed: " + e);
                resultQueue.offer(-1L);
            }
        });
        
        try {
            Long result = resultQueue.poll(request.getTimeoutMillis(), TimeUnit.MILLISECONDS);
            return result != null ? result : -1;
        } catch (InterruptedException e) {
            error = Utils.getErrorBytes("Begin timeout: " + request.getTimeoutMillis() + " ms");
            return -1;
        }
    }

    @Override
    public DroidGuardInitReply nextStep(long sessionId, Map<String, String> stepData) {
        if (error != null || handle == null || sessionId != currentSessionId) {
            return null;
        }
        
        ArrayBlockingQueue<DroidGuardInitReply> resultQueue = new ArrayBlockingQueue<>(1);
        apiClient.runOnHandler(() -> {
            try {
                DroidGuardInitReply reply = handle.nextStep(sessionId, stepData);
                resultQueue.offer(reply);
            } catch (Exception e) {
                error = Utils.getErrorBytes("Next step failed: " + e);
                resultQueue.offer(null);
            }
        });
        
        try {
            return resultQueue.poll(request.getTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            error = Utils.getErrorBytes("Next step timeout: " + request.getTimeoutMillis() + " ms");
            return null;
        }
    }

    @Override
    public String snapshotWithSession(long sessionId, Map<String, String> data) {
        if (error != null || handle == null || sessionId != currentSessionId) {
            return Utils.toBase64(Utils.getErrorBytes("Invalid session or error state"));
        }
        
        ArrayBlockingQueue<byte[]> resultQueue = new ArrayBlockingQueue<>(1);
        apiClient.runOnHandler(() -> {
            try {
                byte[] result = handle.snapshotWithSession(sessionId, data);
                resultQueue.offer(result);
            } catch (Exception e) {
                error = Utils.getErrorBytes("Snapshot with session failed: " + e);
                resultQueue.offer(Utils.getErrorBytes("Snapshot with session failed: " + e));
            }
        });
        
        try {
            byte[] result = resultQueue.poll(request.getTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (result == null) {
                result = Utils.getErrorBytes("Snapshot with session timeout: " + request.getTimeoutMillis() + " ms");
            }
            return Utils.toBase64(result);
        } catch (InterruptedException e) {
            return Utils.toBase64(Utils.getErrorBytes("Results transfer failed: " + e));
        }
    }

    @Override
    public void closeSession(long sessionId) {
        if (sessionId == currentSessionId && handle != null) {
            apiClient.runOnHandler(() -> {
                try {
                    handle.closeSession(sessionId);
                    currentSessionId = -1;
                } catch (Exception e) {
                    Log.w(TAG, "Error while closing session: " + e);
                }
            });
        }
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
