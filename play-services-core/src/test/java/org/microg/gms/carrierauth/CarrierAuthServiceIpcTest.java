/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.carrierauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

import android.os.Bundle;
import android.os.Parcel;
import android.os.IBinder;
import android.content.Context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.microg.gms.common.PackageUtils;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CarrierAuthServiceIpcTest {
    private static final String DESCRIPTOR =
            "com.google.android.gms.carrierauth.internal.ICarrierAuthService";

    @Test
    public void interfaceTransactionReturnsExplicitDescriptor() throws Exception {
        CarrierAuthService service = org.robolectric.Robolectric.setupService(CarrierAuthService.class);
        IBinder binder = getStub(service);
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            assertTrue(binder.transact(IBinder.INTERFACE_TRANSACTION, data, reply, 0));
            reply.setDataPosition(0);
            assertEquals(DESCRIPTOR, reply.readString());
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    /*
    @Test
    public void statusTransactionReturnsUnauthorizedBundleWithoutTelephonyAccess() throws Exception {
        CarrierAuthService service = org.robolectric.Robolectric.setupService(CarrierAuthService.class);
        IBinder binder = getStub(service);
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(DESCRIPTOR);

        try (org.mockito.MockedStatic<PackageUtils> packageUtils = mockStatic(PackageUtils.class)) {
            packageUtils.when(() -> PackageUtils.checkPackageUid(
                    any(Context.class),
                    eq("com.google.android.apps.messaging"),
                    eq(android.os.Binder.getCallingUid()))).thenAnswer(invocation -> null);
            packageUtils.when(() -> PackageUtils.firstSignatureDigest(
                    any(Context.class),
                    eq("com.google.android.apps.messaging"),
                    eq(true))).thenReturn("24bb24c05e47e0aefa68a58a766179d9b613a600");
            packageUtils.when(() -> PackageUtils.firstSignatureDigest(
                    any(Context.class),
                    eq("com.google.android.apps.messaging"),
                    eq(false))).thenReturn("24bb24c05e47e0aefa68a58a766179d9b613a600");

            assertTrue(binder.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0));
            reply.setDataPosition(0);
            reply.readException();
            assertEquals(1, reply.readInt());
            Bundle result = Bundle.CREATOR.createFromParcel(reply);
            assertNotNull(result);
            assertFalse(result.getBoolean("auth_success"));
            assertEquals(1, result.getInt("status_code"));
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
    */

    private IBinder getStub(CarrierAuthService service) throws Exception {
        Field field = CarrierAuthService.class.getDeclaredField("serviceStub");
        field.setAccessible(true);
        return (IBinder) field.get(service);
    }
}
