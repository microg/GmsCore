/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cast.framework.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.SessionManager;

public class MediaIntentReceiver extends BroadcastReceiver {
    /**
     * The action for ending the current session and disconnecting from the receiver app.
     */
    public static final String ACTION_DISCONNECT = "com.google.android.gms.cast.framework.action.DISCONNECT";
    /**
     * The action for forwarding the current item. When building an Intent with this action, use {@link #EXTRA_SKIP_STEP_MS} to set the
     * time to forward in milliseconds.
     */
    public static final String ACTION_FORWARD = "com.google.android.gms.cast.framework.action.FORWARD";
    /**
     * The action for rewinding the current item. When building an Intent with this action, use {@link #EXTRA_SKIP_STEP_MS} to set the
     * time to rewind in milliseconds.
     */
    public static final String ACTION_REWIND = "com.google.android.gms.cast.framework.action.REWIND";
    /**
     * The action for skipping to the next item in the queue.
     */
    public static final String ACTION_SKIP_NEXT = "com.google.android.gms.cast.framework.action.SKIP_NEXT";
    /**
     * The action for skipping to the previous item in the queue.
     */
    public static final String ACTION_SKIP_PREV = "com.google.android.gms.cast.framework.action.SKIP_PREV";
    /**
     * The action for ending the current session and stopping the receiver app.
     */
    public static final String ACTION_STOP_CASTING = "com.google.android.gms.cast.framework.action.STOP_CASTING";
    /**
     * The action for toggling remote media playback.
     */
    public static final String ACTION_TOGGLE_PLAYBACK = "com.google.android.gms.cast.framework.action.TOGGLE_PLAYBACK";
    /**
     * The extra key for specifying how much the currently playing item should be forwarded or rewinded to handle
     * {@link #ACTION_FORWARD} and {@link #ACTION_REWIND}.
     */
    public static final String EXTRA_SKIP_STEP_MS = "googlecast-extra_skip_step_ms";

    @Override
    public void onReceive(Context context, Intent intent) {
        SessionManager sessionManager = CastContext.getSharedInstance(context).getSessionManager();
        Session currentSession = sessionManager.getCurrentSession();
        if (intent.getAction() != null && currentSession != null) {
            switch (intent.getAction()) {
                case ACTION_TOGGLE_PLAYBACK:
                    onReceiveActionTogglePlayback(currentSession);
                    break;
                case ACTION_SKIP_NEXT:
                    onReceiveActionSkipNext(currentSession);
                    break;
                case ACTION_SKIP_PREV:
                    onReceiveActionSkipPrev(currentSession);
                    break;
                case ACTION_FORWARD:
                    onReceiveActionForward(currentSession, intent.getLongExtra(EXTRA_SKIP_STEP_MS, 0));
                    break;
                case ACTION_REWIND:
                    onReceiveActionRewind(currentSession, intent.getLongExtra(EXTRA_SKIP_STEP_MS, 0));
                    break;
                case ACTION_STOP_CASTING:
                    sessionManager.endCurrentSession(true);
                    break;
                case ACTION_DISCONNECT:
                    sessionManager.endCurrentSession(false);
                    break;
                case Intent.ACTION_MEDIA_BUTTON:
                    onReceiveActionMediaButton(currentSession, intent);
                    break;
                default:
                    onReceiveOtherAction(context, intent.getAction(), intent);
                    break;
            }
        }
    }

    /**
     * Called when {@link #ACTION_FORWARD} is received. The default implementation forwards the current playing item by
     * {@code forwardStepMs} if {@code currentSession} is a {@link CastSession}. Subclasses can override this method to change the behavior
     * or handle other type of {@link Session}. Subclasses should call through to super to let the SDK handle the action if
     * {@code currentSession} is a {@link CastSession}
     *
     * @param currentSession The current {@link Session}.
     * @param forwardStepMs  Time to forward in milliseconds.
     */
    protected void onReceiveActionForward(Session currentSession, long forwardStepMs) {
        if (!(currentSession instanceof CastSession)) return;
        // TODO Seek forwardStepMs
    }

    /**
     * Called when {@link Intent#ACTION_MEDIA_BUTTON} is received. The default implementation toggles playback state if
     * {@code currentSession} is a {@link CastSession}. Subclasses can override this method to change the behavior or handle other type
     * of {@link Session}. Subclasses should call through to super to let the SDK handle the action if {@code currentSession} is a
     * {@link CastSession}
     *
     * @param currentSession The current {@link Session}.
     * @param intent         The Intent of this action.
     */
    protected void onReceiveActionMediaButton(Session currentSession, Intent intent) {
        if (!(currentSession instanceof CastSession)) return;
        if (intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                // TODO Toggle Playback
            }
        }
    }

    /**
     * Called when {@link #ACTION_REWIND} is received. The default implementation forwards the current playing item by
     * {@code rewindStepMs} if {@code currentSession} is a {@link CastSession}. Subclasses can override this method to change the behavior or
     * handle other type of {@link Session}. Subclasses should call through to super to let the SDK handle the action if
     * {@code currentSession} is a {@link CastSession}
     *
     * @param currentSession The current {@link Session}.
     * @param rewindStepMs   Time to rewind in milliseconds.
     */
    protected void onReceiveActionRewind(Session currentSession, long rewindStepMs) {
        if (!(currentSession instanceof CastSession)) return;
        // TODO Seek -rewindStepMs
    }

    /**
     * Called when {@link #ACTION_SKIP_NEXT} is received. The default implementation plays the next item in the queue if
     * {@code currentSession} is a {@link CastSession} and there is a next item. Subclasses can override this method to change the
     * behavior or handle other type of {@link Session}. Subclasses should call through to super to let the SDK handle the action if
     * {@code currentSession} is a {@link CastSession}
     *
     * @param currentSession The current {@link Session}.
     */
    protected void onReceiveActionSkipNext(Session currentSession) {
        if (!(currentSession instanceof CastSession)) return;
        // TODO Queue next
    }

    /**
     * Called when {@link #ACTION_SKIP_PREV} is received. The default implementation plays the previous item in the queue if
     * {@code currentSession} is a {@link CastSession} and there is a previous item. Subclasses can override this method to change the
     * behavior or handle other type of {@link Session}. Subclasses should call through to super to let the SDK handle the action if
     * {@code currentSession} is a {@link CastSession}
     *
     * @param currentSession The current {@link Session}.
     */
    protected void onReceiveActionSkipPrev(Session currentSession) {
        if (!(currentSession instanceof CastSession)) return;
        // TODO Queue prev
    }

    /**
     * Called when {@link #ACTION_TOGGLE_PLAYBACK} is received. The default implementation toggles playback state if
     * {@code currentSession} is a {@link CastSession}. Subclasses can override this method to change the
     * behavior or handle other type of {@link Session}. Subclasses should call through to super to let the SDK handle the action if
     * {@code currentSession} is a {@link CastSession}
     *
     * @param currentSession The current {@link Session}.
     */
    protected void onReceiveActionTogglePlayback(Session currentSession) {
        if (!(currentSession instanceof CastSession)) return;
        // TODO Toggle Playback
    }

    /**
     * @deprecated Override {@link #onReceiveOtherAction(Context, String, Intent)} instead.
     */
    @Deprecated
    protected void onReceiveOtherAction(String action, Intent intent) {
        onReceiveOtherAction(null, action, intent);
    }

    /**
     * Called when other type of actions are received. The default implementation does nothing.
     *
     * @param context The Context in which the receiver is running.
     * @param action  The action.
     * @param intent  The Intent of this action.
     */
    protected void onReceiveOtherAction(Context context, String action, Intent intent) {

    }
}
