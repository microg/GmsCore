/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism

import android.os.Bundle
import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.constellation.core.proto.AsterismClient
import org.microg.gms.constellation.core.proto.AsterismConsent.DeviceConsentVersion
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.ConsentSource
import org.microg.gms.constellation.core.proto.ConsentVersion
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AsterismConsentTest {

    @Test
    fun getConsentRequest_roundTripPreservesFields() {
        val original = GetAsterismConsentRequest(7, AsterismClient.CONSTELLATION.value)
        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = GetAsterismConsentRequest.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        assertEquals(7, restored.requestCode)
        assertEquals(AsterismClient.CONSTELLATION.value, restored.asterismClientValue)
        assertEquals(AsterismClient.CONSTELLATION, restored.asterismClient)
    }

    @Test
    fun getConsentResponse_roundTripPreservesFields() {
        val original = getAsterismConsentResponse(
            3,
            Consent.CONSENTED,
            "iid-token",
            "fid-value",
            ConsentVersion.RCS_CONSENT
        )
        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = GetAsterismConsentResponse.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        assertEquals(3, restored.requestCode)
        assertEquals(Consent.CONSENTED.value, restored.consentStateValue)
        assertEquals("iid-token", restored.gmscoreIidToken)
        assertEquals("fid-value", restored.fid)
        assertEquals(ConsentVersion.RCS_CONSENT.value, restored.consentTypeValue)
    }

    @Test
    fun getConsentResponse_mapsExpiredToNoConsent() {
        val response = getAsterismConsentResponse(
            1,
            Consent.EXPIRED,
            null,
            null,
            ConsentVersion.CONSENT_VERSION_UNSPECIFIED
        )

        assertEquals(Consent.NO_CONSENT.value, response.consentStateValue)
        assertNull(response.gmscoreIidToken)
        assertNull(response.fid)
    }

    @Test
    fun getConsentResponse_keepsUnknownAndConsented() {
        val unknown = getAsterismConsentResponse(
            1,
            Consent.CONSENT_UNKNOWN,
            "iid",
            "fid",
            ConsentVersion.CONSENT_VERSION_UNSPECIFIED
        )
        val consented = getAsterismConsentResponse(
            1,
            Consent.CONSENTED,
            "iid",
            "fid",
            ConsentVersion.RCS_CONSENT
        )

        assertEquals(Consent.CONSENT_UNKNOWN.value, unknown.consentStateValue)
        assertEquals(Consent.CONSENTED.value, consented.consentStateValue)
    }

    @Test
    fun setConsentRequest_roundTripPreservesFields() {
        val extras = Bundle().apply { putString("policy_id", "rcs") }
        val original = setConsentRequest(
            requestCode = 11,
            client = AsterismClient.CONSTELLATION.value,
            source = ConsentSource.ANDROID_DEVICE_SETTINGS.value,
            version = DeviceConsentVersion.PHONE_VERIFICATION_DEFAULT.value,
            consent = Consent.CONSENTED.value,
            extras = extras
        )
        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = SetAsterismConsentRequest.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        assertEquals(11, restored.requestCode)
        assertEquals(AsterismClient.CONSTELLATION.value, restored.asterismClientValue)
        assertEquals(Consent.CONSENTED.value, restored.consentValue)
        assertEquals(ConsentSource.ANDROID_DEVICE_SETTINGS.value, restored.deviceConsentSourceValue)
        assertEquals(
            DeviceConsentVersion.PHONE_VERIFICATION_DEFAULT.value,
            restored.deviceConsentVersionValue
        )
        assertEquals("rcs", restored.extras?.getString("policy_id"))
    }

    @Test
    fun setConsentResponse_roundTripPreservesFields() {
        val original = SetAsterismConsentResponse(4, "iid-token", "fid-value")
        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = SetAsterismConsentResponse.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        assertEquals(4, restored.requestCode)
        assertEquals("iid-token", restored.gmscoreIidToken)
        assertEquals("fid-value", restored.fid)
    }

    @Test
    fun isDevicePnvrFlow_requiresConstellationAndDeviceConsentFields() {
        assertTrue(
            setConsentRequest(
                client = AsterismClient.CONSTELLATION.value,
                source = ConsentSource.ANDROID_DEVICE_SETTINGS.value,
                version = DeviceConsentVersion.PHONE_VERIFICATION_DEFAULT.value
            ).isDevicePnvrFlow()
        )
        assertFalse(
            setConsentRequest(
                client = AsterismClient.RCS.value,
                source = ConsentSource.ANDROID_DEVICE_SETTINGS.value,
                version = DeviceConsentVersion.PHONE_VERIFICATION_DEFAULT.value
            ).isDevicePnvrFlow()
        )
        assertFalse(
            setConsentRequest(
                client = AsterismClient.CONSTELLATION.value,
                source = 0,
                version = DeviceConsentVersion.PHONE_VERIFICATION_DEFAULT.value
            ).isDevicePnvrFlow()
        )
        assertFalse(
            setConsentRequest(
                client = AsterismClient.CONSTELLATION.value,
                source = ConsentSource.ANDROID_DEVICE_SETTINGS.value,
                version = 0
            ).isDevicePnvrFlow()
        )
    }

    @Test
    fun deviceConsentVersion_defaultsUnknownToPhoneVerificationDefault() {
        val unknown = setConsentRequest(version = DeviceConsentVersion.UNKNOWN.value)
        val specified = setConsentRequest(
            version = DeviceConsentVersion.PHONE_VERIFICATION_REACHABILITY_INTL_SMS_CALLS.value
        )

        assertEquals(DeviceConsentVersion.PHONE_VERIFICATION_DEFAULT, unknown.deviceConsentVersion)
        assertEquals(
            DeviceConsentVersion.PHONE_VERIFICATION_REACHABILITY_INTL_SMS_CALLS,
            specified.deviceConsentVersion
        )
    }

    @Test
    fun status_fallsBackToResourceTosFallback() {
        assertEquals(
            SetAsterismConsentRequestStatus.RCS,
            SetAsterismConsentRequestStatus.fromValue(2)
        )
        assertEquals(
            SetAsterismConsentRequestStatus.RESOURCE_TOS_FALLBACK,
            SetAsterismConsentRequestStatus.fromValue(99)
        )
    }

    private fun setConsentRequest(
        requestCode: Int = 1,
        client: Int = AsterismClient.CONSTELLATION.value,
        source: Int = ConsentSource.ANDROID_DEVICE_SETTINGS.value,
        version: Int = DeviceConsentVersion.PHONE_VERIFICATION_DEFAULT.value,
        consent: Int = Consent.CONSENTED.value,
        extras: Bundle? = null
    ) = SetAsterismConsentRequest(
        requestCode,
        client,
        0,
        null,
        null,
        consent,
        extras,
        0,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        0,
        source,
        version
    )
}
