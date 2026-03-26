package org.microg.gms.constellation.core.proto.builders

import android.content.Context
import org.microg.gms.constellation.core.AuthManager
import org.microg.gms.constellation.core.authManager
import org.microg.gms.constellation.core.proto.GaiaToken

data class RequestBuildContext(
    val iidToken: String,
    val gaiaTokens: List<GaiaToken>
)

suspend fun buildRequestContext(
    context: Context,
    authManager: AuthManager = context.authManager
): RequestBuildContext {
    return RequestBuildContext(
        iidToken = authManager.getIidToken(),
        gaiaTokens = GaiaToken.getList(context)
    )
}
