package com.google.android.gms.wearable.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class WearableService : Service() {
    private lateinit var impl: WearableServiceImpl

    override fun onCreate() {
        super.onCreate()
        impl = WearableServiceImpl(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == "com.google.android.gms.wearable.BIND") {
            return impl
        }
        return null
    }
}
