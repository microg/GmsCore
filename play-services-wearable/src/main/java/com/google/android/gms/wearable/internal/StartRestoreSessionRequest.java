package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class StartRestoreSessionRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public String nodeId;

    private StartRestoreSessionRequest() {
    }

    public StartRestoreSessionRequest(String nodeId) {
        this.nodeId = nodeId;
    }

    public static final Creator<StartRestoreSessionRequest> CREATOR =
            new AutoCreator<>(StartRestoreSessionRequest.class);
}
