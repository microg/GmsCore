/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.droidguard;

import android.content.Context;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;
import com.google.android.gms.tasks.Task;

import java.util.Map;

public interface DroidGuardClient {
    @NonNull Task<DroidGuardHandle> init(@NonNull String flow, @Nullable DroidGuardResultsRequest request);

    @NonNull Task<String> getResults(@NonNull String flow, @Nullable Map<String, String> data, @Nullable DroidGuardResultsRequest request);

    /**
     * Runs the given flow using the multi-step session API
     * ({@link DroidGuardHandle#begin}, {@link DroidGuardHandle#nextStep},
     * {@link DroidGuardHandle#snapshotWithSession}) instead of the single-shot
     * {@link DroidGuardHandle#snapshot}. This is required for flows (such as the
     * PoToken / Play Integrity flow) when DroidGuard is executed remotely, as the
     * remote protocol negotiates multiple steps.
     */
    @NonNull Task<String> getResultsMultiStep(@NonNull String flow, @Nullable Map<String, String> data, @Nullable DroidGuardResultsRequest request);

    @NonNull
    static Task<String> getResultsMultiStep(@NonNull Context context, @NonNull String flow, @Nullable Map<String, String> data, @Nullable DroidGuardResultsRequest request) {
        return DroidGuard.getClient(context).getResultsMultiStep(flow, data, request);
    }

    @NonNull
    static Task<DroidGuardHandle> init(@NonNull Context context, @NonNull String flow) {
        return DroidGuard.getClient(context).init(flow, null);
    }

    @NonNull
    static Task<String> getResults(@NonNull Context context, @NonNull String flow, @Nullable Map<String, String> data) {
        return getResults(context, flow, data, (DroidGuardResultsRequest) null);
    }

    @NonNull
    static Task<String> getResults(@NonNull Context context, @NonNull String flow, @Nullable Map<String, String> data, @Nullable Bundle extras) {
        DroidGuardResultsRequest request = null;
        if (extras != null) {
            request = new DroidGuardResultsRequest();
            request.bundle.putAll(extras);
        }
        return getResults(context, flow, data, request);
    }

    @NonNull
    static Task<String> getResults(@NonNull Context context, @NonNull String flow, @Nullable Map<String, String> data, @Nullable DroidGuardResultsRequest request) {
        return DroidGuard.getClient(context).getResults(flow, data, request);
    }
}
