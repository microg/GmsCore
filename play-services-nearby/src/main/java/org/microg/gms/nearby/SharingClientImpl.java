/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby;

import android.content.Context;

import com.google.android.gms.nearby.sharing.LocalShare;
import com.google.android.gms.nearby.sharing.ReceivedShare;
import com.google.android.gms.nearby.sharing.ShareTarget;
import com.google.android.gms.nearby.sharing.SharedContent;
import com.google.android.gms.nearby.sharing.SharingClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of {@link SharingClient} for the microG play-services-nearby stack.
 */
public class SharingClientImpl implements SharingClient {
    private final Context context;
    private final List<ShareTarget> discoveredShareTargets = new CopyOnWriteArrayList<>();
    private final List<ReceivedShare> receivedShares = new CopyOnWriteArrayList<>();

    public SharingClientImpl(Context context) {
        this.context = context;
    }

    @Override
    public Task<Void> startAdvertising(LocalShare share) {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> stopAdvertising() {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> startDiscovery() {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> stopDiscovery() {
        return Tasks.forResult(null);
    }

    @Override
    public Task<List<ShareTarget>> getDiscoveredShareTargets() {
        return Tasks.forResult(Collections.unmodifiableList(new ArrayList<>(discoveredShareTargets)));
    }

    @Override
    public Task<Void> send(ShareTarget target, SharedContent content) {
        if (target == null) {
            return Tasks.forException(new IllegalArgumentException("Share target must not be null"));
        }
        if (content == null) {
            return Tasks.forException(new IllegalArgumentException("Shared content must not be null"));
        }
        return Tasks.forResult(null);
    }

    @Override
    public Task<List<ReceivedShare>> getReceivedShares() {
        return Tasks.forResult(Collections.unmodifiableList(new ArrayList<>(receivedShares)));
    }
}
