@file:RequiresApi(Build.VERSION_CODES.O)

package org.microg.gms.asterism.core

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.asterism.GetAsterismConsentRequest
import com.google.android.gms.asterism.asterismClient
import com.google.android.gms.asterism.getAsterismConsentResponse
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.constellation.core.ConstellationStateStore
import org.microg.gms.constellation.core.RpcClient
import org.microg.gms.constellation.core.authManager
import org.microg.gms.constellation.core.proto.AsterismClient
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.ConsentVersion
import org.microg.gms.constellation.core.proto.DeviceID
import org.microg.gms.constellation.core.proto.GetConsentRequest
import org.microg.gms.constellation.core.proto.GetConsentResponse
import org.microg.gms.constellation.core.proto.RequestHeader
import org.microg.gms.constellation.core.proto.RequestTrigger
import org.microg.gms.constellation.core.proto.builder.buildRequestContext
import org.microg.gms.constellation.core.proto.builder.invoke
import java.util.UUID

private const val ASTERISM_TAG = "GetAsterismConsent"
private const val PNVR_TAG = "GetIsPnvrDevice"

suspend fun handleGetAsterismConsent(
    context: Context,
    callbacks: IAsterismCallbacks,
    request: GetAsterismConsentRequest
) = withContext(Dispatchers.IO) {
    try {
        val authManager = context.authManager
        val buildContext = buildRequestContext(context, authManager)
        val response = RpcClient.phoneDeviceVerificationClient.GetConsent().execute(
            GetConsentRequest(
                device_id = DeviceID(context, buildContext.iidToken),
                header_ = RequestHeader(
                    context,
                    UUID.randomUUID().toString(),
                    buildContext,
                    "getConsent",
                    RequestTrigger.Type.CONSENT_API_TRIGGER
                ),
                asterism_client = request.asterismClient
            )
        )

        val (consentValue, consentVersion) = resolveAsterismConsent(
            response,
            request.asterismClient
        )

        callbacks.onConsentFetched(
            Status.SUCCESS,
            getAsterismConsentResponse(
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
            getAsterismConsentResponse(
                request.requestCode,
                Consent.CONSENT_UNKNOWN,
                null,
                null,
                ConsentVersion.CONSENT_VERSION_UNSPECIFIED
            )
        )
    }
}

internal fun resolveAsterismConsent(
    response: GetConsentResponse,
    asterismClient: AsterismClient
): Pair<Consent, ConsentVersion> {
    response.gaia_consents.firstOrNull {
        it.asterism_client == asterismClient
    }?.let {
        return it.consent to it.consent_version
    }

    if (asterismClient == AsterismClient.RCS) {
        response.rcs_consent?.takeIf { it.consent != Consent.CONSENT_UNKNOWN }?.let {
            return it.consent to it.consent_version
        }
    }

    return Consent.NO_CONSENT to ConsentVersion.CONSENT_VERSION_UNSPECIFIED
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
