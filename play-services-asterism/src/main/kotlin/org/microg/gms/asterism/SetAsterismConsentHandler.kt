package org.microg.gms.asterism

import android.content.Context
import android.util.Log
import com.google.android.gms.asterism.SetAsterismConsentRequest
import com.google.android.gms.asterism.SetAsterismConsentResponse
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import com.google.android.gms.common.api.Status
import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.microg.gms.constellation.AuthManager
import org.microg.gms.constellation.proto.AsterismClient
import org.microg.gms.constellation.proto.AsterismConsent
import org.microg.gms.constellation.proto.AuditToken
import org.microg.gms.constellation.proto.Consent
import org.microg.gms.constellation.proto.ConsentVersion
import org.microg.gms.constellation.proto.FlowContext
import org.microg.gms.constellation.proto.OnDemandConsent
import org.microg.gms.constellation.proto.Param
import org.microg.gms.constellation.proto.RcsConsent
import org.microg.gms.constellation.proto.RequestHeader
import org.microg.gms.constellation.proto.RequestTrigger
import org.microg.gms.constellation.proto.SetConsentRequest
import org.microg.gms.constellation.proto.builders.*
import org.microg.gms.asterism.rpc.builders.invoke
import org.microg.gms.constellation.ConstellationStateStore
import org.microg.gms.constellation.RpcClient
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

        if (request.extras != null) {
            Log.d(TAG, "Extras keys: ${request.extras.keySet().joinToString()}")
            for (key in request.extras.keySet()) {
                val value = request.extras.get(key)
                Log.d(TAG, "  $key = $value")
            }
        }

        if (request.status == SetAsterismConsentRequest.Status.ON_DEMAND) {
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

        val extras = request.extras
        val apiParams = Param.getList(extras)

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
            request.status == SetAsterismConsentRequest.Status.ON_DEMAND -> {
                onDemandConsent = OnDemandConsent(context, request, extras)
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

        val authManager = AuthManager.get(context)
        val buildContext = buildRequestContext(context, authManager)
        val sessionId = UUID.randomUUID().toString()

        val protoRequest = SetConsentRequest(
            header_ = RequestHeader(
                context,
                sessionId,
                buildContext,
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
                Log.w(TAG, "Suspicious client status ${e.grpcStatus.name}. Clearing DroidGuard cache...")
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
            SetAsterismConsentResponse(request.requestCode, buildContext.iidToken, authManager.getFid())
        )

    } catch (e: Exception) {
        Log.e(TAG, "setAsterismConsent failed", e)
        callbacks.onConsentRegistered(
            Status.INTERNAL_ERROR,
            SetAsterismConsentResponse(request.requestCode, "", "")
        )
    }
}
