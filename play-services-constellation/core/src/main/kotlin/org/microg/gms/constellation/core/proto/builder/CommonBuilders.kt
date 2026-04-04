package org.microg.gms.constellation.core.proto.builder

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import okio.ByteString.Companion.toByteString
import org.microg.gms.constellation.core.authManager
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

@RequiresApi(Build.VERSION_CODES.O)
suspend operator fun RequestHeader.Companion.invoke(
    context: Context,
    sessionId: String,
    buildContext: RequestBuildContext,
    rpc: String,
    triggerType: RequestTrigger.Type = RequestTrigger.Type.TRIGGER_API_CALL,
    includeClientAuth: Boolean = false,
): RequestHeader {
    val authManager = if (includeClientAuth) context.authManager else null
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
        client_info = ClientInfo(context, buildContext, rpc),
        client_auth = clientAuth,
        session_id = sessionId,
        trigger = RequestTrigger(type = triggerType)
    )
}
