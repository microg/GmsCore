/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.sourcedevice

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import com.google.android.gms.fido.sourcedevice.SourceStartDirectTransferOptions
import com.upokecenter.cbor.CBOREncodeOptions
import com.upokecenter.cbor.CBORObject
import org.microg.gms.auth.AuthConstants
import org.microg.gms.fido.core.digest
import org.microg.gms.fido.core.protocol.AuthenticatorData
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetAssertionRequest
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetInfoResponse
import org.microg.gms.fido.core.protocol.msgs.Ctap2CommandCode
import org.microg.gms.fido.core.transport.CtapStatus
import org.microg.gms.fido.core.transport.screenlock.ScreenLockTransportHandler
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

private const val NEXT_ASSERTION_TIMEOUT_MILLIS = 30_000L
private const val USER_VERIFICATION_EXTENSION = "google_userVerificationOrigin"
private const val USER_VERIFICATION_ORIGIN = "Quick Start"

/** Account-bound CTAP authenticator used by source-device DirectTransfer. */
class SourceFidoAuthenticator(
    context: Context,
    private val options: SourceStartDirectTransferOptions,
) {
    private val applicationContext = context.applicationContext
    private val keyStore = FidoSourceKeyStore(applicationContext)
    private var assertionRequest: AuthenticatorGetAssertionRequest? = null
    private var assertionCandidates: List<Account> = emptyList()
    private var nextAssertionIndex = 0
    private var lastAssertionAtMillis = 0L
    var isComplete = false
        private set

    @Synchronized
    fun handle(request: ByteArray): ByteArray {
        if (request.isEmpty()) {
            isComplete = true
            return error(CtapStatus.CTAP1_ERR_INVALID_LENGTH)
        }
        val command = Ctap2CommandCode.entries.firstOrNull { it.byte == request[0] }
            ?: run {
                isComplete = true
                return error(CtapStatus.CTAP1_ERR_INVALID_COMMAND)
            }
        val parameters = if (request.size > 1) {
            runCatching { CBORObject.Read(ByteArrayInputStream(request, 1, request.size - 1)) }
                .getOrElse {
                    isComplete = true
                    return error(CtapStatus.CTAP2_ERR_INVALID_CBOR)
                }
        } else {
            null
        }
        return runCatching {
            when (command) {
                Ctap2CommandCode.AuthenticatorGetInfo -> getInfo()
                Ctap2CommandCode.AuthenticatorGetAssertion -> {
                    if (parameters == null) {
                        isComplete = true
                        error(CtapStatus.CTAP2_ERR_MISSING_PARAMETER)
                    } else {
                        val repeatedRequest = assertionRequest != null
                        getAssertion(AuthenticatorGetAssertionRequest.decodeFromCbor(parameters)).also {
                            isComplete = repeatedRequest || nextAssertionIndex >= assertionCandidates.size
                        }
                    }
                }
                Ctap2CommandCode.AuthenticatorGetNextAssertion -> getNextAssertion().also {
                    isComplete = assertionRequest == null || nextAssertionIndex >= assertionCandidates.size
                }
                else -> {
                    isComplete = true
                    error(CtapStatus.CTAP1_ERR_INVALID_COMMAND)
                }
            }
        }.getOrElse {
            isComplete = true
            error(CtapStatus.CTAP1_ERR_OTHER)
        }
    }

    private fun getInfo(): ByteArray = success(
        AuthenticatorGetInfoResponse(
            versions = listOf("FIDO_2_0"),
            aaguid = ScreenLockTransportHandler.getAaguid(),
            options = AuthenticatorGetInfoResponse.Companion.Options(
                platformDevice = false,
                residentKey = false,
                clientPin = false,
                userPresence = true,
                userVerification = true,
            ),
        ).encodePayloadAsCbor(),
    )

    private fun getAssertion(request: AuthenticatorGetAssertionRequest): ByteArray {
        if (assertionRequest != null) return error(CtapStatus.CTAP2_ERR_NOT_ALLOWED)
        if (request.rpId.isEmpty() || request.clientDataHash.isEmpty()) {
            return error(CtapStatus.CTAP2_ERR_MISSING_PARAMETER)
        }
        if (options.accounts == null && request.allowList.isEmpty()) {
            return error(CtapStatus.CTAP1_ERR_INVALID_PARAMETER)
        }
        if (request.options?.userVerification == true && !options.isUserVerified) {
            return error(CtapStatus.CTAP2_ERR_UNSUPPORTED_OPTION)
        }
        if (options.isUserVerified && options.userVerificationOrigin != USER_VERIFICATION_ORIGIN) {
            return error(CtapStatus.CTAP2_ERR_UNSUPPORTED_OPTION)
        }
        if (request.options?.userPresence != false && !options.isUserPresenceSupported) {
            return error(CtapStatus.CTAP2_ERR_UNSUPPORTED_OPTION)
        }

        val requestedAccounts = options.accounts
        val candidateAccounts = requestedAccounts?.toList()
            ?: AccountManager.get(applicationContext).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).toList()
        val candidates = candidateAccounts.filter { account ->
            if (!keyStore.isActive(account.name, FidoSourceKeyStore.SOFTWARE_KEY_NAME)) {
                return@filter false
            }
            if (requestedAccounts != null || request.allowList.isEmpty()) return@filter true
            val handle = keyStore.getHandle(account.name, FidoSourceKeyStore.SOFTWARE_KEY_NAME)
                ?: return@filter false
            request.allowList.any { descriptor -> descriptor.id.contentEquals(handle) }
        }
        if (candidates.isEmpty()) return error(CtapStatus.CTAP2_ERR_NO_CREDENTIALS)

        assertionRequest = request
        assertionCandidates = candidates
        nextAssertionIndex = 1
        lastAssertionAtMillis = System.currentTimeMillis()
        return createAssertion(candidates.first(), request, candidates.size.takeIf { it > 1 })
    }

    private fun getNextAssertion(): ByteArray {
        val request = assertionRequest ?: return error(CtapStatus.CTAP2_ERR_NO_CREDENTIALS)
        val now = System.currentTimeMillis()
        if (now - lastAssertionAtMillis >= NEXT_ASSERTION_TIMEOUT_MILLIS) {
            clearAssertions()
            return error(CtapStatus.CTAP1_ERR_TIMEOUT)
        }
        val account = assertionCandidates.getOrNull(nextAssertionIndex)
            ?: return error(CtapStatus.CTAP2_ERR_NO_CREDENTIALS)
        nextAssertionIndex++
        lastAssertionAtMillis = now
        return createAssertion(account, request, null)
    }

    private fun createAssertion(
        account: Account,
        request: AuthenticatorGetAssertionRequest,
        numberOfCredentials: Int?,
    ): ByteArray {
        val credentialHandle = keyStore.getHandle(account.name, FidoSourceKeyStore.SOFTWARE_KEY_NAME)
            ?: return error(CtapStatus.CTAP2_ERR_NO_CREDENTIALS)
        val extensions = if (
            options.isUserVerified &&
            options.callerType == SourceStartDirectTransferOptions.CALLER_TYPE_SOURCE_DEVICE
        ) {
            CBORObject.NewMap()
                .Add(USER_VERIFICATION_EXTENSION, checkNotNull(options.userVerificationOrigin))
                .EncodeToBytes(CBOREncodeOptions.DefaultCtap2Canonical)
        } else {
            null
        }
        val authenticatorData = AuthenticatorData(
            rpIdHash = request.rpId.toByteArray(StandardCharsets.UTF_8).digest("SHA-256"),
            userPresent = options.isUserVerified || options.isUserPresenceSupported,
            userVerified = options.isUserVerified,
            signCount = keyStore.nextCounter(account.name),
            extensions = extensions,
        ).encode()
        val signature = keyStore.sign(account.name, authenticatorData + request.clientDataHash)
        val response = CBORObject.NewMap().apply {
            set(1, CBORObject.NewMap().apply {
                set("type", CBORObject.FromObject("public-key"))
                set("id", CBORObject.FromObject(credentialHandle))
            })
            set(2, CBORObject.FromObject(authenticatorData))
            set(3, CBORObject.FromObject(signature))
            set(4, CBORObject.NewMap().apply {
                set("id", CBORObject.FromObject(account.name.toByteArray(StandardCharsets.UTF_8)))
            })
            numberOfCredentials?.let { set(5, CBORObject.FromObject(it)) }
        }
        return success(response)
    }

    private fun clearAssertions() {
        assertionRequest = null
        assertionCandidates = emptyList()
        nextAssertionIndex = 0
        lastAssertionAtMillis = 0L
    }

    private fun success(payload: CBORObject): ByteArray =
        byteArrayOf(CtapStatus.CTAP1_ERR_SUCCESS.code) +
            payload.EncodeToBytes(CBOREncodeOptions.DefaultCtap2Canonical)

    private fun error(status: CtapStatus): ByteArray = byteArrayOf(status.code)
}
