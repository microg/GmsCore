/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common

import android.content.Intent
import android.os.Process

class InternalIntentMessenger(val handleIntent: (Intent) -> Unit) : IIntentMessenger.Stub() {
    override fun sendIntent(intent: Intent?) {
        if (intent == null || getCallingUid() != Process.myUid()) return
        handleIntent(intent)
    }
}