package com.google.android.gms.common.internal;

interface IGmsCallbacks {
    void onPostInitComplete(int statusCode, IBinder binder, in Bundle params);
}
