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

import android.util.Log;

import com.squareup.wire.Message;

import org.microg.gms.gcm.mcs.DataMessageStanza;
import org.microg.gms.gcm.mcs.HeartbeatAck;
import org.microg.gms.gcm.mcs.HeartbeatPing;
import org.microg.gms.gcm.mcs.LoginRequest;

import java.io.IOException;
import java.io.OutputStream;

import static org.microg.gms.gcm.Constants.*;

public class McsOutputStream {
    private static final String TAG = "GmsGcmMcsOutput";

    private final OutputStream os;
    private boolean initialized;
    private int version = MCS_VERSION_CODE;
    private int streamId = 0;

    public McsOutputStream(OutputStream os) {
        this(os, false);
    }

    public McsOutputStream(OutputStream os, boolean initialized) {
        this.os = os;
        this.initialized = initialized;
    }

    public int getStreamId() {
        return streamId;
    }

    public void write(DataMessageStanza message) throws IOException {
        write(message, MCS_DATA_MESSAGE_STANZA_TAG);
    }

    public void write(LoginRequest loginRequest) throws IOException {
        write(loginRequest, MCS_LOGIN_REQUEST_TAG);
    }

    public void write(HeartbeatAck ack) throws IOException {
        write(ack, MCS_HEARTBEAT_ACK_TAG);
    }

    public void write(HeartbeatPing ping) throws IOException{
        write(ping, MCS_HEARTBEAT_PING_TAG);
    }

    public synchronized void write(Message message, int tag) throws IOException {
        if (!initialized) {
            Log.d(TAG, "Write MCS version code: " + version);
            os.write(version);
            initialized = true;
        }
        Log.d(TAG, "Write to MCS: " + message);
        os.write(tag);
        writeVarint(os, message.getSerializedSize());
        os.write(message.toByteArray());
        os.flush();
        streamId++;
    }

    private void writeVarint(OutputStream os, int value) throws IOException {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                os.write(value);
                return;
            } else {
                os.write((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }
}
