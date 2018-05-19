/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.cast.framework.media;

import java.util.List;

import com.google.android.gms.cast.framework.media.INotificationActionsProvider;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class NotificationOptions extends AutoSafeParcelable {

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    public List<String> actions;
    @SafeParceled(3)
    public int[] compatActionIndices;
    @SafeParceled(4)
    public long skipStepMs;
    @SafeParceled(5)
    public String targetActivityClassName;
    @SafeParceled(6)
    public int getSmallIconDrawableResId;
    @SafeParceled(7)
    public int getStopLiveStreamDrawableResId;
    @SafeParceled(8)
    public int getPauseDrawableResId;
    @SafeParceled(9)
    public int getPlayDrawableResId;
    @SafeParceled(10)
    public int getSkipNextDrawableResId;
    @SafeParceled(11)
    public int getSkipPrevDrawableResId;
    @SafeParceled(12)
    public int getForwardDrawableResId;
    @SafeParceled(13)
    public int getForward10DrawableResId;
    @SafeParceled(14)
    public int getForward30DrawableResId;
    @SafeParceled(15)
    public int getRewindDrawableResId;
    @SafeParceled(16)
    public int getRewind10DrawableResId;
    @SafeParceled(17)
    public int getRewind30DrawableResId;
    @SafeParceled(18)
    public int getDisconnectDrawableResId;
    @SafeParceled(19)
    public int intvar19;
    @SafeParceled(20)
    public int getCastingToDeviceStringResId;
    @SafeParceled(21)
    public int getStopLiveStreamTitleResId;
    @SafeParceled(22)
    public int intvar22;
    @SafeParceled(23)
    public int intvar23;
    @SafeParceled(24)
    public int intvar24;
    @SafeParceled(25)
    public int intvar25;
    @SafeParceled(26)
    public int intvar26;
    @SafeParceled(27)
    public int intvar27;
    @SafeParceled(28)
    public int intvar28;
    @SafeParceled(29)
    public int intvar29;
    @SafeParceled(30)
    public int intvar30;
    @SafeParceled(31)
    public int intvar31;
    @SafeParceled(32)
    public int intvar32;
    @SafeParceled(33)
    public INotificationActionsProvider notificationActionsProvider;

    public static Creator<NotificationOptions> CREATOR = new AutoCreator<NotificationOptions>(NotificationOptions.class);
}
