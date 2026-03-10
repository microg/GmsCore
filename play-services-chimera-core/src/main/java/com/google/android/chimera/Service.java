/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.chimera;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class Service extends ContextWrapper implements InstanceProvider {
    public static final int START_CONTINUATION_MASK = 0xf;
    public static final int START_FLAG_REDELIVERY = 1;
    public static final int START_FLAG_RETRY = 2;
    public static final int START_NOT_STICKY = 2;
    public static final int START_REDELIVER_INTENT = 3;
    public static final int START_STICKY = 1;
    public static final int START_STICKY_COMPATIBILITY = 0;

    private android.app.Service containerService;
    private ProxyCallbacks callbacks;

    public interface ProxyCallbacks {
        void superOnCreate();

        void superOnDestroy();

        int superOnStartCommand(Intent intent, int flags, int startId);

        void superStopSelf();

        void superStopSelf(int startId);

        boolean superStopSelfResult(int startId);
    }

    public Service() {
        super(null);
    }

    protected void dump(FileDescriptor fs, PrintWriter writer, String[] args) {
    }

    public final Application getApplication() {
        return containerService.getApplication();
    }

    @Override
    public Object getChimeraImpl() {
        return this;
    }

    public android.app.Service getContainerService() {
        return containerService;
    }

    public abstract IBinder onBind(Intent intent);


    public void onConfigurationChanged(Configuration configuration) {
    }

    public void onCreate() {
        this.callbacks.superOnCreate();
    }

    public void onDestroy() {
        this.callbacks.superOnDestroy();
    }

    public void onLowMemory() {
    }

    public void onRebind(Intent intent) {
    }

    public void onStart(Intent intent, int startId) {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return this.callbacks.superOnStartCommand(intent, flags, startId);
    }

    public void onTaskRemoved(Intent rootIntent) {
    }

    public void onTrimMemory(int level) {
    }

    public boolean onUnbind(Intent intent) {
        return false;
    }

    public void publicDump(FileDescriptor fd, PrintWriter writer, String[] args) {
        dump(fd, writer, args);
    }

    public void setProxy(android.app.Service service, Context context) {
        this.containerService = service;
        this.callbacks = (ProxyCallbacks) service;
        attachBaseContext(context);
    }

    public final void startForeground(int id, Notification notification) {
        this.containerService.startForeground(id, notification);
    }

    public final void stopForeground(boolean removeNotification) {
        this.containerService.stopForeground(removeNotification);
    }

    public final void stopSelf() {
        this.callbacks.superStopSelf();
    }

    public final boolean stopSelfResult(int startId) {
        return this.callbacks.superStopSelfResult(startId);
    }

    public final void stopSelf(int startId) {
        this.callbacks.superStopSelf(startId);
    }
}
