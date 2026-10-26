package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class GetRestoreStateRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public String nodeId;

    private GetRestoreStateRequest() {
    }

    public GetRestoreStateRequest(String nodeId) {
        this.nodeId = nodeId;
    }

    public static final Creator<GetRestoreStateRequest> CREATOR =
            new AutoCreator<>(GetRestoreStateRequest.class);
}
