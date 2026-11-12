package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class AddSupervisedAccountRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public String unk1;
    @SafeParceled(2)
    public String unk2;
    @SafeParceled(3)
    public boolean unk3;

    public AddSupervisedAccountRequest() {
    }

    public AddSupervisedAccountRequest(String unk1, String unk2, boolean unk3) {
        this.unk1 = unk1;
        this.unk2 = unk2;
        this.unk3 = unk3;
    }

    public static final Creator<AddSupervisedAccountRequest> CREATOR = new AutoCreator<>(AddSupervisedAccountRequest.class);
}

