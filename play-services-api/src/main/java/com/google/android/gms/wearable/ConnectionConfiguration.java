package com.google.android.gms.wearable;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ConnectionConfiguration extends AutoSafeParcelable {

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    public final String name;
    @SafeParceled(3)
    public final String address;
    @SafeParceled(4)
    public final int type;
    @SafeParceled(5)
    public final int role;
    @SafeParceled(6)
    public final boolean enabled;
    @SafeParceled(7)
    public boolean connected = false;
    @SafeParceled(8)
    public String peerNodeId;
    @SafeParceled(9)
    public boolean btlePriority = true;
    @SafeParceled(10)
    public String nodeId;

    private ConnectionConfiguration() {
        name = address = null;
        type = role = 0;
        enabled = false;
    }

    public ConnectionConfiguration(String name, String address, int type, int role, boolean enabled) {
        this.name = name;
        this.address = address;
        this.type = type;
        this.role = role;
        this.enabled = enabled;
    }

    public ConnectionConfiguration(String name, String address, int type, int role, boolean enabled, String nodeId) {
        this.name = name;
        this.address = address;
        this.type = type;
        this.role = role;
        this.enabled = enabled;
        this.nodeId = nodeId;
    }

    public static final Creator<ConnectionConfiguration> CREATOR = new AutoCreator<ConnectionConfiguration>(ConnectionConfiguration.class);
}
