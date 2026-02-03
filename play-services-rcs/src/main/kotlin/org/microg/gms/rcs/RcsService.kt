/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsService - Main entry point for RCS functionality
 * 
 * This service handles binding from Google Messages and other RCS clients.
 * It manages the lifecycle of RCS registration, provisioning, and messaging.
 */

package org.microg.gms.rcs

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

class RcsService : BaseService(TAG, GmsService.RCS) {

    companion object {
        private const val TAG = "GmsRcsService"
    }

    private lateinit var rcsImplementation: RcsImplementation
    private lateinit var provisioningManager: RcsProvisioningManager
    private lateinit var capabilitiesManager: RcsCapabilitiesManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RCS Service created")
        
        initializeComponents()
    }

    private fun initializeComponents() {
        org.microg.gms.rcs.di.RcsServiceLocator.initialize(this)
        
        provisioningManager = RcsProvisioningManager(this)
        capabilitiesManager = RcsCapabilitiesManager(this)
        
        // Register instances with ServiceLocator so Activity can access them
        org.microg.gms.rcs.di.RcsServiceLocator.registerSingleton(RcsProvisioningManager::class.java) { provisioningManager }
        org.microg.gms.rcs.di.RcsServiceLocator.registerSingleton(RcsCapabilitiesManager::class.java) { capabilitiesManager }
        
        rcsImplementation = RcsImplementation(this, provisioningManager, capabilitiesManager)
        
        Log.d(TAG, "RCS components initialized successfully")
    }

    override fun handleServiceRequest(
        callback: com.google.android.gms.common.internal.IGmsCallbacks,
        request: com.google.android.gms.common.internal.GetServiceRequest,
        service: GmsService
    ) {
        val callingPackage = request.packageName
        Log.d(TAG, "Service request from package: $callingPackage")
        
        val isGoogleMessages = callingPackage == "com.google.android.apps.messaging"
        val hasPermission = checkCallerPermissions(callingPackage)
        
        if (!hasPermission) {
            Log.w(TAG, "Package $callingPackage does not have required permissions")
        }
        
        callback.onPostInitComplete(
            com.google.android.gms.common.api.CommonStatusCodes.SUCCESS,
            rcsImplementation.asBinder(),
            null
        )
    }

    private fun checkCallerPermissions(packageName: String): Boolean {
        val packageManager = packageManager
        
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo != null
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to check permissions for $packageName", exception)
            false
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "RCS Service destroyed")
        provisioningManager.cleanup()
        super.onDestroy()
    }
}
