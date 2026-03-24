package org.microg.gms.asterism

import android.content.Context
import android.util.Log
import com.google.android.gms.asterism.GetAsterismConsentRequest
import com.google.android.gms.asterism.GetAsterismConsentResponse
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.constellation.AuthManager
import org.microg.gms.constellation.ConstellationStateStore
import org.microg.gms.constellation.RpcClient
import org.microg.gms.constellation.proto.Consent
import org.microg.gms.constellation.proto.ConsentVersion
import org.microg.gms.constellation.proto.DeviceID
import org.microg.gms.constellation.proto.GetConsentRequest
import org.microg.gms.constellation.proto.RequestHeader
import org.microg.gms.constellation.proto.builders.buildRequestContext
import org.microg.gms.constellation.proto.builders.invoke
import java.util.UUID

private const val ASTERISM_TAG = "GetAsterismConsent"
private const val PNVR_TAG = "GetIsPnvrDevice"

suspend fun handleGetAsterismConsent(
    context: Context,
    callbacks: IAsterismCallbacks,
    request: GetAsterismConsentRequest
) = withContext(Dispatchers.IO) {
    try {
        val authManager = AuthManager.get(context)
        val buildContext = buildRequestContext(context, authManager)
        val response = RpcClient.phoneDeviceVerificationClient.GetConsent().execute(
            GetConsentRequest(
                device_id = DeviceID(context, buildContext.iidToken),
                header_ = RequestHeader(context, UUID.randomUUID().toString(), buildContext),
                asterism_client = request.asterismClient
            )
        )

        val gaiaConsent = response.gaia_consents.find {
            it.asterism_client == request.asterismClient
        }
        val (consentValue, consentVersion) = if (gaiaConsent != null) {
            gaiaConsent.consent to gaiaConsent.consent_version
        } else {
            Consent.NO_CONSENT to ConsentVersion.CONSENT_VERSION_UNSPECIFIED
        }

        callbacks.onConsentFetched(
            Status.SUCCESS,
            GetAsterismConsentResponse(
                request.requestCode,
                consentValue,
                buildContext.iidToken,
                authManager.getFid(),
                consentVersion
            )
        )
    } catch (e: Exception) {
        Log.e(ASTERISM_TAG, "getAsterismConsent failed", e)
        callbacks.onConsentFetched(
            Status.INTERNAL_ERROR,
            GetAsterismConsentResponse(
                request.requestCode,
                Consent.CONSENT_UNKNOWN,
                null,
                null,
                ConsentVersion.CONSENT_VERSION_UNSPECIFIED
            )
        )
    }
}

suspend fun handleGetIsPnvrConstellationDevice(
    context: Context,
    callbacks: IAsterismCallbacks
) = withContext(Dispatchers.IO) {
    try {
        val consentValue = ConstellationStateStore.loadPnvrNoticeConsent(context)
        val isPnvrDevice = consentValue == Consent.CONSENTED || consentValue == Consent.NO_CONSENT

        callbacks.onIsPnvrConstellationDevice(Status.SUCCESS, isPnvrDevice)
    } catch (e: Exception) {
        Log.e(PNVR_TAG, "getIsPnvrConstellationDevice failed", e)
        callbacks.onIsPnvrConstellationDevice(Status.INTERNAL_ERROR, false)
    }
}
