/*
 * SPDX-FileCopyrightText: 2018 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cast.framework.media;

import java.util.List;

import android.app.Activity;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Configuration parameters for building the media notification. The {@link NotificationOptions.Builder} is used to create an
 * instance of {@link NotificationOptions}, and so contains the corresponding setter methods.
 */
public class NotificationOptions extends AutoSafeParcelable {
    /**
     * Constant for notification skip step, ten seconds in milliseconds.
     */
    public static final long SKIP_STEP_TEN_SECONDS_IN_MS = 10000;
    /**
     * Constant for notification skip step, thirty seconds in milliseconds.
     */
    public static final long SKIP_STEP_THIRTY_SECONDS_IN_MS = 30000;

    @Field(1)
    private int versionCode = 1;
    @Field(2)
    private List<String> actions;
    @Field(3)
    private int[] compatActionIndices;
    @Field(4)
    private long skipStepMs;
    @Field(5)
    private String targetActivityClassName;
    @Field(6)
    private int getSmallIconDrawableResId;
    @Field(7)
    private int getStopLiveStreamDrawableResId;
    @Field(8)
    private int getPauseDrawableResId;
    @Field(9)
    private int getPlayDrawableResId;
    @Field(10)
    private int getSkipNextDrawableResId;
    @Field(11)
    private int getSkipPrevDrawableResId;
    @Field(12)
    private int getForwardDrawableResId;
    @Field(13)
    private int getForward10DrawableResId;
    @Field(14)
    private int getForward30DrawableResId;
    @Field(15)
    private int getRewindDrawableResId;
    @Field(16)
    private int getRewind10DrawableResId;
    @Field(17)
    private int getRewind30DrawableResId;
    @Field(18)
    private int getDisconnectDrawableResId;
    @Field(19)
    private int imageSizeDimenResId;
    @Field(20)
    private int getCastingToDeviceStringResId;
    @Field(21)
    private int getStopLiveStreamTitleResId;
    @Field(22)
    private int pauseTitleResId;
    @Field(23)
    private int playTitleResId;
    @Field(24)
    private int skipNextTitleResId;
    @Field(25)
    private int skipPrevTitleResId;
    @Field(26)
    private int forwardTitleResId;
    @Field(27)
    private int forward10TitleResId;
    @Field(28)
    private int forward30TitleResId;
    @Field(29)
    private int rewindTitleResId;
    @Field(30)
    private int rewind10TitleResId;
    @Field(31)
    private int rewind30TitleResId;
    @Field(32)
    private int disconnectTitleResId;
    @Field(33)
    private INotificationActionsProvider notificationActionsProvider;
    @Field(34)
    private boolean skipToPrevSlotReserved;
    @Field(35)
    private boolean skipToNextSlotReserved;

    /**
     * Returns the list of actions to show in the notification.
     */
    public List<String> getActions() {
        return actions;
    }

    /**
     * Returns the amount to jump if {@link MediaIntentReceiver#ACTION_FORWARD} or {@link MediaIntentReceiver#ACTION_REWIND}
     * are included in the notification actions. Any tap on those actions will result in moving the media position forward or
     * backward by {@code skipStepMs} milliseconds. The default value is {@link #SKIP_STEP_TEN_SECONDS_IN_MS}.
     */
    public long getSkipStepMs() {
        return skipStepMs;
    }

    /**
     * Returns the name of the {@link Activity} that will be launched when user taps on the content area of the notification.
     */
    public String getTargetActivityClassName() {
        return targetActivityClassName;
    }

    public static Creator<NotificationOptions> CREATOR = new AutoCreator<NotificationOptions>(NotificationOptions.class);
}
