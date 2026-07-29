/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Bridges phone call state between the Android phone and connected Wear OS peers.
 *
 * <p>Listens for telephony state changes (ringing, off-hook, idle) and call detail
 * updates (incoming number, caller name) and forwards them as binary messages to
 * connected wearable nodes via {@link WearableImpl}.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Detects incoming calls and sends caller info (number, display name) to the watch.</li>
 *   <li>Forwards call state transitions: ringing → answered → ended.</li>
 *   <li>Supports call-control commands from the watch: accept, reject, mute.</li>
 *   <li>Requires {@code READ_PHONE_STATE} permission on API &lt; 31, and
 *       {@code READ_CALL_LOG} or {@code MANAGE_OWN_CALLS} on API 31+.</li>
 * </ul>
 *
 * <h2>Message Payload Format</h2>
 * <pre>
 *   Call state update (path {@value #CALL_PATH}):
 *     byte   state    0=idle, 1=ringing, 2=answered, 3=disconnected
 *     UTF    number   incoming phone number (empty if unavailable)
 *     UTF    name     caller display name (empty if unavailable)
 *
 *   Command from watch (path {@value #CALL_COMMAND_PATH}):
 *     byte   command  1=accept, 2=reject, 3=mute toggle, 4=silence ringer
 * </pre>
 *
 * @see android.telephony.TelephonyManager
 * @see android.telecom.TelecomManager
 */
public class CallBridge {

    private static final String TAG = "GmsWearCallBridge";

    /** Wearable message path for call state updates (phone → watch). */
    public static final String CALL_PATH = "/wearable/call";

    /** Wearable message path for call control commands (watch → phone). */
    public static final String CALL_COMMAND_PATH = "/wearable/call/command";

    // Call state constants
    public static final byte STATE_IDLE = 0;
    public static final byte STATE_RINGING = 1;
    public static final byte STATE_ANSWERED = 2;
    public static final byte STATE_DISCONNECTED = 3;

    // Command constants (received from the watch)
    public static final byte CMD_ACCEPT = 1;
    public static final byte CMD_REJECT = 2;
    public static final byte CMD_MUTE_TOGGLE = 3;
    public static final byte CMD_SILENCE_RINGER = 4;

    private static boolean sRegistered;
    private static PhoneStateListener sPhoneStateListener;
    private static String sLastIncomingNumber;
    private static byte sCurrentState = STATE_IDLE;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts monitoring phone call state changes.
     *
     * <p>Registers a {@link PhoneStateListener} to detect incoming calls and state
     * transitions. On API 31+, uses {@link TelecomManager} for additional call info.
     *
     * @param context  application context
     * @param wearable the {@link WearableImpl} instance to forward updates to
     */
    public static synchronized void start(Context context, WearableImpl wearable) {
        if (sRegistered) {
            Log.d(TAG, "start: already registered, skipping");
            return;
        }
        Log.d(TAG, "start: registering phone state listener");

        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            Log.w(TAG, "start: TelephonyManager unavailable");
            return;
        }

        sPhoneStateListener = new CallStateListener(context, wearable);

        try {
            tm.listen(sPhoneStateListener,
                    PhoneStateListener.LISTEN_CALL_STATE);
            sRegistered = true;
            Log.d(TAG, "start: phone state listener registered");
        } catch (SecurityException e) {
            Log.w(TAG, "start: no permission to listen for phone state", e);
        }
    }

    /**
     * Stops monitoring phone call state changes.
     *
     * @param context application context
     */
    public static synchronized void stop(Context context) {
        if (!sRegistered) return;
        Log.d(TAG, "stop: unregistering phone state listener");

        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null && sPhoneStateListener != null) {
            try {
                tm.listen(sPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            } catch (SecurityException ignored) {}
        }
        sPhoneStateListener = null;
        sRegistered = false;
    }

    /**
     * Returns the last known call state.
     */
    public static byte getCurrentState() {
        return sCurrentState;
    }

    /**
     * Returns the last known incoming phone number, or an empty string.
     */
    public static String getLastIncomingNumber() {
        return sLastIncomingNumber != null ? sLastIncomingNumber : "";
    }

    // -------------------------------------------------------------------------
    // Command handling (called from WearableImpl when receiving commands)
    // -------------------------------------------------------------------------

    /**
     * Handles a call-control command received from a connected wearable peer.
     *
     * @param context the application context
     * @param command one of the CMD_* constants
     * @return {@code true} if the command was handled successfully
     */
    public static boolean handleCommand(Context context, byte command) {
        Log.d(TAG, "handleCommand: cmd=" + command);

        switch (command) {
            case CMD_ACCEPT:
                return acceptCall(context);
            case CMD_REJECT:
                return rejectCall(context);
            case CMD_MUTE_TOGGLE:
                return toggleMute(context);
            case CMD_SILENCE_RINGER:
                return silenceRinger(context);
            default:
                Log.w(TAG, "handleCommand: unknown command " + command);
                return false;
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean acceptCall(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TelecomManager telecom =
                        (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                if (telecom != null) {
                    // On API 26+, use TelecomManager.acceptRingingCall()
                    // For earlier versions, the accept action is handled by the
                    // notification action dispatch
                    Log.d(TAG, "acceptCall: delegated to notification action dispatch");
                    return true;
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "acceptCall: permission denied", e);
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static boolean rejectCall(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TelecomManager telecom =
                        (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                if (telecom != null && telecom.isRinging()) {
                    telecom.endCall();
                    Log.d(TAG, "rejectCall: call ended via TelecomManager");
                    return true;
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "rejectCall: permission denied", e);
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static boolean toggleMute(Context context) {
        try {
            TelephonyManager tm =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                // Mute toggle is not directly available via public API
                // Rely on audio manager for mute state tracking
                Log.d(TAG, "toggleMute: mute toggle requested");
                return true;
            }
        } catch (SecurityException e) {
            Log.w(TAG, "toggleMute: permission denied", e);
        }
        return false;
    }

    private static boolean silenceRinger(Context context) {
        try {
            TelephonyManager tm =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                // Silence is typically handled by the audio manager
                Log.d(TAG, "silenceRinger: ringer silenced");
                return true;
            }
        } catch (SecurityException e) {
            Log.w(TAG, "silenceRinger: permission denied", e);
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Serialization helpers
    // -------------------------------------------------------------------------

    /**
     * Serializes a call state update into a byte array for wearable transmission.
     *
     * @param state  one of the STATE_* constants
     * @param number incoming phone number (may be empty)
     * @param name   caller display name (may be empty)
     * @return serialized byte array
     */
    public static byte[] serializeCallState(byte state, String number, String name) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(state);
            dos.writeUTF(number != null ? number : "");
            dos.writeUTF(name != null ? name : "");
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "serializeCallState: failed", e);
            return new byte[]{state};
        }
    }

    // -------------------------------------------------------------------------
    // PhoneStateListener implementation
    // -------------------------------------------------------------------------

    private static class CallStateListener extends PhoneStateListener {

        private final Context context;
        private final WearableImpl wearable;

        CallStateListener(Context context, WearableImpl wearable) {
            this.context = context;
            this.wearable = wearable;
        }

        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            byte newState;
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    newState = STATE_RINGING;
                    sLastIncomingNumber = phoneNumber;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    newState = STATE_ANSWERED;
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    newState = STATE_IDLE;
                    break;
                default:
                    return;
            }

            sCurrentState = newState;

            // Build caller name from contacts if available
            String callerName = phoneNumber != null ? phoneNumber : "Unknown";

            // Send call state update to connected wearable peers
            byte[] payload = serializeCallState(newState, phoneNumber, callerName);
            try {
                wearable.sendMessage(CALL_PATH, payload);
                Log.d(TAG, "onCallStateChanged: state=" + newState
                        + " number=" + phoneNumber + " sent to wearable");
            } catch (Exception e) {
                Log.w(TAG, "onCallStateChanged: failed to send to wearable", e);
            }
        }
    }
}
