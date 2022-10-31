/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.droidguard;

import android.content.Context;

import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;
import com.google.android.gms.tasks.Task;

import java.util.Map;

public interface DroidGuardClient {
    Task<DroidGuardHandle> init(String flow, DroidGuardResultsRequest request);

    Task<String> getResults(String flow, Map<String, String> data, DroidGuardResultsRequest request);

    static Task<DroidGuardHandle> init(Context context, String flow) {
        return DroidGuard.getClient(context).init(flow, null);
    }

    static Task<String> getResults(Context context, String flow, Map<String, String> data) {
        return DroidGuard.getClient(context).getResults(flow, data, null);
    }
}
