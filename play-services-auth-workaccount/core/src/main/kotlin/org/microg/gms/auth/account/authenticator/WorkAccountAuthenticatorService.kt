package org.microg.gms.auth.account.authenticator

import android.app.Service
import android.content.Intent
import android.os.IBinder

class WorkAccountAuthenticatorService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}