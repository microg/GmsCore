/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.os.RemoteException;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.internal.AmsEntityUpdateParcelable;
import com.google.android.gms.wearable.internal.AncsNotificationParcelable;
import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.ChannelEventParcelable;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.MessageEventParcelable;
import com.google.android.gms.wearable.internal.NodeParcelable;

import java.util.List;

/**
 * No-op {@link IWearableListener} stub for client API wrappers that only care about a subset
 * of wearable events.
 */
public class BaseWearableListener extends IWearableListener.Stub {
    @Override
    public void onDataChanged(DataHolder data) throws RemoteException {
    }

    @Override
    public void onMessageReceived(MessageEventParcelable messageEvent) throws RemoteException {
    }

    @Override
    public void onPeerConnected(NodeParcelable node) throws RemoteException {
    }

    @Override
    public void onPeerDisconnected(NodeParcelable node) throws RemoteException {
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
}
