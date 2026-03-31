/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.droidguard;

import com.google.android.gms.droidguard.internal.DroidGuardInitReply;
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;

import java.util.Map;

public interface DroidGuardHandle {
    String snapshot(Map<String, String> data);

    boolean isOpened();

    void close();
    
    // Multi-step session support for Play Integrity
    long begin(String flow, DroidGuardResultsRequest request, Map<String, String> initialData);
    
    DroidGuardInitReply nextStep(long sessionId, Map<String, String> stepData);
    
    String snapshotWithSession(long sessionId, Map<String, String> data);
    
    void closeSession(long sessionId);
}
