package org.microg.gms.gcm.WearOS;

import android.util.Log;

import com.google.android.gms.wearable.WearableListenerService;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import android.net.Uri;

public class WearSyncService extends WearableListenerService {

    private static final String TAG = "WearSyncService";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            Uri uri = event.getDataItem().getUri();
            DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

            Log.d(TAG, "Data changed at URI: " + uri);
            Log.d(TAG, "DataMap received: " + dataMap.toString());
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "Message received from: " + messageEvent.getSourceNodeId());
        Log.d(TAG, "Message path: " + messageEvent.getPath());
        Log.d(TAG, "Message data: " + new String(messageEvent.getData()));
    }

    @Override
    public void onPeerConnected(Node peer) {
        Log.d(TAG, "Peer connected: " + peer.getDisplayName() + " (" + peer.getId() + ")");
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(TAG, "Peer disconnected: " + peer.getDisplayName() + " (" + peer.getId() + ")");
    }
}