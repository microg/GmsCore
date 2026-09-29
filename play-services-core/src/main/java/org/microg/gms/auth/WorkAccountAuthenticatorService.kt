/*
 * SPDX-FileCopyrightText: 2024 e foundation
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth

import android.accounts.AccountManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.microg.gms.auth.loginservice.AccountAuthenticator

class WorkAccountAuthenticatorService : Service() {
    private val authenticator by lazy { AccountAuthenticator(this) }

    override fun onBind(intent: Intent): IBinder? {
        if (intent.action == AccountManager.ACTION_AUTHENTICATOR_INTENT) {
            return authenticator.iBinder
        }
        return null
    }
}