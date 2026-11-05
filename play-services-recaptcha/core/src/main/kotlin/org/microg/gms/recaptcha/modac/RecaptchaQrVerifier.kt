/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import org.microg.gms.common.Constants
import org.microg.gms.recaptcha.modac.signals.DefaultSignalCollectionManager
import org.microg.gms.recaptcha.modac.signals.SignalCollectionManager
import org.microg.gms.recaptcha.modac.storage.RecaptchaCredentialStore

private const val TAG = "RecaptchaQr"
private const val DEFAULT_VERIFY_TIMEOUT_MS = 25_000L

internal const val MODAC_SITE_KEY = "6LfuGqkrAAAAAOcfldxrFN7IZFkus_70m0tcbHNa"

internal sealed interface VerificationOutcome {
    data object Verified : VerificationOutcome
    data class Failed(val cause: Throwable) : VerificationOutcome
}

internal object RecaptchaQrVerifier {
    @Volatile
    private var client: RecaptchaQrClient? = null

    @Synchronized
    fun init(
        context: Context,
        signalUpdateScope: CoroutineScope,
        siteKey: String = MODAC_SITE_KEY,
        packageName: String = Constants.GMS_PACKAGE_NAME,
        signalCollectionManager: SignalCollectionManager? = null,
    ) {
        val signalManager = signalCollectionManager ?: DefaultSignalCollectionManager(context)
        val workflow = DefaultRecaptchaClientWorkflow(
            context = context,
            protocolClient = RecaptchaProtocolClient(VolleyRecaptchaProtoTransport(context)),
            credentialStore = RecaptchaCredentialStore(context),
            signalUpdateScope = signalUpdateScope,
        )
        client = RecaptchaQrClient(
            workflow = workflow,
            siteKey = siteKey,
            session = RecaptchaSession(packageName),
            signalCollectionManager = signalManager,
        )
    }

    suspend fun verifyToken(qrToken: String): VerificationOutcome {
        val activeClient = client ?: return VerificationOutcome.Failed(
            RecaptchaError(
                RecaptchaErrorKind.INTERNAL,
                RecaptchaErrorSubKind.UNKNOWN,
                "verifier_not_initialized",
            ),
        )
        return try {
            activeClient.verify(RecaptchaAction.MODAC_VERIFY, qrToken, DEFAULT_VERIFY_TIMEOUT_MS)
            VerificationOutcome.Verified
        } catch (e: CancellationException) {
            throw e
        } catch (e: RecaptchaError) {
            Log.w(TAG, "Verification failed: kind=${e.kindCode}, subKind=${e.subKindCode}, label=${e.label}")
            VerificationOutcome.Failed(e)
        } catch (e: Exception) {
            Log.w(TAG, "Verification failed with an unexpected error", e)
            VerificationOutcome.Failed(
                RecaptchaError(
                    RecaptchaErrorKind.INTERNAL,
                    RecaptchaErrorSubKind.RUNTIME_ERROR,
                    "unexpected_error",
                    cause = e,
                ),
            )
        }
    }
}
