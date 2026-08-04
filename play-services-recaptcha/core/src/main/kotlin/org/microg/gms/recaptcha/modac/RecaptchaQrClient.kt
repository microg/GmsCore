/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac

import java.util.UUID
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.microg.gms.recaptcha.modac.signals.SignalCollectionManager

private const val MINIMUM_VERIFY_TIMEOUT_MS = 5_000L
private val SUPPORTED_ACTIONS = setOf("modacVerify")

internal class RecaptchaSession(val packageName: String)

internal class RecaptchaAction(val name: String) {
    companion object {
        val MODAC_VERIFY = RecaptchaAction("modacVerify")
    }
}

internal class RecaptchaExecutionRequest(
    val siteKey: String,
    val packageName: String,
    val sessionRequestId: String,
    val action: RecaptchaAction,
    val timeoutMs: Long,
    val qrToken: String,
)

internal interface RecaptchaClientWorkflow {
    suspend fun execute(
        request: RecaptchaExecutionRequest,
        signalCollectionManager: SignalCollectionManager,
    ): String
}

internal class RecaptchaQrClient(
    private val workflow: RecaptchaClientWorkflow,
    private val siteKey: String,
    private val session: RecaptchaSession,
    private val signalCollectionManager: SignalCollectionManager,
) {
    private val verificationMutex = Mutex()

    suspend fun verify(action: RecaptchaAction, qrToken: String, timeoutMs: Long): String {
        if (action.name !in SUPPORTED_ACTIONS) {
            throw RecaptchaError(
                RecaptchaErrorKind.INTERNAL,
                RecaptchaErrorSubKind.RUNTIME_ERROR,
                "unsupported_action",
            )
        }
        if (timeoutMs < MINIMUM_VERIFY_TIMEOUT_MS) {
            throw RecaptchaError(
                RecaptchaErrorKind.INTERNAL,
                RecaptchaErrorSubKind.UNKNOWN,
                "timeout_too_short",
            )
        }
        return try {
            withTimeout(timeoutMs) {
                verificationMutex.withLock {
                    workflow.execute(
                        request = RecaptchaExecutionRequest(
                            siteKey = siteKey,
                            packageName = session.packageName,
                            sessionRequestId = UUID.randomUUID().toString(),
                            action = action,
                            timeoutMs = timeoutMs,
                            qrToken = qrToken,
                        ),
                        signalCollectionManager = signalCollectionManager,
                    )
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw RecaptchaError(
                RecaptchaErrorKind.NETWORK,
                RecaptchaErrorSubKind.UNKNOWN,
                "verification_timeout",
                cause = e,
            )
        }
    }
}
