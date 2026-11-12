package org.microg.gms.wearable.channel;

public interface ChannelCallbacks {
    void onChannelOpened(ChannelToken token, String path);
    void onChannelClosed(ChannelToken token, String path, int closeReason, int errorCode);
    void onChannelInputClosed(ChannelToken token, String path, int closeReason, int errorCode);
    void onChannelOutputClosed(ChannelToken token, String path, int closeReason, int errorCode);
}