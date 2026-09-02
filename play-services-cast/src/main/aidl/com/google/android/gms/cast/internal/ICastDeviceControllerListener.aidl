/*
 * Copyright (C) 2013-2026 microG Project Team
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

package com.google.android.gms.cast.internal;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.internal.ApplicationStatusDetails;
import com.google.android.gms.cast.internal.DeviceStatusDetails;

interface ICastDeviceControllerListener {
    void onApplicationStatusChanged(in ApplicationStatusDetails applicationStatusDetails) = 0;
    void onDeviceStatusChanged(in DeviceStatusDetails deviceStatusDetails) = 1;
    void onApplicationDisconnected(int statusCode) = 2;
    void onApplicationConnectionFailed(int statusCode) = 3;
    void onMessageReceived(String namespace, String message) = 4;
    void onConnected(String sessionId) = 12;
}