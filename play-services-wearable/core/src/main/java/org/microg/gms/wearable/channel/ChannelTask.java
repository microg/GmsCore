package org.microg.gms.wearable.channel;

import android.util.Log;

import java.io.IOException;

public abstract class ChannelTask implements Runnable {
    private static final String TAG = "ChannelTask";

    protected final ChannelManager channelManager;
    protected final boolean requiresRunning;
    protected ChannelStateMachine channel;

    protected ChannelTask(ChannelManager channelManager) {
        this(channelManager, true);
    }

    protected ChannelTask(ChannelManager channelManager, boolean requiresRunning) {
        this.channelManager = channelManager;
        this.requiresRunning = requiresRunning;
    }

    protected abstract void execute() throws IOException, ChannelException;

    @Override
    public final void run() {
        if (requiresRunning && !channelManager.isRunning()) {
            Log.d(TAG, "Skipping task - manager not running");
            return;
        }

        try {
            execute();
        } catch (ChannelException e) {
            Log.w(TAG, "Channel exception in task", e);
            if (channel != null) {
                try {
                    channel.forceClose();
                    channelManager.removeChannel(channel.token);
                } catch (Exception ex) {
                    Log.e(TAG, "Error during cleanup", ex);
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "IO exception in task", e);
            if (channel != null) {
                try {
                    channel.forceClose();
                    channelManager.removeChannel(channel.token);
                } catch (Exception ex) {
                    Log.e(TAG, "Error during cleanup", ex);
                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Uncaught runtime exception in task", e);
            if (channel != null) {
                try {
                    channel.forceClose();
                    channelManager.removeChannel(channel.token);
                } catch (Exception ex) {
                    Log.e(TAG, "Error during cleanup", ex);
                }
            }
        }
    }

    protected void setChannel(ChannelStateMachine channel) {
        this.channel = channel;
    }
}