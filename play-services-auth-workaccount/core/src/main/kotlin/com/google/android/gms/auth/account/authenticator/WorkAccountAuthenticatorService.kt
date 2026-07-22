/*
 * SPDX-FileCopyrightText: 2024 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.account.authenticator

import android.accounts.AccountManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class WorkAccountAuthenticatorService : Service() {
    private val authenticator by lazy { WorkAccountAuthenticator(this) }

    override fun onBind(intent: Intent): IBinder? {
        if (intent.action == AccountManager.ACTION_AUTHENTICATOR_INTENT) {
            return authenticator.iBinder
        }
        return null
    }
}