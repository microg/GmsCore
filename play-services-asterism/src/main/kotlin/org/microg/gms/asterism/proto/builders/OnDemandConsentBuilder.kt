package org.microg.gms.asterism.rpc.builders

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.asterism.SetAsterismConsentRequest
import org.microg.gms.constellation.proto.Consent
import org.microg.gms.constellation.proto.GaiaToken
import org.microg.gms.constellation.proto.OnDemandConsent

private const val ODC_TAG = "OnDemandConsent"

@SuppressLint("MissingPermission")
operator fun OnDemandConsent.Companion.invoke(
    context: Context,
    request: SetAsterismConsentRequest,
    extras: Bundle?
): OnDemandConsent? {
    val isOnDemand = request.status == SetAsterismConsentRequest.Status.ON_DEMAND ||
            extras?.containsKey("consent_variant_key") == true ||
            extras?.containsKey("consent_trigger_key") == true ||
            extras?.containsKey("gaia_access_token") == true

    if (!isOnDemand) return null

    val consentVariant = extras?.getString("consent_variant_key")
        ?: request.consentVariant

    val consentTrigger = extras?.getString("consent_trigger_key")
        ?: request.consentTrigger

    var gaiaAccessToken = extras?.getString("gaia_access_token")

    if (gaiaAccessToken.isNullOrBlank()) {
        val accountName = request.accountName
        if (!accountName.isNullOrBlank()) {
            gaiaAccessToken = try {
                val account = Account(accountName, "com.google")
                val accountManager = AccountManager.get(context)
                val future = accountManager.getAuthToken(
                    account, "oauth2:https://www.googleapis.com/auth/numberer",
                    null, false, null, null
                )
                future.result?.getString(AccountManager.KEY_AUTHTOKEN)
            } catch (e: Exception) {
                Log.w(ODC_TAG, "Failed to get auth token for account $accountName: ${e.message}")
                null
            }
        }
    }

    if (gaiaAccessToken.isNullOrBlank()) {
        Log.w(ODC_TAG, "ODC missing gaiaAccessToken")
        return null
    }

    val consentValue = if (request.consent == Consent.CONSENTED) {
        Consent.CONSENTED
    } else {
        Consent.NO_CONSENT
    }

    return OnDemandConsent(
        consent = consentValue,
        gaia_token = GaiaToken(token = gaiaAccessToken),
        consent_variant = consentVariant ?: "",
        trigger = consentTrigger ?: ""
    )
}
