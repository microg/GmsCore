package com.google.android.gms.rcs

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.TelephonyManager
import com.google.android.gms.rcs.internal.IRcsService

class RcsService : Service() {

    private val binder = object : IRcsService.Stub() {
        override fun isRcsEnabled(): Boolean {
            return true
        }

        override fun getRcsConfig(): RcsConfig {
            return RcsConfig.Builder()
                .setEnabled(true)
                .setCapabilities(RcsCapabilities.MESSAGING_INDIVIDUAL)
                .build()
        }

        override fun startRegistration() {
            val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            val subId = tm.subscriptionId
            // Simulate authentication via SIM
            val authResult = RcsAuthentication.authenticate(subId)
            if (authResult.isSuccess) {
                // Notify RCS backend
                RcsBackend.getInstance(this).register(authResult.identity)
            }
        }

        override fun stopRegistration() {
            RcsBackend.getInstance(this).unregister()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }
}