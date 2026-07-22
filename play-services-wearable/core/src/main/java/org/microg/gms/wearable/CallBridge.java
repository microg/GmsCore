/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.ref.WeakReference;

/**
 * Bridges phone call state between the Android phone and connected Wear OS peers.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Registers a {@link PhoneStateListener} to monitor ringing / off-hook / idle state.
 *   <li>Sends call-state updates to all connected watch nodes via
 *       {@link WearableImpl#sendMessage} on path {@value #PHONE_PATH}.
 *   <li>Handles call-control commands arriving from the watch on path
 *       {@value #PHONE_COMMAND_PATH}: answer, reject/end, and silence-ringer.
 * </ul>
 *
 * <h3>Phone-state payload format (path {@value #PHONE_PATH})</h3>
 * <pre>
 *   byte  type          0 = idle, 1 = ringing, 2 = off-hook
 *   UTF   phoneNumber   (empty string when unavailable)
 *   UTF   contactName   (empty string when unavailable)
 * </pre>
 *
 * <h3>Command payload format (path {@value #PHONE_COMMAND_PATH})</h3>
 * <pre>
 *   byte  command       1 = answer, 2 = reject/end, 3 = silence ringer
 * </pre>
 */
public class CallBridge {

    private static final String TAG = "GmsWearCallBridge";

    /** Path used to push phone-state updates to connected wearable peers. */
    public static final String PHONE_PATH = "/wearable/phone";

    /** Path on which the wearable peer sends call-control commands. */
    public static final String PHONE_COMMAND_PATH = "/wearable/phone/command";

    // Phone-state type constants (sent in the payload to the watch)
    private static final byte STATE_IDLE     = 0;
    private static final byte STATE_RINGING  = 1;
    private static final byte STATE_OFFHOOK  = 2;

    // Command constants (received from the watch)
    private static final byte CMD_ANSWER  = 1;
    private static final byte CMD_END     = 2;
    private static final byte CMD_SILENCE = 3;

    private static PhoneStateListener sPhoneStateListener;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Registers a {@link PhoneStateListener} and begins forwarding call-state changes
     * to all connected wearable nodes.
     *
     * <p>Safe to call multiple times; subsequent calls replace the previous listener.
     */
    public static synchronized void start(Context context, WearableImpl wearable) {
        stop(context);
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            Log.w(TAG, "TelephonyManager unavailable, call monitoring disabled");
            return;
        }
        sPhoneStateListener = new WearPhoneStateListener(context, wearable);
        tm.listen(sPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        Log.d(TAG, "call state listener registered");
    }

    /**
     * Unregisters the previously registered {@link PhoneStateListener}.
     */
    public static synchronized void stop(Context context) {
        if (sPhoneStateListener == null) return;
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            tm.listen(sPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        sPhoneStateListener = null;
        Log.d(TAG, "call state listener unregistered");
    }

    // -------------------------------------------------------------------------
    // Call-control actions (invoked by WearableServiceImpl or command handler)
    // -------------------------------------------------------------------------

    /**
     * Answers the current ringing call using {@link TelecomManager}.
     * Requires {@code android.permission.ANSWER_PHONE_CALLS} on API 26+.
     */
    @SuppressLint("MissingPermission")
    public static void answerCall(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (tm != null) {
                try {
                    tm.acceptRingingCall();
                    Log.d(TAG, "acceptRingingCall() called");
                } catch (SecurityException e) {
                    Log.w(TAG, "Missing ANSWER_PHONE_CALLS permission", e);
                }
            }
        } else {
            Log.d(TAG, "answerCall: requires API 26+, ignoring");
        }
    }

    /**
     * Ends the current call (or rejects an incoming one) using {@link TelecomManager}.
     * Requires {@code android.permission.ANSWER_PHONE_CALLS} on API 28+ for
     * ending an active call.
     */
    @SuppressLint("MissingPermission")
    public static void endCall(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (tm != null) {
                try {
                    tm.endCall();
                    Log.d(TAG, "endCall() called");
                } catch (SecurityException e) {
                    Log.w(TAG, "Missing ANSWER_PHONE_CALLS permission for endCall", e);
                }
            }
        } else {
            Log.d(TAG, "endCall: requires API 28+, ignoring");
        }
    }

    /**
     * Silences the ringer for the current incoming call without rejecting it.
     * Uses {@link TelecomManager#silenceRinger()} on API 23+.
     */
    @SuppressLint("MissingPermission")
    public static void silenceRinger(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (tm != null) {
                try {
                    tm.silenceRinger();
                    Log.d(TAG, "silenceRinger() called");
                } catch (SecurityException e) {
                    Log.w(TAG, "Missing permission for silenceRinger", e);
                }
            }
        } else {
            Log.d(TAG, "silenceRinger: requires API 23+, ignoring");
        }
    }

    // -------------------------------------------------------------------------
    // Incoming command handling
    // -------------------------------------------------------------------------

    /**
     * Dispatches a call-control command received from the watch.
     *
     * @param context application context
     * @param data    raw payload bytes from the watch message
     */
    public static void handleCommand(Context context, byte[] data) {
        if (data == null || data.length == 0) {
            Log.w(TAG, "handleCommand: empty payload");
            return;
        }
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            byte command = dis.readByte();
            Log.d(TAG, "handleCommand: command=" + command);
            switch (command) {
                case CMD_ANSWER:
                    answerCall(context);
                    break;
                case CMD_END:
                    endCall(context);
                    break;
                case CMD_SILENCE:
                    silenceRinger(context);
                    break;
                default:
                    Log.w(TAG, "handleCommand: unknown command=" + command);
            }
        } catch (Exception e) {
            Log.e(TAG, "handleCommand: failed to parse payload", e);
        }
    }

    // -------------------------------------------------------------------------
    // Encoding helpers
    // -------------------------------------------------------------------------

    /**
     * Encodes a phone-state update payload to send to the watch.
     *
     * @param stateType   one of {@link #STATE_IDLE}, {@link #STATE_RINGING}, or
     *                    {@link #STATE_OFFHOOK}
     * @param phoneNumber incoming phone number, or empty string
     * @param contactName resolved contact name, or empty string
     * @return encoded bytes or {@code null} on error
     */
    static byte[] encodeState(byte stateType, String phoneNumber, String contactName) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(stateType);
            dos.writeUTF(phoneNumber != null ? phoneNumber : "");
            dos.writeUTF(contactName != null ? contactName : "");
            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "encodeState: failed", e);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // PhoneStateListener inner class
    // -------------------------------------------------------------------------

    private static final class WearPhoneStateListener extends PhoneStateListener {
        private final Context context;
        // WeakReference to avoid preventing WearableImpl GC
        private final WeakReference<WearableImpl> wearableRef;

        WearPhoneStateListener(Context context, WearableImpl wearable) {
            this.context = context.getApplicationContext();
            this.wearableRef = new WeakReference<>(wearable);
        }

        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            WearableImpl wearable = wearableRef.get();
            if (wearable == null) return;

            byte type;
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    type = STATE_RINGING;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    type = STATE_OFFHOOK;
                    break;
                default:
                    type = STATE_IDLE;
                    break;
            }

            Log.d(TAG, "onCallStateChanged: type=" + type + ", number=" + phoneNumber);
            byte[] payload = encodeState(type, phoneNumber, "");
            if (payload == null) return;

            for (String nodeId : wearable.getAllConnectedNodes()) {
                int result = wearable.sendMessage(context.getPackageName(), nodeId, PHONE_PATH, payload);
                if (result < 0) {
                    Log.w(TAG, "sendMessage to " + nodeId + " failed (result=" + result + ")");
                }
            }
        }
    }
}
