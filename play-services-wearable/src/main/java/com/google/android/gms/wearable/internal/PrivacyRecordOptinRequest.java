package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class PrivacyRecordOptinRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public String packageName;
    @SafeParceled(2)
    public int optInType;
    @SafeParceled(3)
    public boolean optedIn;
    @SafeParceled(4)
    public String nodeId;

    private PrivacyRecordOptinRequest() {
    }

    public PrivacyRecordOptinRequest(String packageName, int optInType,
                                     boolean optedIn, String nodeId) {
        this.packageName = packageName;
        this.optInType = optInType;
        this.optedIn = optedIn;
        this.nodeId = nodeId;
    }

    public static final Creator<PrivacyRecordOptinRequest> CREATOR =
            new AutoCreator<>(PrivacyRecordOptinRequest.class);
}
