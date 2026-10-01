package com.google.android.gms.wearable.internal;

import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;
import com.google.android.gms.wearable.internal.IWearableCallbacks;

interface IWearableService {
    void putData(IWearableCallbacks callbacks, in PutDataRequest request);
    void sendMessage(IWearableCallbacks callbacks, String nodeId, String path, in byte[] data);
    void getLocalNode(IWearableCallbacks callbacks);
    void getConnectedNodes(IWearableCallbacks callbacks);
    void addListener(IWearableCallbacks callbacks, in AddListenerRequest request);
    void removeListener(IWearableCallbacks callbacks, in RemoveListenerRequest request);
}
