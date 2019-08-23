/*
 * Copyright (C) 2013-2019 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.games.multiplayer.realtime;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class RealTimeMessage implements Parcelable {
    public static final int RELIABLE = 1;
    public static final int UNRELIABLE = 0;

    private final String mSenderParticipantId;
    private final byte[] mMessageData;
    private final int mIsReliable;

    public RealTimeMessage(String senderParticipantId, byte[] messageData, int isReliable) {
        this.mSenderParticipantId = senderParticipantId;
        this.mMessageData = messageData.clone();
        this.mIsReliable = isReliable;
    }

    private RealTimeMessage(Parcel parcel) {
        this(parcel.readString(), parcel.createByteArray(), parcel.readInt());
    }

    public static final Creator<RealTimeMessage> CREATOR = new Creator<RealTimeMessage>() {
        @Override
        public RealTimeMessage createFromParcel(Parcel in) {
            return new RealTimeMessage(in);
        }
        @Override
        public RealTimeMessage[] newArray(int size) {
            return new RealTimeMessage[size];
        }
    };

    public byte[] getMessageData() {
        return this.mMessageData;
    }

    public String getSenderParticipantId() {
        return this.mSenderParticipantId;
    }

    public boolean isReliable() {
        return this.mIsReliable == RELIABLE;
    }
    
    @Override
    public void writeToParcel(Parcel parcel, int flag) {
        parcel.writeString(this.mSenderParticipantId);
        parcel.writeByteArray(this.mMessageData);
        parcel.writeInt(this.mIsReliable);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
