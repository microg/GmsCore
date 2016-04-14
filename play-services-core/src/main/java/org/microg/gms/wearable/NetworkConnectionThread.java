/*
 * Copyright 2013-2015 microG Project Team
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

package org.microg.gms.wearable;

import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.wearable.SocketWearableConnection;
import org.microg.wearable.WearableConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class NetworkConnectionThread extends Thread {
    private static final String TAG = "GmsWearNetConnThr";
    private static final int WEAR_TCP_PORT = 5601;

    private ConnectionConfiguration config;
    private ServerSocket socket;
    private MessageHandler handler;
    private WearableConnection wearableConnection;

    public NetworkConnectionThread(WearableImpl wearable, ConnectionConfiguration config) {
        this.config = config;
        this.handler  = new MessageHandler(wearable, config);
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    public WearableConnection getWearableConnection() {
        return wearableConnection;
    }

    @Override
    public void run() {
        try {
            socket = new ServerSocket(WEAR_TCP_PORT);
            Log.d(TAG, "Listening for connections on TCP :" + WEAR_TCP_PORT);
            Socket accepted = socket.accept();
            (wearableConnection = new SocketWearableConnection(accepted, handler)).run();
            Log.d(TAG, "Connection terminated, me too");
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }
}
