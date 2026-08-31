/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.content.IntentFilter;
import android.os.RemoteException;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.AmsEntityUpdateParcelable;
import com.google.android.gms.wearable.internal.AncsNotificationParcelable;
import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.NodeParcelable;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;

import java.util.List;

/**
 * Forwards wearable service callbacks to the client-side Node/Data/Message listeners.
 */
public class WearableListenerStub extends IWearableListener.Stub {
    private final MessageApi.MessageListener messageListener;
    private final NodeApi.NodeListener nodeListener;
    private final DataApi.DataListener dataListener;

    public WearableListenerStub(MessageApi.MessageListener messageListener) {
        this(messageListener, null, null);
    }

    public WearableListenerStub(NodeApi.NodeListener nodeListener) {
        this(null, nodeListener, null);
    }

    public WearableListenerStub(DataApi.DataListener dataListener) {
        this(null, null, dataListener);
    }

    private WearableListenerStub(MessageApi.MessageListener messageListener, NodeApi.NodeListener nodeListener, DataApi.DataListener dataListener) {
        this.messageListener = messageListener;
        this.nodeListener = nodeListener;
        this.dataListener = dataListener;
    }

    public AddListenerRequest toAddRequest() {
        return new AddListenerRequest(this, new IntentFilter[0], null);
    }

    public RemoveListenerRequest toRemoveRequest() {
        return new RemoveListenerRequest(this);
    }

    @Override
    public void onDataChanged(DataHolder data) throws RemoteException {
        if (dataListener != null && data != null) {
            dataListener.onDataChanged(new DataEventBuffer(data));
        }
    }

    @Override
    public void onMessageReceived(MessageEventParcelable messageEvent) throws RemoteException {
        if (messageListener != null && messageEvent != null) {
            messageListener.onMessageReceived(messageEvent);
        }
    }

    @Override
    public void onPeerConnected(NodeParcelable node) throws RemoteException {
        if (nodeListener != null && node != null) {
            nodeListener.onPeerConnected(node);
        }
    }

    @Override
    public void onPeerDisconnected(NodeParcelable node) throws RemoteException {
        if (nodeListener != null && node != null) {
            nodeListener.onPeerDisconnected(node);
        }
    }

    @Override
    public void onConnectedNodes(List<NodeParcelable> nodes) throws RemoteException {
    }

    @Override
    public void onNotificationReceived(AncsNotificationParcelable notification) throws RemoteException {
    }

    @Override
    public void onChannelEvent(ChannelEventParcelable channelEvent) throws RemoteException {
    }

    @Override
    public void onConnectedCapabilityChanged(CapabilityInfoParcelable capabilityInfo) throws RemoteException {
    }

    @Override
    public void onEntityUpdate(AmsEntityUpdateParcelable update) throws RemoteException {
    }

    public static BaseWearableCallbacks statusCallback(final org.microg.gms.common.GmsConnector.Callback.ResultProvider<Status> resultProvider) {
        return new BaseWearableCallbacks() {
            @Override
            public void onStatus(Status status) {
                resultProvider.onResultAvailable(status);
            }
        };
    }
}
