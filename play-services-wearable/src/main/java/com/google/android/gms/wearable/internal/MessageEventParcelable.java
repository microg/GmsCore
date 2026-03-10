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

import com.google.android.gms.wearable.MessageEvent;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class MessageEventParcelable extends AutoSafeParcelable implements MessageEvent {

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    public int requestId;
    @SafeParceled(3)
    public String path;
    @SafeParceled(4)
    public byte[] data;
    @SafeParceled(5)
    public String sourceNodeId;

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public int getRequestId() {
        return requestId;
    }

    @Override
    public String getSourceNodeId() {
        return sourceNodeId;
    }

    @Override
    public String toString() {
        return "MessageEventParcelable{" +
                "requestId=" + requestId +
                ", path='" + path + '\'' +
                ", dataSize=" + (data != null ? data.length : -1) +
                ", sourceNodeId='" + sourceNodeId + '\'' +
                '}';
    }

    public static final Creator<MessageEventParcelable> CREATOR = new AutoCreator<MessageEventParcelable>(MessageEventParcelable.class);
}
