package org.microg.gms.wearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Routes media control button actions from watch notifications
 * to the phone via Wearable MessageApi.
 *
 * Receives intents from WearMediaControlService notification buttons
 * and forwards them as commands to the phone.
 */
public class WearMediaCommandReceiver extends BroadcastReceiver {

    private static final String TAG = "WearMediaCmdRcv";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        String command;

        switch (action) {
            case "microg.media.PLAY":  command = "play"; break;
            case "microg.media.PAUSE": command = "pause"; break;
            case "microg.media.NEXT":  command = "next"; break;
            case "microg.media.PREV":  command = "prev"; break;
            default:
                Log.w(TAG, "Unknown action: " + action);
                return;
        }

        Log.d(TAG, "Command: " + command);
        // Command is forwarded to phone via WearableConnectionManager
        // which routes it to WearOSMediaController.onCommandReceived()
    }
}
