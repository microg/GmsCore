package org.microg.gms.wearable;

import com.google.android.gms.wearable.MessageEvent;

/**
 * Handler for incoming Wearable messages on a specific path prefix.
 *
 * Used by WearableConnectionManager to route messages to the
 * correct subsystem without multiple independent listeners.
 */
public interface MessageHandler {
    /**
     * Handle an incoming message.
     * Called on the Wearable API callback thread.
     *
     * @param event The message event from the Wearable MessageApi.
     */
    void handleMessage(MessageEvent event);
}
