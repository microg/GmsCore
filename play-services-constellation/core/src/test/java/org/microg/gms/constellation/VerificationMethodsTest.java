/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

import com.google.android.gms.constellation.GetPnvCapabilitiesResponse;
import com.google.android.gms.constellation.VerificationStatus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VerificationMethodsTest {
    @Test
    public void toClientMethodMapsTs43ToNine() {
        assertEquals(VerificationMethods.METHOD_TS43,
                VerificationMethods.toClientMethod(VerificationMethods.SERVER_METHOD_TS43));
    }

    @Test
    public void toClientMethodKeepsOtherMethods() {
        assertEquals(VerificationMethods.METHOD_MO_SMS,
                VerificationMethods.toClientMethod(VerificationMethods.SERVER_METHOD_MO_SMS));
        assertEquals(VerificationMethods.METHOD_MT_SMS,
                VerificationMethods.toClientMethod(VerificationMethods.SERVER_METHOD_MT_SMS));
        assertEquals(VerificationMethods.METHOD_CARRIER_ID,
                VerificationMethods.toClientMethod(VerificationMethods.SERVER_METHOD_CARRIER_ID));
        assertEquals(VerificationMethods.METHOD_IMSI_LOOKUP,
                VerificationMethods.toClientMethod(VerificationMethods.SERVER_METHOD_IMSI_LOOKUP));
        assertEquals(VerificationMethods.METHOD_REGISTERED_SMS,
                VerificationMethods.toClientMethod(VerificationMethods.SERVER_METHOD_REGISTERED_SMS));
        assertEquals(VerificationMethods.METHOD_FLASH_CALL,
                VerificationMethods.toClientMethod(VerificationMethods.SERVER_METHOD_FLASH_CALL));
    }

    @Test
    public void toClientMethodUnknownMapsToZero() {
        assertEquals(VerificationMethods.METHOD_UNKNOWN, VerificationMethods.toClientMethod(0));
        assertEquals(VerificationMethods.METHOD_UNKNOWN, VerificationMethods.toClientMethod(42));
    }

    @Test
    public void onlySmsMethodsAreSupportedLocally() {
        assertTrue(VerificationMethods.isSupportedLocally(VerificationMethods.METHOD_MO_SMS));
        assertTrue(VerificationMethods.isSupportedLocally(VerificationMethods.METHOD_MT_SMS));
        assertFalse(VerificationMethods.isSupportedLocally(VerificationMethods.METHOD_CARRIER_ID));
        assertFalse(VerificationMethods.isSupportedLocally(VerificationMethods.METHOD_IMSI_LOOKUP));
        assertFalse(VerificationMethods.isSupportedLocally(VerificationMethods.METHOD_REGISTERED_SMS));
        assertFalse(VerificationMethods.isSupportedLocally(VerificationMethods.METHOD_FLASH_CALL));
        assertFalse(VerificationMethods.isSupportedLocally(VerificationMethods.METHOD_TS43));
    }

    @Test
    public void verificationStatusFromInt() {
        assertEquals(VerificationStatus.SUPPORTED, VerificationStatus.fromInt(1));
        assertEquals(VerificationStatus.UNSUPPORTED_CARRIER, VerificationStatus.fromInt(2));
        assertEquals(VerificationStatus.UNSUPPORTED_API_VERSION, VerificationStatus.fromInt(3));
        assertEquals(VerificationStatus.UNSUPPORTED_SIM_NOT_READY, VerificationStatus.fromInt(4));
        assertEquals(VerificationStatus.SUPPORTED, VerificationStatus.fromInt(99));
    }

    // Note: VerificationStatus.verificationCapability() is not unit-tested here:
    // instantiating a SafeParcelable needs the Creator the safe-parcel annotation
    // processor generates at AGP build time, which a plain JVM run does not have.
    // It is a one-line constructor call; the build verifies it compiles.

    @Test
    public void signedContentFormat() {
        assertEquals("token:1700000000:123000000",
                ConstellationAuth.buildSignedContent("token", 1700000000L, 123000000L));
    }
}
