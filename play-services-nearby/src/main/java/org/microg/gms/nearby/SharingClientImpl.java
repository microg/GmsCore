/*
 * SPDX-FileCopyrightText: 2023, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.nearby.sharing.ShareCallback;
import com.google.android.gms.nearby.sharing.ShareTarget;
import com.google.android.gms.nearby.sharing.SharingClient;
import com.google.android.gms.nearby.sharing.TransferMetadata;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class SharingClientImpl implements SharingClient {
    private final Context context;
    private volatile boolean sharingEnabled = true;
    private volatile int visibility = VISIBILITY_EVERYONE;
    private final Set<ShareCallback> receiveCallbacks = new CopyOnWriteArraySet<>();
    private final Map<ShareCallback, ShareCallback> sendCallbacks = new ConcurrentHashMap<>();

    public SharingClientImpl(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public Task<Boolean> isSharingEnabled() {
        return Tasks.forResult(sharingEnabled);
    }

    @Override
    public Task<Void> setSharingEnabled(boolean enabled) {
        this.sharingEnabled = enabled;
        return Tasks.forResult(null);
    }

    @Override
    public Task<Integer> getDeviceVisibility() {
        return Tasks.forResult(visibility);
    }

    @Override
    public Task<Void> setDeviceVisibility(int visibility) {
        if (visibility < VISIBILITY_HIDDEN || visibility > VISIBILITY_EVERYONE) {
            return Tasks.forException(new IllegalArgumentException("Invalid visibility: " + visibility));
        }
        this.visibility = visibility;
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> registerReceiveSurface(ShareCallback callback, int surfaceType) {
        if (callback == null) {
            return Tasks.forException(new NullPointerException("ShareCallback must not be null"));
        }
        receiveCallbacks.add(callback);
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> unregisterReceiveSurface(ShareCallback callback) {
        if (callback != null) {
            receiveCallbacks.remove(callback);
        }
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> registerSendSurface(ShareCallback transferCallback, ShareCallback discoveryCallback, int surfaceType) {
        if (transferCallback == null || discoveryCallback == null) {
            return Tasks.forException(new NullPointerException("Send surface callbacks must not be null"));
        }
        sendCallbacks.put(transferCallback, discoveryCallback);
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> unregisterSendSurface(ShareCallback transferCallback) {
        if (transferCallback != null) {
            sendCallbacks.remove(transferCallback);
        }
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> send(ShareTarget shareTarget, Intent contentIntent) {
        if (shareTarget == null || contentIntent == null) {
            return Tasks.forException(new IllegalArgumentException("ShareTarget and contentIntent are required"));
        }
        String pin = derivePinToken(shareTarget.getEndpointId());
        TransferMetadata connecting = new TransferMetadata(TransferMetadata.STATUS_CONNECTING, 0.0f, pin);
        TransferMetadata complete = new TransferMetadata(TransferMetadata.STATUS_COMPLETE, 100.0f, pin);
        for (ShareCallback cb : sendCallbacks.keySet()) {
            cb.onTransferUpdate(shareTarget, connecting);
            cb.onTransferUpdate(shareTarget, complete);
        }
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> accept(ShareTarget shareTarget) {
        if (shareTarget == null) {
            return Tasks.forException(new IllegalArgumentException("ShareTarget is required"));
        }
        String pin = derivePinToken(shareTarget.getEndpointId());
        TransferMetadata complete = new TransferMetadata(TransferMetadata.STATUS_COMPLETE, 100.0f, pin);
        for (ShareCallback cb : receiveCallbacks) {
            cb.onTransferUpdate(shareTarget, complete);
        }
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> reject(ShareTarget shareTarget) {
        if (shareTarget == null) {
            return Tasks.forException(new IllegalArgumentException("ShareTarget is required"));
        }
        TransferMetadata rejected = new TransferMetadata(TransferMetadata.STATUS_REJECTED, 0.0f, null);
        for (ShareCallback cb : receiveCallbacks) {
            cb.onTransferUpdate(shareTarget, rejected);
        }
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> cancel(ShareTarget shareTarget) {
        if (shareTarget == null) {
            return Tasks.forException(new IllegalArgumentException("ShareTarget is required"));
        }
        TransferMetadata cancelled = new TransferMetadata(TransferMetadata.STATUS_CANCELLED, 0.0f, null);
        for (ShareCallback cb : sendCallbacks.keySet()) {
            cb.onTransferUpdate(shareTarget, cancelled);
        }
        for (ShareCallback cb : receiveCallbacks) {
            cb.onTransferUpdate(shareTarget, cancelled);
        }
        return Tasks.forResult(null);
    }

    public static String derivePinToken(String endpointId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((endpointId != null ? endpointId : "").getBytes(StandardCharsets.UTF_8));
            int code = ((digest[0] & 0xFF) << 8 | (digest[1] & 0xFF)) % 10000;
            return String.format(Locale.US, "%04d", code);
        } catch (Exception e) {
            return "0000";
        }
    }
}
