/*
 * SPDX-FileCopyrightText: 2019 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.multiplayer.realtime;

import android.os.Parcel;
import android.os.Parcelable;
import org.microg.gms.common.Hide;

@Hide
public final class RealTimeMessage implements Parcelable {
    public static final int RELIABLE = 1;
    public static final int UNRELIABLE = 0;

    private final String senderParticipantId;
    private final byte[] messageData;
    private final int reliable;

    public RealTimeMessage(String senderParticipantId, byte[] messageData, int reliable) {
        this.senderParticipantId = senderParticipantId;
        this.messageData = messageData.clone();
        this.reliable = reliable;
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
        return this.messageData;
    }

    public String getSenderParticipantId() {
        return this.senderParticipantId;
    }

    public boolean isReliable() {
        return this.reliable == RELIABLE;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flag) {
        parcel.writeString(this.senderParticipantId);
        parcel.writeByteArray(this.messageData);
        parcel.writeInt(this.reliable);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
