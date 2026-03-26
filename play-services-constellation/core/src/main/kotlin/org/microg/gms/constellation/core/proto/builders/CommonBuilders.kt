package org.microg.gms.constellation.core.proto.builders

import android.content.Context
import android.os.Bundle
import okio.ByteString.Companion.toByteString
import org.microg.gms.constellation.core.AuthManager
import org.microg.gms.constellation.core.proto.AuditToken
import org.microg.gms.constellation.core.proto.AuditTokenMetadata
import org.microg.gms.constellation.core.proto.AuditUuid
import org.microg.gms.constellation.core.proto.ClientInfo
import org.microg.gms.constellation.core.proto.DeviceID
import org.microg.gms.constellation.core.proto.Param
import org.microg.gms.constellation.core.proto.RequestHeader
import org.microg.gms.constellation.core.proto.RequestTrigger

fun Param.Companion.getList(extras: Bundle?): List<Param> {
    if (extras == null) return emptyList()
    val params = mutableListOf<Param>()
    val ignoreKeys = setOf("consent_variant_key", "consent_trigger_key", "gaia_access_token")
    for (key in extras.keySet()) {
        if (key !in ignoreKeys) {
            extras.getString(key)?.let { value ->
                params.add(Param(key = key, value_ = value))
            }
        }
    }
    return params
}

fun AuditToken.Companion.generate(): AuditToken {
    val uuid = java.util.UUID.randomUUID()
    return AuditToken(
        metadata = AuditTokenMetadata(
            uuid = AuditUuid(
                uuid_msb = uuid.mostSignificantBits,
                uuid_lsb = uuid.leastSignificantBits
            )
        )
    )
}

suspend operator fun RequestHeader.Companion.invoke(
    context: Context,
    sessionId: String,
    buildContext: RequestBuildContext,
    triggerType: RequestTrigger.Type = RequestTrigger.Type.CONSENT_API_TRIGGER,
    includeClientAuth: Boolean = false
): RequestHeader {
    val authManager = if (includeClientAuth) AuthManager.get(context) else null
    val clientAuth = if (includeClientAuth) {
        val (signature, timestamp) = authManager!!.signIidToken(buildContext.iidToken)
        org.microg.gms.constellation.core.proto.ClientAuth(
            device_id = DeviceID(context, buildContext.iidToken),
            signature = signature.toByteString(),
            sign_timestamp = timestamp
        )
    } else {
        null
    }

    return RequestHeader(
        client_info = ClientInfo(context, buildContext),
        client_auth = clientAuth,
        session_id = sessionId,
        trigger = RequestTrigger(type = triggerType)
    )
}
