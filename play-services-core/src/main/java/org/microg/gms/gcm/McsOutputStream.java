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
import android.os.Looper;
import android.util.Log;

import com.squareup.wire.Message;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import static org.microg.gms.gcm.McsConstants.MCS_VERSION_CODE;
import static org.microg.gms.gcm.McsConstants.MSG_OUTPUT;
import static org.microg.gms.gcm.McsConstants.MSG_OUTPUT_DONE;
import static org.microg.gms.gcm.McsConstants.MSG_OUTPUT_ERROR;
import static org.microg.gms.gcm.McsConstants.MSG_OUTPUT_READY;
import static org.microg.gms.gcm.McsConstants.MSG_TEARDOWN;

public class McsOutputStream extends Thread implements Handler.Callback, Closeable {
    private static final String TAG = "GmsGcmMcsOutput";

    private final OutputStream os;
    private boolean initialized;
    private int version = MCS_VERSION_CODE;
    private int streamId = 0;

    private final Handler mainHandler;
    private Handler myHandler;

    private volatile boolean closed = false;

    public McsOutputStream(OutputStream os, Handler mainHandler) {
        this(os, mainHandler, false);
    }

    public McsOutputStream(OutputStream os, Handler mainHandler, boolean initialized) {
        this.os = os;
        this.mainHandler = mainHandler;
        this.initialized = initialized;
        setName("McsOutputStream");
    }

    public int getStreamId() {
        return streamId;
    }

    @Override
    public void run() {
        Looper.prepare();
        myHandler = new Handler(this);
        mainHandler.dispatchMessage(mainHandler.obtainMessage(MSG_OUTPUT_READY));
        Looper.loop();
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {
        switch (msg.what) {
            case MSG_OUTPUT:
                try {
                    Log.d(TAG, "Outgoing message: " + msg.obj);
                    writeInternal((Message) msg.obj, msg.arg1);
                    mainHandler.dispatchMessage(mainHandler.obtainMessage(MSG_OUTPUT_DONE, msg.arg1, msg.arg2, msg.obj));
                } catch (IOException e) {
                    if (closed) {
                        Log.d(TAG, "We were closed already. Ignoring IOException");
                    } else {
                        mainHandler.dispatchMessage(mainHandler.obtainMessage(MSG_OUTPUT_ERROR, e));
                    }
                }
                return true;
            case MSG_TEARDOWN:
                try {
                    os.close();
                } catch (IOException ignored) {
                }
                try {
                    Looper.myLooper().quit();
                } catch (Exception ignored) {
                }
                return true;
        }
        return false;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            myHandler.getLooper().quit();
            interrupt();
        }
    }

    private synchronized void writeInternal(Message message, int tag) throws IOException {
        if (!initialized) {
            Log.d(TAG, "Write MCS version code: " + version);
            os.write(version);
            initialized = true;
        }
        os.write(tag);
        byte[] bytes = message.encode();
        writeVarint(os, bytes.length);
        os.write(bytes);
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

    public Handler getHandler() {
        return myHandler;
    }
}
