/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.provision

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import org.microg.gms.checkin.getCheckinServiceInfo
import org.microg.gms.checkin.setCheckinServiceConfiguration
import org.microg.gms.gcm.getGcmServiceInfo
import org.microg.gms.gcm.setGcmServiceConfiguration
import org.microg.gms.snet.getSafetyNetServiceInfo
import org.microg.gms.snet.setSafetyNetServiceConfiguration

class ProvisionService : LifecycleService() {
    private fun Bundle.getBooleanOrNull(key: String): Boolean? {
        return if (containsKey(key)) getBoolean(key) else null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        lifecycleScope.launchWhenStarted {
            intent?.extras?.let {
                val s = it.keySet().map { key -> "$key = ${it[key]}" }.joinToString(", ")
                Log.d(TAG, "Provisioning: $s")
            }

            intent?.extras?.getBooleanOrNull("checkin_enabled")?.let { setCheckinServiceConfiguration(this@ProvisionService, getCheckinServiceInfo(this@ProvisionService).configuration.copy(enabled = it)) }
            intent?.extras?.getBooleanOrNull("gcm_enabled")?.let { setGcmServiceConfiguration(this@ProvisionService, getGcmServiceInfo(this@ProvisionService).configuration.copy(enabled = it)) }
            intent?.extras?.getBooleanOrNull("safetynet_enabled")?.let { setSafetyNetServiceConfiguration(this@ProvisionService, getSafetyNetServiceInfo(this@ProvisionService).configuration.copy(enabled = it)) }
            // What else?

            delay(2 * 1000) // Wait 2 seconds to give provisioning some extra time
            stopSelfResult(startId)
        }
        return Service.START_NOT_STICKY
    }

    companion object {
        private const val TAG = "GmsProvision"
    }
}
