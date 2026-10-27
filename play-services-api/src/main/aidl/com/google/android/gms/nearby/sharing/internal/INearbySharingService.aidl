package com.google.android.gms.nearby.sharing.internal;

import com.google.android.gms.nearby.sharing.ShareTarget;
import com.google.android.gms.nearby.sharing.Attachment;
import com.google.android.gms.nearby.sharing.internal.INearbySharingCallback;

interface INearbySharingService {
    void registerSendSurface(in INearbySharingCallback callback, int surfaceType) = 1;
    void unregisterSendSurface(in INearbySharingCallback callback) = 2;
    void registerReceiveSurface(in INearbySharingCallback callback, int surfaceType) = 3;
    void unregisterReceiveSurface(in INearbySharingCallback callback) = 4;
    void send(in ShareTarget target, in List<Attachment> attachments, in INearbySharingCallback callback) = 5;
    void accept(in ShareTarget target, in INearbySharingCallback callback) = 6;
    void reject(in ShareTarget target, in INearbySharingCallback callback) = 7;
    void cancel(in ShareTarget target, in INearbySharingCallback callback) = 8;
    void setVisibility(int visibility, in INearbySharingCallback callback) = 9;
    void setDeviceName(in String name, in INearbySharingCallback callback) = 10;
}
