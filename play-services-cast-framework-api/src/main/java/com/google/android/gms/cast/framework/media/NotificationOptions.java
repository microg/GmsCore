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

    @Field(1)
    private int versionCode = 1;
    @Field(2)
    public List<String> actions;
    @Field(3)
    public int[] compatActionIndices;
    @Field(4)
    public long skipStepMs;
    @Field(5)
    public String targetActivityClassName;
    @Field(6)
    public int getSmallIconDrawableResId;
    @Field(7)
    public int getStopLiveStreamDrawableResId;
    @Field(8)
    public int getPauseDrawableResId;
    @Field(9)
    public int getPlayDrawableResId;
    @Field(10)
    public int getSkipNextDrawableResId;
    @Field(11)
    public int getSkipPrevDrawableResId;
    @Field(12)
    public int getForwardDrawableResId;
    @Field(13)
    public int getForward10DrawableResId;
    @Field(14)
    public int getForward30DrawableResId;
    @Field(15)
    public int getRewindDrawableResId;
    @Field(16)
    public int getRewind10DrawableResId;
    @Field(17)
    public int getRewind30DrawableResId;
    @Field(18)
    public int getDisconnectDrawableResId;
    @Field(19)
    public int intvar19;
    @Field(20)
    public int getCastingToDeviceStringResId;
    @Field(21)
    public int getStopLiveStreamTitleResId;
    @Field(22)
    public int intvar22;
    @Field(23)
    public int intvar23;
    @Field(24)
    public int intvar24;
    @Field(25)
    public int intvar25;
    @Field(26)
    public int intvar26;
    @Field(27)
    public int intvar27;
    @Field(28)
    public int intvar28;
    @Field(29)
    public int intvar29;
    @Field(30)
    public int intvar30;
    @Field(31)
    public int intvar31;
    @Field(32)
    public int intvar32;
    @Field(33)
    public INotificationActionsProvider notificationActionsProvider;

    public static Creator<NotificationOptions> CREATOR = new AutoCreator<NotificationOptions>(NotificationOptions.class);
}
