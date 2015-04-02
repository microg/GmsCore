/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.gms.gcm;

import android.util.Base64;
import android.util.Log;

import com.squareup.wire.Message;
import com.squareup.wire.Wire;

import org.microg.gms.gcm.mcs.Close;
import org.microg.gms.gcm.mcs.DataMessageStanza;
import org.microg.gms.gcm.mcs.HeartbeatAck;
import org.microg.gms.gcm.mcs.HeartbeatPing;
import org.microg.gms.gcm.mcs.IqStanza;
import org.microg.gms.gcm.mcs.LoginRequest;
import org.microg.gms.gcm.mcs.LoginResponse;

import java.io.IOException;
import java.io.InputStream;

import static org.microg.gms.gcm.Constants.MCS_CLOSE_TAG;
import static org.microg.gms.gcm.Constants.MCS_DATA_MESSAGE_STANZA_TAG;
import static org.microg.gms.gcm.Constants.MCS_HEARTBEAT_ACK_TAG;
import static org.microg.gms.gcm.Constants.MCS_HEARTBEAT_PING_TAG;
import static org.microg.gms.gcm.Constants.MCS_IQ_STANZA_TAG;
import static org.microg.gms.gcm.Constants.MCS_LOGIN_REQUEST_TAG;
import static org.microg.gms.gcm.Constants.MCS_LOGIN_RESPONSE_TAG;

public class McsInputStream {
    private static final String TAG = "GmsGcmMcsInput";

    private final InputStream is;
    private boolean initialized;
    private int version = -1;
    private int lastStreamIdReported = -1;
    private int streamId = 0;

    public McsInputStream(InputStream is) {
        this(is, false);
    }

    public McsInputStream(InputStream is, boolean initialized) {
        this.is = is;
        this.initialized = initialized;
    }

    public int getStreamId() {
        lastStreamIdReported = streamId;
        return streamId;
    }

    public boolean newStreamIdAvailable() {
        return lastStreamIdReported != streamId;
    }

    public int getVersion() {
        ensureVersionRead();
        return version;
    }

    private void ensureVersionRead() {
        if (!initialized) {
            try {
                version = is.read();
                Log.d(TAG, "Reading from MCS version=" + version);
                initialized = true;
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }
    }

    public synchronized Message read() throws IOException {
        ensureVersionRead();
        int mcsTag = is.read();
        int mcsSize = readVarint();
        Log.d(TAG, "Reading from MCS tag=" + mcsTag + " size=" + mcsSize);
        byte[] bytes = new byte[mcsSize];
        int len = 0;
        while ((len += is.read(bytes, len, mcsSize - len)) < mcsSize) ;
        Log.d(TAG, "Reading from MCS: " + Base64.encodeToString(bytes, 0));
        Message read = read(mcsTag, bytes, len);
        Log.d(TAG, "Read from MCS: " + read);
        streamId++;
        return read;
    }

    private static Message read(int mcsTag, byte[] bytes, int len) throws IOException {
        Wire wire = new Wire();
        switch (mcsTag) {
            case MCS_HEARTBEAT_PING_TAG:
                return wire.parseFrom(bytes, 0, len, HeartbeatPing.class);
            case MCS_HEARTBEAT_ACK_TAG:
                return wire.parseFrom(bytes, 0, len, HeartbeatAck.class);
            case MCS_LOGIN_REQUEST_TAG:
                return wire.parseFrom(bytes, 0, len, LoginRequest.class);
            case MCS_LOGIN_RESPONSE_TAG:
                return wire.parseFrom(bytes, 0, len, LoginResponse.class);
            case MCS_CLOSE_TAG:
                return wire.parseFrom(bytes, 0, len, Close.class);
            case MCS_IQ_STANZA_TAG:
                return wire.parseFrom(bytes, 0, len, IqStanza.class);
            case MCS_DATA_MESSAGE_STANZA_TAG:
                return wire.parseFrom(bytes, 0, len, DataMessageStanza.class);
            default:
                return null;
        }
    }

    private int readVarint() throws IOException {
        int res = 0;
        int s = 0;
        int b = 0x80;
        while ((b & 0x80) == 0x80) {
            b = is.read();
            res |= (b & 0x7F) << s;
            s += 7;
        }
        return res;
    }


}
