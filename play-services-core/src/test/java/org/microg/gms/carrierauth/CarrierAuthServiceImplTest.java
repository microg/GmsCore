/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.carrierauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.TelephonyManager;

import com.google.android.gms.carrierauth.EAPAKARequest;
import com.google.android.gms.carrierauth.EAPAKAResponse;
import com.google.android.gms.carrierauth.EapInfoRequest;
import com.google.android.gms.carrierauth.EapInfoResponse;
import com.google.android.gms.carrierauth.internal.ICarrierAuthCallbacks;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.S}, shadows = {ShadowTelephonyManager.class})
public class CarrierAuthServiceImplTest {
    private Context context;
    private TelephonyManager telephony;
    private TelephonyManager subscriptionTelephony;
    private ICarrierAuthCallbacks callback;
    private CarrierAuthServiceImpl service;

    @Before
    public void setUp() {
        context = mock(Context.class);
        telephony = mock(TelephonyManager.class);
        subscriptionTelephony = mock(TelephonyManager.class);
        callback = mock(ICarrierAuthCallbacks.class);

        when(context.getApplicationContext()).thenReturn(context);
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephony);
        when(telephony.createForSubscriptionId(7)).thenReturn(subscriptionTelephony);
        service = new CarrierAuthServiceImpl(context);
    }

    @Test
    public void eapAkaRequestRoundTripsThroughSafeParcel() {
        EAPAKARequest original = new EAPAKARequest("challenge", 2, 5, 7, 11);

        EAPAKARequest restored = roundTrip(original, EAPAKARequest.CREATOR);

        assertEquals(original.a, restored.a);
        assertEquals(original.b, restored.b);
        assertEquals(original.c, restored.c);
        assertEquals(original.d, restored.d);
        assertEquals(original.e, restored.e);
    }

    @Test
    public void eapAkaResponseRoundTripsThroughSafeParcel() {
        EAPAKAResponse original = new EAPAKAResponse("modem-response");

        EAPAKAResponse restored = roundTrip(original, EAPAKAResponse.CREATOR);

        assertEquals("modem-response", restored.a);
    }

    @Test
    public void emptyRequestsRoundTripThroughSafeParcel() {
        assertNotNull(roundTrip(new EapInfoRequest(), EapInfoRequest.CREATOR));
        assertNotNull(roundTrip(new EapInfoResponse(), EapInfoResponse.CREATOR));
    }

    @Test
    public void validRequestUsesDefaultsAndDispatchesModemResponse() throws Exception {
        when(subscriptionTelephony.hasCarrierPrivileges()).thenReturn(true);
        when(subscriptionTelephony.getIccAuthentication(
                TelephonyManager.APPTYPE_USIM,
                TelephonyManager.AUTHTYPE_EAP_AKA,
                "challenge"))
                .thenReturn("modem-response");

        service.performEAPAKA(
                callback,
                new EAPAKARequest("challenge", null, null, null, 7),
                null);

        verify(telephony).createForSubscriptionId(7);
        verify(subscriptionTelephony).getIccAuthentication(
                TelephonyManager.APPTYPE_USIM,
                TelephonyManager.AUTHTYPE_EAP_AKA,
                "challenge");

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        ArgumentCaptor<EAPAKAResponse> responseCaptor =
                ArgumentCaptor.forClass(EAPAKAResponse.class);
        verify(callback).onEAPAKAResponse(
                statusCaptor.capture(),
                responseCaptor.capture(),
                eq(null));
        assertSame(Status.SUCCESS, statusCaptor.getValue());
        assertEquals("modem-response", responseCaptor.getValue().a);
    }

    @Test
    public void testPerformEAPAKA_VirtualSuccess() throws Exception {
        Context application = RuntimeEnvironment.getApplication();
        CarrierAuthServiceImpl virtualService = new CarrierAuthServiceImpl(application);
        ShadowTelephonyManager.setCarrierPrivileges(7, true);
        ShadowTelephonyManager.setMockAuthResponse(
                TelephonyManager.APPTYPE_USIM,
                "virtual-auth-response");

        virtualService.performEAPAKA(
                callback,
                new EAPAKARequest("virtual-challenge", null, null, null, 7),
                null);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        ArgumentCaptor<EAPAKAResponse> responseCaptor =
                ArgumentCaptor.forClass(EAPAKAResponse.class);
        verify(callback).onEAPAKAResponse(
                statusCaptor.capture(),
                responseCaptor.capture(),
                eq(null));
        assertSame(Status.SUCCESS, statusCaptor.getValue());
        assertEquals("virtual-auth-response", responseCaptor.getValue().a);
    }

    @Test
    public void missingCarrierPrivilegesFailsClosed() throws Exception {
        when(subscriptionTelephony.hasCarrierPrivileges()).thenReturn(false);

        service.performEAPAKA(
                callback,
                new EAPAKARequest("challenge", null, null, null, 7),
                null);

        verify(subscriptionTelephony).hasCarrierPrivileges();
        verify(subscriptionTelephony, org.mockito.Mockito.never())
                .getIccAuthentication(
                        eq(TelephonyManager.APPTYPE_USIM),
                        eq(TelephonyManager.AUTHTYPE_EAP_AKA),
                        eq("challenge"));
        assertFailureCallback();
    }

    @Test
    public void telephonySecurityExceptionFailsClosed() throws Exception {
        when(subscriptionTelephony.hasCarrierPrivileges()).thenReturn(true);
        when(subscriptionTelephony.getIccAuthentication(
                TelephonyManager.APPTYPE_USIM,
                TelephonyManager.AUTHTYPE_EAP_AKA,
                "challenge"))
                .thenThrow(new SecurityException("denied"));

        service.performEAPAKA(
                callback,
                new EAPAKARequest("challenge", null, null, null, 7),
                null);

        assertFailureCallback();
    }

    @Test
    public void nullRequestAndCallbackAreIgnored() throws Exception {
        service.performEAPAKA(null, null, null);
        service.performEAPAKA(callback, null, null);
        service.performEAPAKA(null, new EAPAKARequest("challenge", null, null, null, 7), null);

        verifyNoInteractions(callback, telephony, subscriptionTelephony);
    }

    @Test
    public void callbackRemoteExceptionDoesNotEscapeService() throws Exception {
        when(subscriptionTelephony.hasCarrierPrivileges()).thenReturn(true);
        when(subscriptionTelephony.getIccAuthentication(
                TelephonyManager.APPTYPE_USIM,
                TelephonyManager.AUTHTYPE_EAP_AKA,
                "challenge"))
                .thenReturn("modem-response");
        doThrow(new RemoteException("disconnected"))
                .when(callback)
                .onEAPAKAResponse(
                        org.mockito.ArgumentMatchers.any(Status.class),
                        org.mockito.ArgumentMatchers.any(EAPAKAResponse.class),
                        eq(null));

        service.performEAPAKA(
                callback,
                new EAPAKARequest("challenge", null, null, null, 7),
                null);
    }

    @Test
    public void eapInfoCallbackReceivesStructuralResponse() throws Exception {
        service.getEapInfo(callback, new EapInfoRequest(), null);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        ArgumentCaptor<EapInfoResponse> responseCaptor =
                ArgumentCaptor.forClass(EapInfoResponse.class);
        verify(callback).onEapInfoResponse(
                statusCaptor.capture(),
                responseCaptor.capture(),
                eq(null));
        assertSame(Status.SUCCESS, statusCaptor.getValue());
        assertNotNull(responseCaptor.getValue());
    }

    private void assertFailureCallback() throws RemoteException {
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        ArgumentCaptor<EAPAKAResponse> responseCaptor =
                ArgumentCaptor.forClass(EAPAKAResponse.class);
        verify(callback).onEAPAKAResponse(
                statusCaptor.capture(),
                responseCaptor.capture(),
                eq(null));
        assertEquals(CommonStatusCodes.CANCELED, statusCaptor.getValue().getStatusCode());
        assertNull(responseCaptor.getValue());
    }

    private static <T extends android.os.Parcelable> T roundTrip(
            T value,
            android.os.Parcelable.Creator<T> creator) {
        Parcel parcel = Parcel.obtain();
        try {
            value.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return creator.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }
}
