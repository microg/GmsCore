/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.IBinder;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.microg.gms.BaseService;
import org.microg.gms.carrierauth.CarrierAuthService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class RcsAndCarrierAuthServiceProvisioningTest {
    private static final String MESSAGE_PACKAGE = "com.google.android.apps.messaging";
    private static final String MESSAGE_SHA1 = "24bb24c05e47e0aefa68a58a766179d9b613a600";

    @Test
    public void rcsReturnsSuccessWithBinderWhenProvisioningComplete() throws Exception {
        assertProvisionedResponse(org.robolectric.Robolectric.setupService(RcsService.class), GmsService.RCS);
    }

    @Test
    public void carrierAuthReturnsSuccessWithBinderWhenProvisioningComplete() throws Exception {
        assertProvisionedResponse(org.robolectric.Robolectric.setupService(CarrierAuthService.class), GmsService.CARRIER_AUTH);
    }

    @Test
    public void rcsReturnsApiUnavailableWhenProvisioningNotComplete() throws Exception {
        assertNotProvisionedResponse(org.robolectric.Robolectric.setupService(RcsService.class), GmsService.RCS);
    }

    @Test
    public void carrierAuthReturnsApiUnavailableWhenProvisioningNotComplete() throws Exception {
        assertNotProvisionedResponse(org.robolectric.Robolectric.setupService(CarrierAuthService.class), GmsService.CARRIER_AUTH);
    }

    private void assertProvisionedResponse(BaseService service, GmsService expectedService) throws Exception {
        Context baseContext = mock(Context.class);
        Context appContext = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        TelephonyManager telephonyManager = mock(TelephonyManager.class);
        SubscriptionManager subscriptionManager = mock(SubscriptionManager.class);

        when(baseContext.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageManager()).thenReturn(packageManager);
        when(appContext.getSystemService(TelephonyManager.class)).thenReturn(telephonyManager);
        when(appContext.getSystemService(SubscriptionManager.class)).thenReturn(subscriptionManager);

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.signatures = new Signature[] { new Signature(new byte[] {1, 2, 3}) };
        when(packageManager.getPackageInfo(MESSAGE_PACKAGE, PackageManager.GET_SIGNATURES)).thenReturn(packageInfo);

        try (org.mockito.MockedStatic<PackageUtils> packageUtils = mockStatic(PackageUtils.class)) {
            packageUtils.when(() -> PackageUtils.checkPackageUid(any(Context.class), eq(MESSAGE_PACKAGE), eq(0))).thenCallRealMethod();
            packageUtils.when(() -> PackageUtils.firstSignatureDigest(any(Context.class), eq(MESSAGE_PACKAGE), eq(true))).thenReturn(MESSAGE_SHA1);
            packageUtils.when(() -> PackageUtils.firstSignatureDigest(any(Context.class), eq(MESSAGE_PACKAGE), eq(false))).thenReturn(MESSAGE_SHA1);

            ProvisioningCoordinator provisioningCoordinator = mock(ProvisioningCoordinator.class);
            when(provisioningCoordinator.getState()).thenReturn(ProvisioningState.COMPLETE);
            setField(service, "provisioningCoordinator", provisioningCoordinator);

            GetServiceRequest request = new GetServiceRequest(expectedService.SERVICE_ID);
            request.packageName = MESSAGE_PACKAGE;

            IGmsCallbacks callbacks = mock(IGmsCallbacks.class);
            service.handleServiceRequest(callbacks, request, expectedService);

            verify(callbacks).onPostInitComplete(eq(ConnectionResult.SUCCESS), any(IBinder.class), org.mockito.ArgumentMatchers.isNull());
        }
    }

    private void assertNotProvisionedResponse(BaseService service, GmsService expectedService) throws Exception {
        Context baseContext = mock(Context.class);
        Context appContext = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        TelephonyManager telephonyManager = mock(TelephonyManager.class);
        SubscriptionManager subscriptionManager = mock(SubscriptionManager.class);

        when(baseContext.getApplicationContext()).thenReturn(appContext);
        when(appContext.getPackageManager()).thenReturn(packageManager);
        when(appContext.getSystemService(TelephonyManager.class)).thenReturn(telephonyManager);
        when(appContext.getSystemService(SubscriptionManager.class)).thenReturn(subscriptionManager);

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.signatures = new Signature[] { new Signature(new byte[] {1, 2, 3}) };
        when(packageManager.getPackageInfo(MESSAGE_PACKAGE, PackageManager.GET_SIGNATURES)).thenReturn(packageInfo);

        try (org.mockito.MockedStatic<PackageUtils> packageUtils = mockStatic(PackageUtils.class)) {
            packageUtils.when(() -> PackageUtils.checkPackageUid(any(Context.class), eq(MESSAGE_PACKAGE), eq(0))).thenCallRealMethod();
            packageUtils.when(() -> PackageUtils.firstSignatureDigest(any(Context.class), eq(MESSAGE_PACKAGE), eq(true))).thenReturn(MESSAGE_SHA1);
            packageUtils.when(() -> PackageUtils.firstSignatureDigest(any(Context.class), eq(MESSAGE_PACKAGE), eq(false))).thenReturn(MESSAGE_SHA1);

            ProvisioningCoordinator provisioningCoordinator = mock(ProvisioningCoordinator.class);
            when(provisioningCoordinator.getState()).thenReturn(ProvisioningState.IDLE);
            setField(service, "provisioningCoordinator", provisioningCoordinator);

            GetServiceRequest request = new GetServiceRequest(expectedService.SERVICE_ID);
            request.packageName = MESSAGE_PACKAGE;

            IGmsCallbacks callbacks = mock(IGmsCallbacks.class);
            service.handleServiceRequest(callbacks, request, expectedService);

            verify(callbacks).onPostInitComplete(eq(ConnectionResult.API_UNAVAILABLE), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull());
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
