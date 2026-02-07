package org.microg.gms.wearable.channel;

import android.util.Log;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelTable {
    private static final String TAG = "ChannelTable";

    private final Map<String, ChannelStateMachine> channels = new ConcurrentHashMap<>();

    public ChannelStateMachine get(String tokenString) {
        return channels.get(tokenString);
    }

    public ChannelStateMachine get(ChannelToken token) {
        return channels.get(token.toTokenString());
    }

    public ChannelStateMachine get(String nodeId, long channelId, boolean isOpener) {
        for (ChannelStateMachine channel : channels.values()) {
            ChannelToken token = channel.token;
            if (token.nodeId.equals(nodeId) &&
                    token.channelId == channelId &&
                    token.thisNodeWasOpener == isOpener) {
                return channel;
            }
        }
        return null;
    }

    public void put(ChannelToken token, ChannelStateMachine channel) {
        String key = token.toTokenString();
        channels.put(key, channel);
        Log.d(TAG, "Added channel to table: " + token + " (total: " + channels.size() + ")");
    }

    public ChannelStateMachine remove(ChannelToken token) {
        String key = token.toTokenString();
        ChannelStateMachine removed = channels.remove(key);
        if (removed != null) {
            Log.d(TAG, "Removed channel from table: " + token + " (remaining: " + channels.size() + ")");
        }
        return removed;
    }

    public Collection<ChannelStateMachine> values() {
        return channels.values();
    }

    public void clear() {
        int count = channels.size();
        channels.clear();
        Log.d(TAG, "Cleared " + count + " channels from table");
    }

    public int size() {
        return channels.size();
    }
}