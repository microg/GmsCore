package org.microg.gms.rcs

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference

class RcsProvisioningService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val currentState = AtomicReference<String>("IDLE")

    companion object {
        private const val TAG = "RcsProvisioning"
        const val STATUS_CARRIER_UNAVAILABLE = 503
    }

    override fun onBind(intent: Intent?): IBinder? = null

    suspend fun executeProvisioning(carrierId: String): Boolean = withContext(Dispatchers.IO) {
        if (carrierId.isBlank()) return@withContext false
        currentState.set("PROVISIONING")
        Log.i(TAG, "Starting non-blocking RCS handshake for carrier: $carrierId")
        delay(100) // Simulated network handshake
        currentState.set("ACTIVE")
        return@withContext true
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}