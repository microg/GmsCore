package org.microg.gms.constellation.core.proto.builder

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.asterism.SetAsterismConsentRequest
import com.google.android.gms.asterism.SetAsterismConsentRequestStatus
import com.google.android.gms.asterism.consent
import com.google.android.gms.asterism.status
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.GaiaToken
import org.microg.gms.constellation.core.proto.OnDemandConsent

private const val TAG = "OnDemandConsent"

@SuppressLint("MissingPermission")
operator fun OnDemandConsent.Companion.invoke(
    context: Context,
    request: SetAsterismConsentRequest,
    extras: Bundle?
): OnDemandConsent? {
    val isOnDemand = request.status == SetAsterismConsentRequestStatus.ON_DEMAND ||
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
                Log.w(TAG, "Failed to get auth token for account $accountName: ${e.message}")
                null
            }
        }
    }

    if (gaiaAccessToken.isNullOrBlank()) {
        Log.w(TAG, "ODC missing gaiaAccessToken")
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
