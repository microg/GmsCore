package com.android.vending.billing

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.RequiresApi
import org.microg.gms.profile.ProfileManager
import org.microg.vending.billing.ContextProvider
import org.microg.vending.billing.InAppBillingServiceImpl

@RequiresApi(21)
class InAppBillingService: Service() {
    override fun onCreate() {
        super.onCreate()
        ProfileManager.ensureInitialized(this)
        ContextProvider.init(application)
    }

    override fun onBind(intent: Intent): IBinder {
        return InAppBillingServiceImpl(this)
    }


}