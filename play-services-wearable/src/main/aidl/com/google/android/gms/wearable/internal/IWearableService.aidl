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

package com.google.android.gms.wearable.internal;

import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.SendMessageOptions;
import android.net.Uri;

interface IWearableService {
    void putDataItem(IWearableCallbacks callbacks, in PutDataRequest request) = 0;
    void getDataItem(IWearableCallbacks callbacks, in Uri uri) = 1;
    void deleteDataItems(IWearableCallbacks callbacks, in Uri uri) = 2;
    void getLocalNode(IWearableCallbacks callbacks) = 3;
    void getConnectedNodes(IWearableCallbacks callbacks) = 4;
    void sendMessage(IWearableCallbacks callbacks, String nodeId, String path, in byte[] data) = 5;
    void addListener(IWearableCallbacks callbacks, in AddListenerRequest request) = 6;
    void removeListener(IWearableCallbacks callbacks, in RemoveListenerRequest request) = 7;
    void getCompanionPackageForNode(IWearableCallbacks callbacks, String nodeId) = 8;
}