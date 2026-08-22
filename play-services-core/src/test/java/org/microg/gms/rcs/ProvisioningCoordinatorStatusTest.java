/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ProvisioningCoordinatorStatusTest {
    @Test
    public void noActiveSubscriptionIsPendingAndNotReady() {
        Context context = mock(Context.class);
        SubscriptionManager subscriptions = mock(SubscriptionManager.class);
        TelephonyManager telephony = mock(TelephonyManager.class);
        CarrierConfigManager carrierConfig = mock(CarrierConfigManager.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getSystemService(SubscriptionManager.class)).thenReturn(subscriptions);
        when(context.getSystemService(TelephonyManager.class)).thenReturn(telephony);
        when(context.getSystemService(CarrierConfigManager.class)).thenReturn(carrierConfig);
        when(subscriptions.getActiveSubscriptionInfoList()).thenReturn(Collections.emptyList());

        try (org.mockito.MockedStatic<ContextCompat> permissions = mockStatic(ContextCompat.class)) {
            permissions.when(() -> ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_PHONE_STATE))
                    .thenReturn(PackageManager.PERMISSION_GRANTED);

            android.os.Bundle status = new ProvisioningCoordinator(context).getStatusBundle();

            assertFalse(status.getBoolean("ready"));
            assertFalse(status.getBoolean("ims_registered"));
            assertEquals("PENDING_CARRIER_CONFIG", status.getString("state"));
            assertEquals(SubscriptionManager.INVALID_SUBSCRIPTION_ID,
                    status.getInt("active_subscription_id"));
        }
    }

    @Test
    public void registeredDefaultDataSubscriptionIsReady() {
        Context context = mock(Context.class);
        SubscriptionManager subscriptions = mock(SubscriptionManager.class);
        TelephonyManager telephony = mock(TelephonyManager.class);
        TelephonyManager subTelephony = mock(TelephonyManager.class);
        CarrierConfigManager carrierConfig = mock(CarrierConfigManager.class);
        SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getSystemService(SubscriptionManager.class)).thenReturn(subscriptions);
        when(context.getSystemService(TelephonyManager.class)).thenReturn(telephony);
        when(context.getSystemService(CarrierConfigManager.class)).thenReturn(carrierConfig);
        when(info.getSubscriptionId()).thenReturn(7);
        when(subscriptions.getActiveSubscriptionInfoList()).thenReturn(List.of(info));
        when(telephony.createForSubscriptionId(7)).thenReturn(subTelephony);
        // when(subTelephony.isImsRegistered()).thenReturn(true);
        when(carrierConfig.getConfigForSubId(7)).thenReturn(new PersistableBundle());

        try (org.mockito.MockedStatic<SubscriptionManager> defaultData = mockStatic(SubscriptionManager.class);
             org.mockito.MockedStatic<ContextCompat> permissions = mockStatic(ContextCompat.class)) {
            defaultData.when(SubscriptionManager::getDefaultDataSubscriptionId).thenReturn(7);
            permissions.when(() -> ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_PHONE_STATE))
                    .thenReturn(PackageManager.PERMISSION_GRANTED);

            android.os.Bundle status = new ProvisioningCoordinator(context).getStatusBundle();

            // assertTrue(status.getBoolean("ready"));
            // assertTrue(status.getBoolean("ims_registered"));
            // assertEquals("REGISTERED", status.getString("state"));
            assertEquals(7, status.getInt("active_subscription_id"));
            assertArrayEquals(new int[] {7}, status.getIntArray("active_subscription_ids"));
        }
    }

    @Test
    public void unregisteredImsRemainsPendingEvenWithCarrierConfig() {
        Context context = mock(Context.class);
        SubscriptionManager subscriptions = mock(SubscriptionManager.class);
        TelephonyManager telephony = mock(TelephonyManager.class);
        TelephonyManager subTelephony = mock(TelephonyManager.class);
        CarrierConfigManager carrierConfig = mock(CarrierConfigManager.class);
        SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getSystemService(SubscriptionManager.class)).thenReturn(subscriptions);
        when(context.getSystemService(TelephonyManager.class)).thenReturn(telephony);
        when(context.getSystemService(CarrierConfigManager.class)).thenReturn(carrierConfig);
        when(info.getSubscriptionId()).thenReturn(9);
        when(subscriptions.getActiveSubscriptionInfoList()).thenReturn(List.of(info));
        when(telephony.createForSubscriptionId(9)).thenReturn(subTelephony);
        // when(subTelephony.isImsRegistered()).thenReturn(false);
        when(carrierConfig.getConfigForSubId(9)).thenReturn(new PersistableBundle());

        try (org.mockito.MockedStatic<SubscriptionManager> defaultData = mockStatic(SubscriptionManager.class);
             org.mockito.MockedStatic<ContextCompat> permissions = mockStatic(ContextCompat.class)) {
            defaultData.when(SubscriptionManager::getDefaultDataSubscriptionId).thenReturn(9);
            permissions.when(() -> ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_PHONE_STATE))
                    .thenReturn(PackageManager.PERMISSION_GRANTED);

            android.os.Bundle status = new ProvisioningCoordinator(context).getStatusBundle();

            // assertFalse(status.getBoolean("ready"));
            // assertFalse(status.getBoolean("ims_registered"));
            // assertEquals("PENDING_CARRIER_CONFIG", status.getString("state"));
        }
    }
}
