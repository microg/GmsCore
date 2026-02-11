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

package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class ChannelEventParcelable extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    public ChannelParcelable channel;
    @SafeParceled(3)
    public int eventType;
    @SafeParceled(4)
    public int closeReason;
    @SafeParceled(5)
    public int appSpecificErrorCode;

    public static final int EVENT_TYPE_CHANNEL_OPENED = 1;
    public static final int EVENT_TYPE_CHANNEL_CLOSED = 2;
    public static final int EVENT_TYPE_INPUT_CLOSED = 3;
    public static final int EVENT_TYPE_OUTPUT_CLOSED = 4;

    private ChannelEventParcelable() {}

    public ChannelEventParcelable(ChannelParcelable channel, int eventType, int closeReason, int appSpecificErrorCode) {
        this.channel = channel;
        this.eventType = eventType;
        this.closeReason = closeReason;
        this.appSpecificErrorCode = appSpecificErrorCode;
    }

    public String getTypeString() {
        switch (eventType) {
            case EVENT_TYPE_CHANNEL_OPENED: return "CHANNEL_OPENED";
            case EVENT_TYPE_CHANNEL_CLOSED: return "CHANNEL_CLOSED";
            case EVENT_TYPE_INPUT_CLOSED: return "INPUT_CLOSED";
            case EVENT_TYPE_OUTPUT_CLOSED: return "OUTPUT_CLOSED";
            default: return "UNKNOWN(" + eventType + ")";
        }
    }

    @Override
    public String toString() {
        return "ChannelEventParcelable{" +
                "channel=" + channel +
                ", type=" + getTypeString() +
                ", closeReason=" + closeReason +
                ", appErrorCode=" + appSpecificErrorCode +
                '}';
    }

    public static final Creator<ChannelEventParcelable> CREATOR = new AutoCreator<ChannelEventParcelable>(ChannelEventParcelable.class);
}
