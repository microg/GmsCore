/*
 * Copyright (C) 2013-2026 microG Project Team
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

package com.google.android.gms.cast;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Display;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CastRemoteDisplayLocalService extends Service {
    private static final String TAG = "CastRemoteDisplayLocalSvc";
    private static final int NOTIFICATION_ID = 404040;
    private static final Object sSyncObject = new Object();
    private static WeakReference<CastRemoteDisplayLocalService> sInstance;
    private static final AtomicBoolean sIsServiceRunning = new AtomicBoolean(false);
    private static Callbacks sCallbacks;
    private static NotificationSettings sNotificationSettings;
    private static CastDevice sCastDevice;
    private static String sApplicationId;
    private static Context sApplicationContext;
    private static ServiceConnection sServiceConnection;

    private final IBinder mBinder = new LocalBinder();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Display mDisplay;
    private boolean mInForeground = false;

    public class LocalBinder extends Binder {
        public CastRemoteDisplayLocalService getService() {
            return CastRemoteDisplayLocalService.this;
        }
    }

    public interface Callbacks {
        void onServiceCreated(CastRemoteDisplayLocalService service);
        void onRemoteDisplaySessionStarted(CastRemoteDisplayLocalService service);
        void onRemoteDisplaySessionError(Status status);
        void onRemoteDisplaySessionEnded(CastRemoteDisplayLocalService service);
    }

    public static final class NotificationSettings {
        private final Notification mNotification;
        private final PendingIntent mPendingIntent;
        private final String mNotificationTitle;
        private final String mNotificationText;

        private NotificationSettings(Builder builder) {
            this.mNotification = builder.mNotification;
            this.mPendingIntent = builder.mPendingIntent;
            this.mNotificationTitle = builder.mNotificationTitle;
            this.mNotificationText = builder.mNotificationText;
        }

        public Notification getNotification() {
            return mNotification;
        }

        public PendingIntent getPendingIntent() {
            return mPendingIntent;
        }

        public String getNotificationTitle() {
            return mNotificationTitle;
        }

        public String getNotificationText() {
            return mNotificationText;
        }

        public static final class Builder {
            private Notification mNotification;
            private PendingIntent mPendingIntent;
            private String mNotificationTitle;
            private String mNotificationText;

            public Builder setNotification(Notification notification) {
                this.mNotification = notification;
                return this;
            }

            public Builder setNotificationPendingIntent(PendingIntent pendingIntent) {
                this.mPendingIntent = pendingIntent;
                return this;
            }

            public Builder setNotificationTitle(String title) {
                this.mNotificationTitle = title;
                return this;
            }

            public Builder setNotificationText(String text) {
                this.mNotificationText = text;
                return this;
            }

            public NotificationSettings build() {
                return new NotificationSettings(this);
            }
        }
    }

    public static void startService(
            Context context,
            Class<? extends CastRemoteDisplayLocalService> serviceClass,
            String applicationId,
            CastDevice castDevice,
            NotificationSettings notificationSettings,
            Callbacks callbacks) {
        synchronized (sSyncObject) {
            if (context == null || serviceClass == null || applicationId == null || castDevice == null) {
                throw new IllegalArgumentException("Arguments cannot be null");
            }
            sApplicationContext = context.getApplicationContext();
            sApplicationId = applicationId;
            sCastDevice = castDevice;
            sNotificationSettings = notificationSettings;
            sCallbacks = callbacks;

            Intent intent = new Intent(context, serviceClass);
            context.startService(intent);

            sServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    if (service instanceof LocalBinder) {
                        CastRemoteDisplayLocalService localService = ((LocalBinder) service).getService();
                        sInstance = new WeakReference<>(localService);
                        if (sCallbacks != null) {
                            sCallbacks.onServiceCreated(localService);
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    sInstance = null;
                }
            };
            context.bindService(intent, sServiceConnection, Context.BIND_AUTO_CREATE);
            sIsServiceRunning.set(true);
        }
    }

    public static void stopService() {
        synchronized (sSyncObject) {
            if (!sIsServiceRunning.get()) {
                return;
            }
            if (sInstance != null) {
                CastRemoteDisplayLocalService service = sInstance.get();
                if (service != null) {
                    service.stopRemoteDisplaySession();
                }
            }
            if (sApplicationContext != null && sServiceConnection != null) {
                try {
                    sApplicationContext.unbindService(sServiceConnection);
                } catch (Exception ignored) {
                }
                sServiceConnection = null;
            }
            sIsServiceRunning.set(false);
            sCallbacks = null;
            sNotificationSettings = null;
            sCastDevice = null;
            sApplicationId = null;
        }
    }

    public static CastRemoteDisplayLocalService getInstance() {
        synchronized (sSyncObject) {
            return sInstance != null ? sInstance.get() : null;
        }
    }

    public static boolean isServiceRunning() {
        return sIsServiceRunning.get();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = new WeakReference<>(this);
        if (sNotificationSettings != null && sNotificationSettings.getNotification() != null) {
            startForeground(NOTIFICATION_ID, sNotificationSettings.getNotification());
            mInForeground = true;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initRemoteDisplaySession();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        stopRemoteDisplaySession();
        if (mInForeground) {
            stopForeground(true);
            mInForeground = false;
        }
        sInstance = null;
        super.onDestroy();
    }

    public CastDevice getCastDevice() {
        return sCastDevice;
    }

    public Display getDisplay() {
        return mDisplay;
    }

    public void setDisplay(Display display) {
        this.mDisplay = display;
    }

    public void updateNotificationSettings(NotificationSettings notificationSettings) {
        sNotificationSettings = notificationSettings;
        if (notificationSettings != null && notificationSettings.getNotification() != null && mInForeground) {
            startForeground(NOTIFICATION_ID, notificationSettings.getNotification());
        }
    }

    protected void onRemoteDisplaySessionStarted() {
        if (sCallbacks != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (sCallbacks != null) {
                        sCallbacks.onRemoteDisplaySessionStarted(CastRemoteDisplayLocalService.this);
                    }
                }
            });
        }
    }

    protected void onRemoteDisplaySessionError(final Status status) {
        if (sCallbacks != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (sCallbacks != null) {
                        sCallbacks.onRemoteDisplaySessionError(status);
                    }
                }
            });
        }
    }

    protected void onRemoteDisplaySessionEnded() {
        if (sCallbacks != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (sCallbacks != null) {
                        sCallbacks.onRemoteDisplaySessionEnded(CastRemoteDisplayLocalService.this);
                    }
                }
            });
        }
    }

    private void initRemoteDisplaySession() {
        if (sCastDevice != null && sApplicationId != null) {
            onRemoteDisplaySessionStarted();
        }
    }

    private void stopRemoteDisplaySession() {
        onRemoteDisplaySessionEnded();
        stopSelf();
    }
}
