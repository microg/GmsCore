/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ProvisioningCoordinatorTest {

    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var telephonyManager: TelephonyManager
    @Mock
    private lateinit var subscriptionManager: SubscriptionManager
    @Mock
    private lateinit var carrierConfigManager: CarrierConfigManager

    private lateinit var coordinator: ProvisioningCoordinator

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.getSystemService(TelephonyManager::class.java)).thenReturn(telephonyManager)
        `when`(context.getSystemService(SubscriptionManager::class.java)).thenReturn(subscriptionManager)
        `when`(context.getSystemService(CarrierConfigManager::class.java)).thenReturn(carrierConfigManager)
        
        coordinator = ProvisioningCoordinator(context, Dispatchers.Unconfined)
    }

    private fun grantPermission(permission: String, granted: Boolean) {
        `when`(context.checkPermission(eq(permission), anyInt(), anyInt())).thenReturn(
            if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        )
    }

    @Test
    fun getStatusBundle_withoutPermission_returnsNotReady() {
        grantPermission(Manifest.permission.READ_PHONE_STATE, false)
        val bundle = coordinator.getStatusBundle()
        assertFalse(bundle.getBoolean("ready"))
        assertEquals("PENDING_CARRIER_CONFIG", bundle.getString("state"))
    }

    @Test
    fun provision_invalidRequest_returnsFailure() = runBlocking {
        // appType provided but authType and challenge are missing
        val request = ProvisioningRequest(appType = 1) 
        val result = coordinator.provision(request)
        
        assertTrue(result is ProvisioningResult.Failure)
        assertEquals(Reason.INVALID_REQUEST, (result as ProvisioningResult.Failure).reason)
        assertEquals(ProvisioningState.PERMANENT_FAILURE, coordinator.state)
    }

    @Test
    fun provision_missingPhoneStatePermission_returnsRetry() = runBlocking {
        val request = ProvisioningRequest()
        grantPermission(Manifest.permission.READ_PHONE_STATE, false)
        
        val result = coordinator.provision(request)
        
        assertTrue(result is ProvisioningResult.Retry)
        assertEquals(Reason.MISSING_READ_PHONE_STATE, (result as ProvisioningResult.Retry).reason)
        assertEquals(ProvisioningState.RETRYABLE_FAILURE, coordinator.state)
    }
    
    @Test
    fun provision_noActiveSubscription_returnsRetry() = runBlocking {
        val request = ProvisioningRequest()
        grantPermission(Manifest.permission.READ_PHONE_STATE, true)
        `when`(subscriptionManager.activeSubscriptionInfoList).thenReturn(emptyList())
        
        val result = coordinator.provision(request)
        
        assertTrue(result is ProvisioningResult.Retry)
        assertEquals(Reason.NO_ACTIVE_SUBSCRIPTION, (result as ProvisioningResult.Retry).reason)
    }

    @Test
    fun provision_missingPhoneNumbersPermission_returnsRetry() = runBlocking {
        val request = ProvisioningRequest()
        grantPermission(Manifest.permission.READ_PHONE_STATE, true)
        grantPermission(Manifest.permission.READ_PHONE_NUMBERS, false)
        
        val subInfo = mock(SubscriptionInfo::class.java)
        `when`(subInfo.subscriptionId).thenReturn(1)
        `when`(subscriptionManager.activeSubscriptionInfoList).thenReturn(listOf(subInfo))
        
        val result = coordinator.provision(request)
        
        assertTrue(result is ProvisioningResult.Retry)
        assertEquals(Reason.MISSING_READ_PHONE_NUMBERS, (result as ProvisioningResult.Retry).reason)
    }

    @Test
    fun provision_carrierConfigUnavailable_returnsRetry() = runBlocking {
        val request = ProvisioningRequest()
        grantPermission(Manifest.permission.READ_PHONE_STATE, true)
        grantPermission(Manifest.permission.READ_PHONE_NUMBERS, true)
        
        val subInfo = mock(SubscriptionInfo::class.java)
        `when`(subInfo.subscriptionId).thenReturn(1)
        `when`(subscriptionManager.activeSubscriptionInfoList).thenReturn(listOf(subInfo))
        
        val telephony = mock(TelephonyManager::class.java)
        `when`(telephonyManager.createForSubscriptionId(1)).thenReturn(telephony)
        `when`(telephony.line1Number).thenReturn("1234567890")
        
        `when`(carrierConfigManager.getConfigForSubId(1)).thenReturn(null)
        
        val result = coordinator.provision(request)
        
        assertTrue(result is ProvisioningResult.Retry)
        assertEquals(Reason.CARRIER_CONFIG_UNAVAILABLE, (result as ProvisioningResult.Retry).reason)
    }
    
    @Test
    fun provision_carrierNotSupported_returnsFailure() = runBlocking {
        val request = ProvisioningRequest()
        grantPermission(Manifest.permission.READ_PHONE_STATE, true)
        grantPermission(Manifest.permission.READ_PHONE_NUMBERS, true)
        
        val subInfo = mock(SubscriptionInfo::class.java)
        `when`(subInfo.subscriptionId).thenReturn(1)
        `when`(subscriptionManager.activeSubscriptionInfoList).thenReturn(listOf(subInfo))
        
        val telephony = mock(TelephonyManager::class.java)
        `when`(telephonyManager.createForSubscriptionId(1)).thenReturn(telephony)
        `when`(telephony.line1Number).thenReturn("1234567890")
        
        val bundle = PersistableBundle()
        bundle.putBoolean("carrier_supports_rcs_provisioning", false)
        `when`(carrierConfigManager.getConfigForSubId(1)).thenReturn(bundle)
        
        val result = coordinator.provision(request)
        
        assertTrue(result is ProvisioningResult.Failure)
        assertEquals(Reason.CARRIER_NOT_SUPPORTED, (result as ProvisioningResult.Failure).reason)
    }

    @Test
    fun provision_successful_returnsSuccess() = runBlocking {
        val request = ProvisioningRequest()
        grantPermission(Manifest.permission.READ_PHONE_STATE, true)
        grantPermission(Manifest.permission.READ_PHONE_NUMBERS, true)
        
        val subInfo = mock(SubscriptionInfo::class.java)
        `when`(subInfo.subscriptionId).thenReturn(1)
        `when`(subscriptionManager.activeSubscriptionInfoList).thenReturn(listOf(subInfo))
        
        val telephony = mock(TelephonyManager::class.java)
        `when`(telephonyManager.createForSubscriptionId(1)).thenReturn(telephony)
        `when`(telephony.line1Number).thenReturn("1234567890")
        
        val bundle = PersistableBundle()
        bundle.putBoolean("carrier_supports_rcs_provisioning", true)
        `when`(carrierConfigManager.getConfigForSubId(1)).thenReturn(bundle)
        
        val result = coordinator.provision(request)
        
        assertTrue(result is ProvisioningResult.Success)
        assertEquals(ProvisioningState.COMPLETE, coordinator.state)
    }
    @Test
    fun provision_multiSim_choosesRcsCapableSimOverDataSim() = runBlocking<Unit> {
        val request = ProvisioningRequest()
        grantPermission(Manifest.permission.READ_PHONE_STATE, true)
        grantPermission(Manifest.permission.READ_PHONE_NUMBERS, true)
        
        val subInfo1 = mock(SubscriptionInfo::class.java)
        `when`(subInfo1.subscriptionId).thenReturn(1)
        val subInfo2 = mock(SubscriptionInfo::class.java)
        `when`(subInfo2.subscriptionId).thenReturn(2)
        `when`(subscriptionManager.activeSubscriptionInfoList).thenReturn(listOf(subInfo1, subInfo2))
        
        val telephony1 = mock(TelephonyManager::class.java)
        `when`(telephonyManager.createForSubscriptionId(1)).thenReturn(telephony1)
        `when`(telephony1.line1Number).thenReturn("1234567890")
        
        val bundle1 = PersistableBundle()
        bundle1.putBoolean("carrier_supports_rcs_provisioning", true)
        `when`(carrierConfigManager.getConfigForSubId(1)).thenReturn(bundle1)

        val bundle2 = PersistableBundle()
        bundle2.putBoolean("carrier_supports_rcs_provisioning", false)
        `when`(carrierConfigManager.getConfigForSubId(2)).thenReturn(bundle2)
        
        mockStatic(SubscriptionManager::class.java).use { mockedSubMgr ->
            mockedSubMgr.`when`<Int> { SubscriptionManager.getDefaultDataSubscriptionId() }.thenReturn(2)
            
            val result = coordinator.provision(request)
            
            assertTrue(result is ProvisioningResult.Success)
            assertEquals(ProvisioningState.COMPLETE, coordinator.state)
            
            verify(telephonyManager, atLeastOnce()).createForSubscriptionId(1)
        }
    }
}
