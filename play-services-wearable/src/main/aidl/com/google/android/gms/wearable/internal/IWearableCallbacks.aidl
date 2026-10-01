package com.google.android.gms.wearable.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.internal.DataItemParcelable;
import com.google.android.gms.wearable.internal.NodeParcelable;
import com.google.android.gms.wearable.internal.PutDataResponse;
import com.google.android.gms.wearable.internal.SendMessageResponse;

interface IWearableCallbacks {
    void onPutDataResponse(in PutDataResponse response);
    void onSendMessageResponse(in SendMessageResponse response);
    void onGetLocalNodeResponse(in NodeParcelable node, in Status status);
    void onGetConnectedNodesResponse(in List<NodeParcelable> nodes);
    void onStatus(in Status status);
}
