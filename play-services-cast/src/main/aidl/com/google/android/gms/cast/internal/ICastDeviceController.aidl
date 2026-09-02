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

import com.google.android.gms.cast.internal.ICastDeviceControllerListener;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.JoinOptions;

interface ICastDeviceController {
    void disconnect() = 0;
    void sendMessage(String namespace, String message, long requestId) = 1;
    void launchApplication(String applicationId, in LaunchOptions launchOptions) = 2;
    void joinApplication(String applicationId, String sessionId, in JoinOptions joinOptions) = 3;
    void stopApplication(String sessionId) = 4;
    void setVolume(double volume, double oldVolume, boolean isMute) = 5;
    void setMute(boolean isMute, double oldVolume, boolean wasMute) = 6;
    void requestStatus() = 7;
    void connect() = 15;
    void addListener(ICastDeviceControllerListener listener) = 16;
}