/*
 * Copyright (C) 2025 microG Project Team
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

package com.google.android.gms.rcs.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.rcs.internal.RcsCapabilitiesResponse;
import com.google.android.gms.rcs.internal.RcsConfigurationResponse;
import com.google.android.gms.rcs.internal.RcsMessageResponse;

interface IRcsCallbacks {
    void onRcsCapabilities(in Status status, in RcsCapabilitiesResponse response) = 1;
    void onRcsEnabled(in Status status) = 2;
    void onRcsDisabled(in Status status) = 3;
    void onRcsConfiguration(in Status status, in RcsConfigurationResponse response) = 4;
    void onRcsConfigurationSet(in Status status) = 5;
    void onRcsEnabledStatus(in Status status, boolean enabled) = 6;
    void onRcsMessageSent(in Status status, in RcsMessageResponse response) = 7;
    void onRcsStatusUpdated(in Status status, boolean enabled) = 8;
}