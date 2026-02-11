package org.microg.gms.wearable.channel;

public interface OpenChannelCallback {
    void onResult(int statusCode, ChannelToken token, String path);
}