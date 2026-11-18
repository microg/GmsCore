package org.microg.gms.wearable.bluetooth;

import android.os.Message;

public abstract class BleState {
    public abstract String getName();

    public void onEnter() {}

    public void onExit() {}

    public abstract boolean handleMessage(Message msg);
}
