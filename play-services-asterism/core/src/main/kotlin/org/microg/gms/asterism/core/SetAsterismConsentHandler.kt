@file:RequiresApi(Build.VERSION_CODES.O)

package org.microg.gms.asterism.core

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.asterism.SetAsterismConsentRequest
import com.google.android.gms.asterism.SetAsterismConsentRequestStatus
import com.google.android.gms.asterism.SetAsterismConsentResponse
import com.google.android.gms.asterism.asterismClient
import com.google.android.gms.asterism.consent
import com.google.android.gms.asterism.deviceConsentSource
import com.google.android.gms.asterism.deviceConsentVersion
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import com.google.android.gms.asterism.isDevicePnvrFlow
import com.google.android.gms.asterism.rcsFlowContext
import com.google.android.gms.asterism.status
import com.google.android.gms.common.api.Status
import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.microg.gms.constellation.core.ConstellationStateStore
import org.microg.gms.constellation.core.RpcClient
import org.microg.gms.constellation.core.authManager
import org.microg.gms.constellation.core.proto.AsterismClient
import org.microg.gms.constellation.core.proto.AsterismConsent
import org.microg.gms.constellation.core.proto.AuditToken
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.ConsentVersion
import org.microg.gms.constellation.core.proto.FlowContext
import org.microg.gms.constellation.core.proto.OnDemandConsent
import org.microg.gms.constellation.core.proto.Param
import org.microg.gms.constellation.core.proto.RcsConsent
import org.microg.gms.constellation.core.proto.RequestHeader
import org.microg.gms.constellation.core.proto.RequestTrigger
import org.microg.gms.constellation.core.proto.SetConsentRequest
import org.microg.gms.constellation.core.proto.builder.buildRequestContext
import org.microg.gms.constellation.core.proto.builder.generate
import org.microg.gms.constellation.core.proto.builder.getList
import org.microg.gms.constellation.core.proto.builder.invoke
import java.util.UUID

private const val TAG = "SetAsterismConsent"

suspend fun handleSetAsterismConsent(
    context: Context,
    callbacks: IAsterismCallbacks,
    request: SetAsterismConsentRequest
) = withContext(Dispatchers.IO) {
    try {
        if (request.consent == Consent.CONSENT_UNKNOWN) {
            Log.e(TAG, "Invalid consentValue: ${request.consentValue}")
            callbacks.onConsentRegistered(
                Status.INTERNAL_ERROR,
                SetAsterismConsentResponse(request.requestCode, "", "")
            )
            return@withContext
        }

        val isRcsSpecificContext = request.rcsFlowContext in listOf(
            FlowContext.FLOW_CONTEXT_RCS_CONSENT,
            FlowContext.FLOW_CONTEXT_RCS_DEFAULT_ON_OUT_OF_BOX,
            FlowContext.FLOW_CONTEXT_RCS_SAMSUNG_UNFREEZE
        )

        if (request.asterismClient != AsterismClient.RCS && isRcsSpecificContext) {
            Log.e(TAG, "RCS-only flow context cannot be used with non-RCS client")
            callbacks.onConsentRegistered(
                Status.INTERNAL_ERROR,
                SetAsterismConsentResponse(request.requestCode, "", "")
            )
            return@withContext
        }

        if (request.status == SetAsterismConsentRequestStatus.ON_DEMAND) {
            if (request.accountName.isNullOrBlank() && request.extras?.getString("gaia_access_token")
                    .isNullOrBlank()
            ) {
                Log.e(TAG, "ODC missing accountName and no gaia_access_token in extras")
                callbacks.onConsentRegistered(
                    Status.INTERNAL_ERROR,
                    SetAsterismConsentResponse(request.requestCode, "", "")
                )
                return@withContext
            }
        }

        val apiParams = Param.getList(request.extras)

        var deviceConsent: AsterismConsent? = null
        var rcsConsent: RcsConsent? = null
        var onDemandConsent: OnDemandConsent? = null
        var flowContext: FlowContext = FlowContext.FLOW_CONTEXT_UNSPECIFIED

        when {
            request.isDevicePnvrFlow() -> {
                deviceConsent = AsterismConsent(
                    consent = request.consent,
                    consent_version = request.deviceConsentVersion,
                    consent_source = request.deviceConsentSource
                )
            }

            request.status == SetAsterismConsentRequestStatus.ON_DEMAND -> {
                onDemandConsent = OnDemandConsent(context, request, request.extras)
                if (onDemandConsent == null) {
                    Log.e(TAG, "ODC Flow missing required fields, aborting.")
                    callbacks.onConsentRegistered(
                        Status.CANCELED,
                        SetAsterismConsentResponse(request.requestCode, "", "")
                    )
                    return@withContext
                }
            }

            else -> {
                if (request.asterismClient == AsterismClient.CONSTELLATION) {
                    if (request.language.isNullOrBlank() || request.consentVariant.isNullOrBlank()) {
                        Log.w(
                            TAG,
                            "CONSTELLATION client requires language and consentVariant"
                        )
                        Log.w(
                            TAG,
                            " language=${request.language}, consentVariant=${request.consentVariant}"
                        )
                        callbacks.onConsentRegistered(
                            Status.INTERNAL_ERROR,
                            SetAsterismConsentResponse(request.requestCode, "", "")
                        )
                        return@withContext
                    }
                }

                val rcsConsentVersion = if (request.consent == Consent.CONSENTED) {
                    ConsentVersion.RCS_CONSENT
                } else {
                    ConsentVersion.RCS_OUT_OF_BOX
                }

                rcsConsent = RcsConsent(
                    consent = request.consent,
                    consent_version = rcsConsentVersion
                )

                flowContext = request.rcsFlowContext
            }
        }

        val needsAuditRecord = deviceConsent != null || onDemandConsent != null
        val auditRecordBytes = if (needsAuditRecord) {
            AuditToken.generate().encode().toByteString()
        } else ByteString.EMPTY

        val authManager = context.authManager
        val buildContext = buildRequestContext(context, authManager)
        val sessionId = UUID.randomUUID().toString()

        val protoRequest = SetConsentRequest(
            header_ = RequestHeader(
                context,
                sessionId,
                buildContext,
                "setConsent",
                RequestTrigger.Type.CONSENT_API_TRIGGER
            ),
            asterism_client = request.asterismClient,
            device_consent = deviceConsent,
            rcs_consent = rcsConsent,
            on_demand_consent = onDemandConsent,
            flow_context = flowContext,
            api_params = apiParams,
            audit_record = auditRecordBytes
        )

        try {
            RpcClient.phoneDeviceVerificationClient.SetConsent().execute(protoRequest)
        } catch (e: GrpcException) {
            if (e.grpcStatus == GrpcStatus.PERMISSION_DENIED ||
                e.grpcStatus == GrpcStatus.UNAUTHENTICATED
            ) {
                Log.w(
                    TAG,
                    "Suspicious client status ${e.grpcStatus.name}. Clearing DroidGuard cache..."
                )
                ConstellationStateStore.clearDroidGuardToken(context)
            }
            throw e
        }

        if (deviceConsent?.consent_version == ConsentVersion.PHONE_VERIFICATION_REACHABILITY_INTL_SMS_CALLS) {
            ConstellationStateStore.storePnvrNotice(
                context,
                deviceConsent.consent,
                deviceConsent.consent_source,
                deviceConsent.consent_version
            )
        }

        callbacks.onConsentRegistered(
            Status.SUCCESS,
            SetAsterismConsentResponse(
                request.requestCode,
                buildContext.iidToken,
                authManager.getFid()
            )
        )

    } catch (e: Exception) {
        Log.e(TAG, "setAsterismConsent failed", e)
        callbacks.onConsentRegistered(
            Status.INTERNAL_ERROR,
            SetAsterismConsentResponse(request.requestCode, "", "")
        )
    }
}
