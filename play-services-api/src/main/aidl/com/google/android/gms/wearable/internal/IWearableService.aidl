package com.google.android.gms.wearable.internal;

import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.ConnectionConfiguration;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.IWearableService;

interface IWearableService {
    void getConfig(IWearableCallbacks callbacks) = 2;
    void putData(IWearableCallbacks callbacks, in PutDataRequest request) = 5;
    void getDataItem(IWearableCallbacks callbacks, in Uri uri) = 6;
    void getDataItems(IWearableCallbacks callbacks) = 7;
    void sendMessage(IWearableCallbacks callbacks, String targetNodeId, String path, in byte[] data) = 11;
    void getLocalNode(IWearableCallbacks callbacks) = 13;
    void getConnectedNodes(IWearableCallbacks callbacks) = 14;
    void addListener(IWearableCallbacks callbacks, in AddListenerRequest request) = 15;
    void removeListener(IWearableCallbacks callbacks, in RemoveListenerRequest request) = 16;
    void putConfig(IWearableCallbacks callbacks, in ConnectionConfiguration config) = 19;
    void deleteConfig(IWearableCallbacks callbacks, String name) = 20;
    void getConfigs(IWearableCallbacks callbacks) = 21;
    void enableConnection(IWearableCallbacks callbacks, String name) = 22;
    void disableConnection(IWearableCallbacks callbacks, String name) = 23;
    void getDataItemsByUri(IWearableCallbacks callbacks, in Uri uri, int i) = 39;
    void deleteDataItems(IWearableCallbacks callbacks, in Uri uri) = 40;
    void optInCloudSync(IWearableCallbacks callbacks, boolean enable) = 47;
}
