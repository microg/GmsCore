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

import com.google.android.gms.rcs.internal.IRcsCallbacks;
import com.google.android.gms.rcs.internal.RcsCapabilitiesRequest;
import com.google.android.gms.rcs.internal.RcsConfigurationRequest;
import com.google.android.gms.rcs.internal.RcsMessageRequest;

interface IRcsService {
    void getRcsCapabilities(IRcsCallbacks callbacks, in RcsCapabilitiesRequest request) = 1;
    void enableRcs(IRcsCallbacks callbacks) = 2;
    void disableRcs(IRcsCallbacks callbacks) = 3;
    void getRcsConfiguration(IRcsCallbacks callbacks, in RcsConfigurationRequest request) = 4;
    void setRcsConfiguration(IRcsCallbacks callbacks, in RcsConfigurationRequest request) = 5;
    void isRcsEnabled(IRcsCallbacks callbacks) = 6;
    void sendRcsMessage(IRcsCallbacks callbacks, in RcsMessageRequest request) = 7;
    void registerForRcsUpdates(IRcsCallbacks callbacks) = 8;
    void unregisterForRcsUpdates(IRcsCallbacks callbacks) = 9;
}