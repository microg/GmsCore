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

package org.microg.gms.gcm;

import android.os.Handler;
import android.util.Log;

import com.squareup.wire.Message;

import org.microg.gms.gcm.mcs.Close;
import org.microg.gms.gcm.mcs.DataMessageStanza;
import org.microg.gms.gcm.mcs.HeartbeatAck;
import org.microg.gms.gcm.mcs.HeartbeatPing;
import org.microg.gms.gcm.mcs.IqStanza;
import org.microg.gms.gcm.mcs.LoginRequest;
import org.microg.gms.gcm.mcs.LoginResponse;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import static org.microg.gms.gcm.McsConstants.MCS_CLOSE_TAG;
import static org.microg.gms.gcm.McsConstants.MCS_DATA_MESSAGE_STANZA_TAG;
import static org.microg.gms.gcm.McsConstants.MCS_HEARTBEAT_ACK_TAG;
import static org.microg.gms.gcm.McsConstants.MCS_HEARTBEAT_PING_TAG;
import static org.microg.gms.gcm.McsConstants.MCS_IQ_STANZA_TAG;
import static org.microg.gms.gcm.McsConstants.MCS_LOGIN_REQUEST_TAG;
import static org.microg.gms.gcm.McsConstants.MCS_LOGIN_RESPONSE_TAG;
import static org.microg.gms.gcm.McsConstants.MSG_INPUT;
import static org.microg.gms.gcm.McsConstants.MSG_INPUT_ERROR;
import static org.microg.gms.gcm.McsConstants.MSG_TEARDOWN;

public class McsInputStream extends Thread implements Closeable {
    private static final String TAG = "GmsGcmMcsInput";

    private final InputStream is;
    private final Handler mainHandler;

    private boolean initialized;
    private int version = -1;
    private int lastStreamIdReported = -1;
    private int streamId = 0;
    private long lastMsgTime = 0;

    private volatile boolean closed = false;

    public McsInputStream(InputStream is, Handler mainHandler) {
        this(is, mainHandler, false);
    }

    public McsInputStream(InputStream is, Handler mainHandler, boolean initialized) {
        this.is = is;
        this.mainHandler = mainHandler;
        this.initialized = initialized;
        setName("McsInputStream");
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted() && !closed) {
                android.os.Message msg = read();
                if (msg != null) {
                    mainHandler.dispatchMessage(msg);
                } else {
                    mainHandler.dispatchMessage(mainHandler.obtainMessage(MSG_TEARDOWN, "null message"));
                    break; // if input is empty, do not continue looping
                }
            }
        } catch (IOException e) {
            if (closed) {
                Log.d(TAG, "We were closed already. Ignoring IOException");
            } else {
                mainHandler.dispatchMessage(mainHandler.obtainMessage(MSG_INPUT_ERROR, e));
            }
        }
        try {
            is.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            interrupt();
        }
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

    private synchronized void ensureVersionRead() {
        if (!initialized) {
            try {
                version = is.read();
                Log.d(TAG, "Reading from MCS version: " + version);
                initialized = true;
            } catch (IOException e) {
                Log.w(TAG, "Error reading version", e);
            }
        }
    }

    public synchronized android.os.Message read() throws IOException {
        ensureVersionRead();
        int mcsTag = is.read();
        int mcsSize = readVarint();
        if (mcsTag < 0 || mcsSize < 0) {
            Log.w(TAG, "mcsTag: " + mcsTag + " mcsSize: " + mcsSize);
            return null;
        }
        byte[] bytes = new byte[mcsSize];
        int len = 0, read = 0;
        while (len < mcsSize && read >= 0) {
            len += (read = is.read(bytes, len, mcsSize - len)) < 0 ? 0 : read;
        }
        Message message = read(mcsTag, bytes, len);
        if (message == null) return null;
        Log.d(TAG, "Incoming message: " + message);
        streamId++;
        return mainHandler.obtainMessage(MSG_INPUT, mcsTag, streamId, message);
    }

    private static Message read(int mcsTag, byte[] bytes, int len) throws IOException {
        try {
            switch (mcsTag) {
                case MCS_HEARTBEAT_PING_TAG:
                    return HeartbeatPing.ADAPTER.decode(bytes);
                case MCS_HEARTBEAT_ACK_TAG:
                    return HeartbeatAck.ADAPTER.decode(bytes);
                case MCS_LOGIN_REQUEST_TAG:
                    return LoginRequest.ADAPTER.decode(bytes);
                case MCS_LOGIN_RESPONSE_TAG:
                    return LoginResponse.ADAPTER.decode(bytes);
                case MCS_CLOSE_TAG:
                    return Close.ADAPTER.decode(bytes);
                case MCS_IQ_STANZA_TAG:
                    return IqStanza.ADAPTER.decode(bytes);
                case MCS_DATA_MESSAGE_STANZA_TAG:
                    return DataMessageStanza.ADAPTER.decode(bytes);
                default:
                    Log.w(TAG, "Unknown tag: " + mcsTag);
                    return null;
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "Error parsing tag: "+mcsTag, e);
            return null;
        }
    }

    private int readVarint() throws IOException {
        int res = 0, s = -7, read;
        do {
            res |= ((read = is.read()) & 0x7F) << (s += 7);
        } while (read >= 0 && (read & 0x80) == 0x80 && s < 32);
        if (read < 0) return -1;
        return res;
    }
}
