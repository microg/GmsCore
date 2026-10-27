package com.google.android.gms.nearby.sharing.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.sharing.ShareTarget;

interface INearbySharingCallback {
    void onStatus(in Status status) = 1;
    void onShareTargetDiscovered(in ShareTarget target) = 2;
    void onShareTargetLost(in ShareTarget target) = 3;
    void onTransferUpdate(in ShareTarget target, in String attachmentId, int status, long bytesTransferred, long totalBytes) = 4;
}
