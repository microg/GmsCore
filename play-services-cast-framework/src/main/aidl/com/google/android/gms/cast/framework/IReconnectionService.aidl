package com.google.android.gms.cast.framework;

interface IReconnectionService {
    void onCreate() = 0;
    int onStartCommand(in Intent intent, int flags, int startId) = 1;
    IBinder onBind(in Intent intent) = 2;
    void onDestroy() = 3;
}