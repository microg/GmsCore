package org.microg.gms.rcs

import android.content.Context
import android.util.Log

class RcsServiceRouter(private val context: Context) {
    private val constellationService = ConstellationRcsService()
    private val asterismManager = AsterismManager(context)

    fun initializeRcsFlow() {
        Log.d("RcsServiceRouter", "Routing RCS initialization request...")

        if (asterismManager.isRcsActivated()) {
            Log.d("RcsServiceRouter", "RCS is already activated via Asterism.")
            return
        }

        constellationService.requestPhoneNumberVerification(
            onSuccess = {
                asterismManager.setRcsActivated(true)
                Log.d("RcsServiceRouter", "RCS successfully provisioned and saved!")
            },
            onError = { error ->
                Log.e("RcsServiceRouter", "RCS routing encountered an error: ${error.localizedMessage}")
            }
        )
    }

    fun cleanup() {
        constellationService.shutdown()
    }
}