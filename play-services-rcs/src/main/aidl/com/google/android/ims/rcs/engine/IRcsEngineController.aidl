/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.ims.rcs.engine;

import com.google.android.ims.rcs.engine.RcsEngineLifecycleServiceResult;
import com.google.android.ims.rcsservice.lifecycle.InitializeAndStartRcsTransportRequest;
import com.google.android.ims.rcsservice.lifecycle.StopAllRcsTransportsExceptRequest;

interface IRcsEngineController {
    RcsEngineLifecycleServiceResult initialize(int subId, int flags);
    RcsEngineLifecycleServiceResult destroy(int subId);
    RcsEngineLifecycleServiceResult triggerStartRcsStack(int subId);
    RcsEngineLifecycleServiceResult triggerStopRcsStack(int subId);
    RcsEngineLifecycleServiceResult initializeAndStartRcsTransport(in InitializeAndStartRcsTransportRequest request);
    RcsEngineLifecycleServiceResult stopAllRcsTransportsExcept(in StopAllRcsTransportsExceptRequest request);
}
