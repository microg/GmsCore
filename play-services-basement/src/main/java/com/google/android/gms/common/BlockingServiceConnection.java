/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BlockingServiceConnection implements ServiceConnection {
    private boolean connected = false;
    private final BlockingQueue<IBinder> blockingQueue = new LinkedBlockingQueue<>();

    public BlockingServiceConnection() {
    }

    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        this.blockingQueue.add(iBinder);
    }

    public void onServiceDisconnected(ComponentName componentName) {
    }

    public IBinder getServiceWithTimeout(long time, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        if (this.connected) {
            throw new IllegalStateException("Cannot call get on this connection more than once");
        } else {
            this.connected = true;
            IBinder iBinder;
            if ((iBinder = this.blockingQueue.poll(time, timeUnit)) == null) {
                throw new TimeoutException("Timed out waiting for the service connection");
            } else {
                return iBinder;
            }
        }
    }

    public IBinder getService() throws InterruptedException {
        if (this.connected) {
            throw new IllegalStateException("Cannot call get on this connection more than once");
        } else {
            this.connected = true;
            return this.blockingQueue.take();
        }
    }
}
