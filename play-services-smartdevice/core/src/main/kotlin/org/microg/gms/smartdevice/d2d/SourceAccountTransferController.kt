/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.accounts.Account
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.smartdevice.d2d.BootstrapAccount
import com.google.android.gms.smartdevice.d2d.data.AccountBootstrapPayload
import com.google.android.gms.smartdevice.d2d.data.DisplayText
import com.google.android.gms.smartdevice.setup.accounts.Assertion
import com.google.android.gms.smartdevice.setup.accounts.Challenge
import com.google.android.gms.smartdevice.setup.accounts.ExchangeAssertionsForUserCredentialsRequest
import com.google.android.gms.smartdevice.setup.accounts.ExchangeSessionCheckpointsForUserCredentialsRequest
import com.google.android.gms.smartdevice.setup.accounts.SessionCheckpoint
import com.google.android.gms.smartdevice.setup.accounts.UserBootstrapInfo
import com.google.android.gms.smartdevice.setup.accounts.UserCredential
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "SourceAccountTransfer"
private const val CHALLENGE_STATUS_OK = 0
private const val CREDENTIAL_TYPE_AUTHZEN = 3
private const val KEY_NAME = "name"
private const val KEY_SESSION_CHECKPOINT = "sessionCheckpoint"
private const val ACCOUNT_MANAGER_CLIENT_ID =
    "1070009224336-sdh77n7uot3oc99ais00jmuft6sk2fg9.apps.googleusercontent.com"

class SourceAccountTransferController(
    private val context: Context,
    accounts: List<BootstrapAccount>,
    private val enrollmentChecker: DirectTransferEnrollmentChecker,
    private val lockScreenShown: () -> Boolean,
    private val acceptsReturnedUserCredentials: Boolean,
    private val send: (AccountBootstrapPayload, DisplayText?) -> Unit,
    private val requestUserCredentialChallenge: (List<UserCredential>) -> Unit,
    private val fail: (Int, String) -> Unit,
) {
    private val keyStore = AuthZenKeyStore(context)
    private val selectedAccounts = accounts.mapNotNull { account ->
        val name = account.name?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        val type = account.type?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        Account(name, type)
    }.distinct()
    private val selectedAccountNames = selectedAccounts.map(Account::name).distinct()
    private val selectedAccountNameSet = selectedAccountNames.toSet()
    private val enrollmentStarted = AtomicBoolean(false)
    private val enrollmentCompleted = AtomicBoolean(false)
    private val enrollmentSucceeded = AtomicBoolean(false)
    private val transferStartRequested = AtomicBoolean(false)
    private val bootstrapInfoSent = AtomicBoolean(false)
    @Volatile private var transferPublicKeyHash: ByteArray? = null

    fun prepareEnrollment() {
        if (!enrollmentStarted.compareAndSet(false, true)) return
        if (selectedAccountNames.isEmpty()) {
            enrollmentCompleted.set(true)
            return
        }
        try {
            enrollmentChecker.forceRegister(selectedAccounts, fidoFlow = false)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start AuthZen ForceRegistration", e)
            enrollmentCompleted.set(true)
        }
    }

    fun start() {
        if (selectedAccountNames.isEmpty()) {
            fail(D2D_STATUS_ACCOUNT_BOOTSTRAP_INFO_ERROR, "No valid account identifiers were selected")
            return
        }
        transferStartRequested.set(true)
        prepareEnrollment()
        if (!enrollmentCompleted.get()) {
            enrollmentSucceeded.set(enrollmentChecker.awaitCompletion())
            enrollmentCompleted.set(true)
        }
        if (!enrollmentSucceeded.get()) {
            fail(D2D_STATUS_ACCOUNT_BOOTSTRAP_INFO_ERROR, "CryptAuth key registration did not complete successfully")
            return
        }
        sendUserBootstrapInfoIfReady()
    }

    private fun sendUserBootstrapInfoIfReady() {
        if (!transferStartRequested.get() || !enrollmentCompleted.get() || !enrollmentSucceeded.get()) return
        if (!bootstrapInfoSent.compareAndSet(false, true)) return
        sendUserBootstrapInfo()
    }

    private fun sendUserBootstrapInfo() {
        try {
            val notReadyAccountCount = selectedAccountNames.count { !keyStore.isTransferReady(it) }
            if (notReadyAccountCount != 0) {
                fail(
                    D2D_STATUS_ACCOUNT_BOOTSTRAP_INFO_ERROR,
                    "$notReadyAccountCount selected account(s) lack active CryptAuth PublicKey or authzen keys",
                )
                return
            }
            val publicKey = keyStore.publicKeyMaterial()
            transferPublicKeyHash = AuthZenWire.sha256(publicKey)
            val progressText = context.resources.getQuantityString(
                R.plurals.d2d_copying_accounts,
                selectedAccountNames.size,
            )
            val payload = AccountBootstrapPayload()
            payload.setUserBootstrapInfos(selectedAccountNames.map { UserBootstrapInfo(it, publicKey) })
            send(payload, DisplayText(progressText))
        } catch (e: Exception) {
            Log.e(TAG, "Unable to create UserBootstrapInfo", e)
            fail(D2D_STATUS_ACCOUNT_BOOTSTRAP_INFO_ERROR, "Unable to create account bootstrap information")
        }
    }

    fun process(payload: AccountBootstrapPayload) {
        val challenges = payload.challenges
        if (challenges != null) {
            processChallenges(challenges)
        }

        val userCredentials = payload.userCredentials
        if (userCredentials != null) {
            if (!acceptsReturnedUserCredentials) {
                fail(
                    D2D_STATUS_ACCOUNT_TRANSFER_SETUP_ERROR,
                    "Target returned a source-side challenge that was not negotiated",
                )
                return
            }
            requestUserCredentialChallenge(userCredentials)
        }

        if (challenges == null && userCredentials == null) {
            Log.w(TAG, "AccountBootstrapPayload contained no source-side operation")
        }
    }

    fun submitSessionCheckpoints(checkpointBundles: List<android.os.Bundle>) {
        try {
            val checkpoints = checkpointBundles.mapNotNull { bundle ->
                val account = bundle.getString(KEY_NAME)
                val checkpoint = bundle.getString(KEY_SESSION_CHECKPOINT)
                when {
                    checkpoint.isNullOrEmpty() -> {
                        Log.w(TAG, "Session checkpoint is empty accountPresent=${account != null}")
                        null
                    }
                    account == null -> {
                        Log.w(TAG, "Account identifier is null")
                        null
                    }
                    else -> SessionCheckpoint(account, checkpoint)
                }
            }
            val payload = AccountBootstrapPayload()
            payload.setExchangeSessionCheckpointsForUserCredentialsRequest(
                ExchangeSessionCheckpointsForUserCredentialsRequest(ArrayList(checkpoints)),
            )
            send(payload, null)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to submit session checkpoints", e)
            fail(D2D_STATUS_ACCOUNT_TRANSFER_SETUP_ERROR, "Unable to submit account session checkpoints")
        }
    }

    private fun processChallenges(challenges: List<Challenge>) {
        try {
            if (challenges.isEmpty()) throw IllegalArgumentException("Target returned no account challenges")
            val signals = sourceDeviceSignals()
            val assertions = challenges.map { challenge -> createAssertion(challenge, signals.encoded) }
            val sourceBackupAccountId = ""
            val platformVariant = context.detectDirectTransferPlatformVariant()
            val request = ExchangeAssertionsForUserCredentialsRequest(
                assertions,
                null,
                CREDENTIAL_TYPE_AUTHZEN,
                ACCOUNT_MANAGER_CLIENT_ID,
                null,
                sourceBackupAccountId,
                signals.androidId,
                false,
                platformVariant,
            )
            val payload = AccountBootstrapPayload()
            payload.setExchangeAssertionsForUserCredentialsRequest(request)
            send(payload, null)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to process account challenges", e)
            fail(D2D_STATUS_ACCOUNT_ASSERTION_ERROR, "Unable to create account challenge assertions")
        }
    }

    private fun createAssertion(challenge: Challenge, sourceDeviceSignals: ByteArray): Assertion {
        val account = challenge.accountIdentifier?.takeIf { it in selectedAccountNameSet }
            ?: throw IllegalArgumentException("Challenge references an unselected account")
        if (challenge.status != CHALLENGE_STATUS_OK) {
            throw IllegalArgumentException("Challenge status is not successful")
        }
        val challengeBytes = challenge.challenge ?: throw IllegalArgumentException("Challenge bytes are missing")
        val expectedPublicKeyHash = transferPublicKeyHash
            ?: throw IllegalStateException("Account bootstrap public key was not sent")
        val clientData = AuthZenWire.createClientData(account, challengeBytes, sourceDeviceSignals)
        val signedAssertion = keyStore.signIdentityAssertion(account, expectedPublicKeyHash, clientData)
        return Assertion(
            account,
            clientData,
            signedAssertion,
            challenge.challengeSessionState,
            challengeBytes,
        )
    }

    private fun sourceDeviceSignals(): SourceSignals {
        val checkinAndroidId = LastCheckinInfo.read(context).androidId
        require(checkinAndroidId != 0L) { "Google check-in Android ID is unavailable" }
        val androidId = java.lang.Long.toHexString(checkinAndroidId)

        val gmsVersion = Constants.GMS_VERSION_CODE.toString()
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

        @Suppress("DEPRECATION")
        val secure = keyguard?.isKeyguardSecure == true
        val shownLockScreen = lockScreenShown()
        val presence = AuthZenWire.encodeUserPresence(
            lockScreenSecure = secure,
            shownLockScreen = shownLockScreen,
        )
        val encoded = AuthZenWire.encodeSourceDeviceSignals(
            androidId = androidId,
            device = Build.DEVICE.orEmpty(),
            model = Build.MODEL.orEmpty(),
            sdkVersion = Build.VERSION.SDK_INT.toString(),
            googlePlayServicesVersion = gmsVersion,
            droidGuardResults = "",
            userPresence = presence,
        )
        return SourceSignals(androidId, encoded)
    }

    private data class SourceSignals(
        val androidId: String,
        val encoded: ByteArray,
    )
}
