/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.wearable.internal.MessageEventParcelable;

import java.util.List;

/**
 * Handles media and call RPCs coming from a paired watch, and exposes the same actions to
 * {@link WearableServiceImpl} for the first-party Wearable API.
 */
public final class WearableRemoteControls {
    private static final String TAG = "GmsWearCtrl";

    public static final String PATH_MEDIA_PLAY = "/media/play";
    public static final String PATH_MEDIA_PAUSE = "/media/pause";
    public static final String PATH_MEDIA_NEXT = "/media/next";
    public static final String PATH_MEDIA_PREVIOUS = "/media/previous";
    public static final String PATH_MEDIA_STOP = "/media/stop";
    public static final String PATH_CALL_END = "/call/end";
    public static final String PATH_CALL_ANSWER = "/call/answer";
    public static final String PATH_CALL_SILENCE = "/call/silence";

    private WearableRemoteControls() {
    }

    public static boolean handleIncoming(Context context, MessageEventParcelable event) {
        if (event == null || event.path == null) {
            return false;
        }
        switch (event.path) {
            case PATH_MEDIA_PLAY:
            case PATH_MEDIA_PAUSE:
            case PATH_MEDIA_NEXT:
            case PATH_MEDIA_PREVIOUS:
            case PATH_MEDIA_STOP:
                if (!WearablePreferences.isMediaControlEnabled(context)) {
                    return true;
                }
                break;
            case PATH_CALL_END:
            case PATH_CALL_ANSWER:
            case PATH_CALL_SILENCE:
                if (!WearablePreferences.isCallControlEnabled(context)) {
                    return true;
                }
                break;
            default:
                return false;
        }
        switch (event.path) {
            case PATH_MEDIA_PLAY:
                dispatchMedia(context, Action.PLAY);
                return true;
            case PATH_MEDIA_PAUSE:
                dispatchMedia(context, Action.PAUSE);
                return true;
            case PATH_MEDIA_NEXT:
                dispatchMedia(context, Action.NEXT);
                return true;
            case PATH_MEDIA_PREVIOUS:
                dispatchMedia(context, Action.PREVIOUS);
                return true;
            case PATH_MEDIA_STOP:
                dispatchMedia(context, Action.STOP);
                return true;
            case PATH_CALL_END:
                endCall(context);
                return true;
            case PATH_CALL_ANSWER:
                acceptRingingCall(context);
                return true;
            case PATH_CALL_SILENCE:
                silenceRinger(context);
                return true;
            default:
                return false;
        }
    }

    public static void dispatchMedia(Context context, Action action) {
        if (Build.VERSION.SDK_INT < 21) {
            return;
        }
        MediaSessionManager manager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (manager == null) {
            return;
        }
        ComponentName listener = new ComponentName(context, WearableNotificationListenerService.class);
        List<MediaController> sessions;
        try {
            sessions = manager.getActiveSessions(listener);
        } catch (SecurityException e) {
            Log.w(TAG, "No notification listener access for media sessions", e);
            return;
        }
        if (sessions == null || sessions.isEmpty()) {
            Log.d(TAG, "No active media session for " + action);
            return;
        }
        MediaController controller = sessions.get(0);
        switch (action) {
            case PLAY:
                controller.getTransportControls().play();
                break;
            case PAUSE:
                controller.getTransportControls().pause();
                break;
            case NEXT:
                controller.getTransportControls().skipToNext();
                break;
            case PREVIOUS:
                controller.getTransportControls().skipToPrevious();
                break;
            case STOP:
                controller.getTransportControls().stop();
                break;
        }
    }

    public static void endCall(Context context) {
        if (Build.VERSION.SDK_INT >= 28) {
            TelecomManager telecom = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (telecom != null) {
                try {
                    telecom.endCall();
                    return;
                } catch (SecurityException e) {
                    Log.w(TAG, "endCall denied", e);
                }
            }
        }
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony != null) {
            try {
                //noinspection JavaReflectionMemberAccess
                TelephonyManager.class.getMethod("endCall").invoke(telephony);
            } catch (Exception e) {
                Log.w(TAG, "TelephonyManager.endCall failed", e);
            }
        }
    }

    public static void acceptRingingCall(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            TelecomManager telecom = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (telecom != null) {
                try {
                    telecom.acceptRingingCall();
                    return;
                } catch (SecurityException e) {
                    Log.w(TAG, "acceptRingingCall denied", e);
                }
            }
        }
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony != null) {
            try {
                //noinspection JavaReflectionMemberAccess
                TelephonyManager.class.getMethod("answerRingingCall").invoke(telephony);
            } catch (Exception e) {
                Log.w(TAG, "TelephonyManager.answerRingingCall failed", e);
            }
        }
    }

    public static void silenceRinger(Context context) {
        if (Build.VERSION.SDK_INT >= 28) {
            TelecomManager telecom = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (telecom != null) {
                try {
                    telecom.silenceRinger();
                    return;
                } catch (SecurityException e) {
                    Log.w(TAG, "silenceRinger denied", e);
                }
            }
        }
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio != null) {
            audio.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
        }
    }

    public enum Action {
        PLAY, PAUSE, NEXT, PREVIOUS, STOP
    }
}
