package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ConsentStatusRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public final String unk;

    public ConsentStatusRequest(String unk) {
        this.unk = unk;
    }

    public static final Creator<ConsentStatusRequest> CREATOR = new AutoCreator<ConsentStatusRequest>(ConsentStatusRequest.class);
}
