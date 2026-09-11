/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.microg.gms.common.Constants
import org.microg.gms.profile.Build
import org.microg.gms.recaptcha.modac.signals.SignalCollectionManager
import org.microg.gms.recaptcha.modac.signals.SignalCollectionRequest
import org.microg.gms.recaptcha.modac.storage.RecaptchaCredentialStore
import org.microg.gms.recaptcha.qr.CachedInitCredential
import org.microg.gms.recaptcha.qr.InitializationRequest
import org.microg.gms.recaptcha.qr.InitializationResponse
import org.microg.gms.recaptcha.qr.RecaptchaSignals
import org.microg.gms.recaptcha.qr.SignalUpdateRequest
import org.microg.gms.recaptcha.qr.VerificationRequest
import org.microg.gms.recaptcha.qr.VerificationResponse

private const val TAG = "RecaptchaQr"
private const val SIGNAL_UPDATE_TIMEOUT_MS = 10_000L
private const val RECAPTCHA_SDK_VERSION = "18.9.0-beta02"
private const val RECAPTCHA_BUILD_ID = "BQDhGLUE"
private const val CLIENT_TYPE_FIRST_PARTY = 1
private const val CLIENT_TYPE_OTHER = 2
private const val PERSISTENT_CREDENTIAL_KEY = "_GRECAPTCHA_KC"

private data class InitializationState(
    val response: InitializationResponse,
    val credential: CachedInitCredential?,
)

private class VerificationDeadline(timeoutMs: Long) {
    private val deadlineMs = SystemClock.elapsedRealtime() + timeoutMs

    fun remainingMs(): Long {
        val remaining = deadlineMs - SystemClock.elapsedRealtime()
        if (remaining <= 0L) {
            throw RecaptchaError(
                RecaptchaErrorKind.NETWORK,
                RecaptchaErrorSubKind.UNKNOWN,
                "verification_timeout",
            )
        }
        return remaining
    }
}

internal class DefaultRecaptchaClientWorkflow(
    context: Context,
    private val protocolClient: RecaptchaProtocolClient,
    private val credentialStore: RecaptchaCredentialStore,
    private val signalUpdateScope: CoroutineScope,
) : RecaptchaClientWorkflow {
    private val context = context.applicationContext
    private val initMutex = Mutex()
    private var sessionInitialization: InitializationState? = null

    override suspend fun execute(
        request: RecaptchaExecutionRequest,
        signalCollectionManager: SignalCollectionManager,
    ): String {
        val deadline = VerificationDeadline(request.timeoutMs)
        val initialization = getOrInitialize(request, deadline)
        val signals = signalCollectionManager.collectSignals(
            initialization.toSignalRequest(request.action.name, deadline.remainingMs()),
        )
        val verificationResponse = executeChallenge(
            request = request,
            initialization = initialization,
            signals = signals,
            timeoutMs = deadline.remainingMs(),
        )
        postExecute(
            verificationResponse = verificationResponse,
            siteKey = request.siteKey,
            action = request.action.name,
            initialization = initialization,
            signalCollectionManager = signalCollectionManager,
        )
        return verificationResponse.challengeToken
    }

    private suspend fun getOrInitialize(
        request: RecaptchaExecutionRequest,
        deadline: VerificationDeadline,
    ): InitializationState {
        val sessionResult = initMutex.withLock {
            sessionInitialization ?: initialize(request, deadline).also { sessionInitialization = it }
        }
        return sessionResult.copy(credential = credentialStore.read(request.siteKey))
    }

    private suspend fun initialize(
        request: RecaptchaExecutionRequest,
        deadline: VerificationDeadline,
    ): InitializationState {
        val isFirstParty = request.packageName == Constants.GMS_PACKAGE_NAME
        val baseRequest = InitializationRequest(
            siteKey = request.siteKey,
            packageName = request.packageName,
            sdkVersion = RECAPTCHA_SDK_VERSION,
            clientType = if (isFirstParty) CLIENT_TYPE_FIRST_PARTY else CLIENT_TYPE_OTHER,
            requestId = request.sessionRequestId,
            sdkInt = Build.VERSION.SDK_INT.toString(),
            initSignal = "",
            playServicesAvailable = isFirstParty,
            playStoreInstalled = isPackageInstalled(Constants.VENDING_PACKAGE_NAME),
            installerPackage = gmsInstallerPackage(),
            buildId = RECAPTCHA_BUILD_ID,
        )
        val cachedCredential = credentialStore.read(request.siteKey)
        val initializationRequest = baseRequest.copy(initSignal = cachedCredential?.credential.orEmpty())
        val initializationResponse = protocolClient.initialize(initializationRequest, deadline.remainingMs())
        return InitializationState(initializationResponse, cachedCredential)
    }

    private suspend fun executeChallenge(
        request: RecaptchaExecutionRequest,
        initialization: InitializationState,
        signals: RecaptchaSignals,
        timeoutMs: Long,
    ): VerificationResponse {
        val response = protocolClient.verify(
            VerificationRequest(
                nonce = initialization.response.nonce,
                landingToken = initialization.response.landingToken,
                siteKey = request.siteKey,
                action = request.action.name,
                signals = signals,
                qrToken = request.qrToken,
            ),
            timeoutMs,
        )
        if (response.challengeToken.isEmpty()) {
            throw RecaptchaError(
                RecaptchaErrorKind.INTERNAL,
                RecaptchaErrorSubKind.EMPTY_CHALLENGE_TOKEN,
                "verification_rejected",
                "errorCode=${response.errorCode}",
            )
        }
        return response
    }

    private suspend fun postExecute(
        verificationResponse: VerificationResponse,
        siteKey: String,
        action: String,
        initialization: InitializationState,
        signalCollectionManager: SignalCollectionManager,
    ) {
        val kcEntry = verificationResponse.persistentStorage.firstOrNull { it.name == PERSISTENT_CREDENTIAL_KEY }
            ?: return
        if (kcEntry.storedValue.isEmpty()) return

        val refreshedCredential = CachedInitCredential(
            credential = kcEntry.storedValue,
            nonceUuid = verificationResponse.nonceUuid,
            droidGuardNonce = verificationResponse.droidGuardNonce,
        )
        val stored = credentialStore.write(siteKey, refreshedCredential)
        if (stored && refreshedCredential.credential != initialization.credential?.credential) {
            scheduleSignalUpdate(
                siteKey = siteKey,
                action = action,
                initialization = initialization,
                credential = refreshedCredential,
                signalCollectionManager = signalCollectionManager,
            )
        }
    }

    private fun scheduleSignalUpdate(
        siteKey: String,
        action: String,
        initialization: InitializationState,
        credential: CachedInitCredential,
        signalCollectionManager: SignalCollectionManager,
    ) {
        signalUpdateScope.launch {
            try {
                withTimeout(SIGNAL_UPDATE_TIMEOUT_MS) {
                    val deadline = VerificationDeadline(SIGNAL_UPDATE_TIMEOUT_MS)
                    val signals = signalCollectionManager.refreshSignals(
                        initialization.copy(credential = credential)
                            .toSignalRequest(action, deadline.remainingMs()),
                    )
                    val response = protocolClient.updateSignals(
                        SignalUpdateRequest(
                            landingToken = initialization.response.landingToken,
                            siteKey = siteKey,
                            signals = signals,
                        ),
                        deadline.remainingMs(),
                    )
                    val replacement = CachedInitCredential(
                        credential = response.credential,
                        nonceUuid = response.nonceUuid,
                        droidGuardNonce = credential.droidGuardNonce,
                    )
                    credentialStore.replaceIfCurrent(
                        siteKey = siteKey,
                        expectedCredential = credential.credential,
                        replacement = replacement,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Background signal update failed", e)
            }
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }

    private fun gmsInstallerPackage(): String = try {
        if (Build.VERSION.SDK_INT >= 30) {
            context.packageManager.getInstallSourceInfo(Constants.GMS_PACKAGE_NAME)
                .initiatingPackageName
                .orEmpty()
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(Constants.GMS_PACKAGE_NAME).orEmpty()
        }
    } catch (e: Exception) {
        ""
    }

    private fun InitializationState.toSignalRequest(
        action: String,
        timeoutMs: Long,
    ) = SignalCollectionRequest(
        action = action,
        initializationResponse = response,
        cachedCredential = credential,
        timeoutMs = timeoutMs,
    )
}
