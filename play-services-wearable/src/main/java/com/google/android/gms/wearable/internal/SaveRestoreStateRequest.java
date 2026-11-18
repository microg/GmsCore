package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class SaveRestoreStateRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public String nodeId;
    @SafeParceled(2)
    public int state;
    @SafeParceled(3)
    public byte[] data;

    private SaveRestoreStateRequest() {
    }

    public SaveRestoreStateRequest(String nodeId, int state, byte[] data) {
        this.nodeId = nodeId;
        this.state = state;
        this.data = data;
    }

    public static final Creator<SaveRestoreStateRequest> CREATOR =
            new AutoCreator<>(SaveRestoreStateRequest.class);
}
