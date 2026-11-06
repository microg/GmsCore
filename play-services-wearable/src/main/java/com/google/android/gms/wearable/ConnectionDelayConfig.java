package com.google.android.gms.wearable;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ConnectionDelayConfig extends AutoSafeParcelable {
    @SafeParceled(1)
    public long unk1;
    @SafeParceled(2)
    public long unk2;

    public ConnectionDelayConfig() {
    }

    public ConnectionDelayConfig(long unk1, long unk2) {
        this.unk1 = unk1;
        this.unk2 = unk2;
    }

    public static final Creator<ConnectionDelayConfig> CREATOR = new AutoCreator<>(ConnectionDelayConfig.class);
}