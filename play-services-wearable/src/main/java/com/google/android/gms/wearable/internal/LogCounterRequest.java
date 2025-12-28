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

public class LogCounterRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public String counterName;
    @SafeParceled(2)
    public long value;
    @SafeParceled(3)
    public byte[] counterData;
    @SafeParceled(4)
    public long timestamp;
    @SafeParceled(5)
    public boolean increment;

    private LogCounterRequest() {}

    public LogCounterRequest(String counterName, long value, byte[] counterData, long timestamp, boolean increment) {
        this.counterName = counterName;
        this.value = value;
        this.counterData = counterData;
        this.timestamp = timestamp;
        this.increment = increment;
    }

    @Override
    public String toString() {
        return "LogCounterRequest{counterName='" + counterName + "', value=" + value +
                ", timestamp=" + timestamp + ", increment=" + increment +
                ", counterData.length=" + (counterData != null ? counterData.length : 0) + "}";
    }

    public static final Creator<LogCounterRequest> CREATOR = new AutoCreator<>(LogCounterRequest.class);
}