/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.context

import android.app.BroadcastOptions
import android.content.ComponentCallbacks
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.annotation.RequiresApi
import org.microg.gms.profile.Build.VERSION.SDK_INT

class GmsContextWrapper(
    private val wrappedContext: Context,
    skipInternalOrImplicitChecks: Boolean = false
) : ContextWrapper(wrappedContext) {

    companion object {
        private fun getDefaultBundle(bundle: Bundle?): Bundle? {
            return if (bundle == null && SDK_INT >= 34) {
                BroadcastOptions.makeBasic().apply {
                    setShareIdentityEnabled(true)
                }.toBundle()
            } else {
                bundle
            }
        }
    }

    override fun getApplicationContext(): Context = GmsContextWrapper(wrappedContext.applicationContext)

    override fun registerComponentCallbacks(callback: ComponentCallbacks?) {
        super.getApplicationContext().registerComponentCallbacks(callback)
    }

    override fun unregisterComponentCallbacks(callback: ComponentCallbacks?) {
        super.getApplicationContext().unregisterComponentCallbacks(callback)
    }

    @RequiresApi(34)
    override fun sendBroadcast(intent: Intent, receiverPermission: String?, options: Bundle?) =
        wrappedContext.sendBroadcast(intent, receiverPermission, getDefaultBundle(options))
}
