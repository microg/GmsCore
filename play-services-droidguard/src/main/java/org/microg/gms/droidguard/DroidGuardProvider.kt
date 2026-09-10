package org.microg.gms.droidguard

import android.content.Context
import android.util.Log
import org.microg.gms.settings.SettingsContract

class DroidGuardProvider {
    private var localDroidGuard: DroidGuardLocal? = null
    private var remoteDroidGuard: DroidGuardRemote? = null
    private var context: Context? = null
    
    fun initialize(context: Context) {
        this.context = context
        val remote = remoteDroidGuard
        return if (remote != null && shouldUseRemote()) {
            Log.d(TAG, "Using remote DroidGuard")
            remote.getResult(context!!, request)
        } else {
            Log.d(TAG, "Using local DroidGuard")
            localDroidGuard?.getResult(request) ?: throw IllegalStateException("No DroidGuard implementation available")