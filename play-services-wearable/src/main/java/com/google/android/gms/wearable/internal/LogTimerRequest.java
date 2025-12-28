/*
 * Copyright 2013-2025 microG Project Team
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

public class LogTimerRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public String timerName;
    @SafeParceled(2)
    public long timestamp;
    @SafeParceled(3)
    public byte[] timerData;

    private LogTimerRequest() {}

    public LogTimerRequest(String timerName, long timestamp, byte[] timerData) {
        this.timerName = timerName;
        this.timestamp = timestamp;
        this.timerData = timerData;
    }

    @Override
    public String toString() {
        return "LogTimerRequest{timerName='" + timerName + "', timestamp=" + timestamp +
                ", timerData.length=" + (timerData != null ? timerData.length : 0) + "}";
    }

    public static final Creator<LogTimerRequest> CREATOR = new AutoCreator<>(LogTimerRequest.class);
}
