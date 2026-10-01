package com.google.android.gms.wearable;

import android.os.Parcel;
import android.os.Parcelable;

public class Node implements Parcelable {
    private final String nodePath;
    private final String deviceName;

    public Node(String nodePath, String deviceName) {
        this.nodePath = nodePath;
        this.deviceName = deviceName;
    }

    public String getNodePath() { return nodePath; }
    public String getDeviceName() { return deviceName; }

    protected Node(Parcel in) {
        nodePath = in.readString();
        deviceName = in.readString();
    }

    public static final Creator<Node> CREATOR = new Creator<Node>() {
        @Override
        public Node createFromParcel(Parcel in) { return new Node(in); }
        @Override
        public Node[] newArray(int size) { return new Node[size]; }
    };

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nodePath);
        dest.writeString(deviceName);
    }
}
