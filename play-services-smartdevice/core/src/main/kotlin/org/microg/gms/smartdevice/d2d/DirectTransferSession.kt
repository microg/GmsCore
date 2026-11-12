/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.ResultReceiver
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.fido.sourcedevice.SourceStartDirectTransferOptions
import com.google.android.gms.smartdevice.d2d.AccountTransferResult
import com.google.android.gms.smartdevice.d2d.AuthenticatingUser
import com.google.android.gms.smartdevice.d2d.BootstrapAccount
import com.google.android.gms.smartdevice.d2d.BootstrapConfigurations
import com.google.android.gms.smartdevice.d2d.BootstrapOptions
import com.google.android.gms.smartdevice.d2d.DeviceDetails
import com.google.android.gms.smartdevice.d2d.SupervisedAccountInfo
import com.google.android.gms.smartdevice.d2d.data.AccountBootstrapPayload
import com.google.android.gms.smartdevice.d2d.data.BlockstorePayload
import com.google.android.gms.smartdevice.d2d.data.DeviceStatus
import com.google.android.gms.smartdevice.d2d.data.MessagePayload
import com.google.android.gms.smartdevice.d2d.data.SecondDeviceAuthPayload
import com.google.android.gms.smartdevice.d2d.internal.IDirectTransferListener
import com.google.android.gms.smartdevice.setup.accounts.UserCredential
import com.google.android.gms.smartdevice.setup.auth.StartSessionResponse
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import org.microg.gms.smartdevice.d2d.ui.SourceDirectTransferActivity
import org.microg.gms.smartdevice.d2d.ui.SourceDirectTransferActivityV1
import org.microg.gms.smartdevice.d2d.ui.SourceDirectTransferActivityV2
import java.io.IOException
import java.security.SecureRandom
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "DirectTransferSession"
private const val GOOGLE_ACCOUNT_TYPE = "com.google"
private const val UNICORN_ACCOUNT_FEATURE = "service_usm"
private const val GOOGLE_DESKTOP_GMS_FEATURE = "com.google.desktop.gms"
private const val UI_REQUEST_CODE = 8
private const val UI_CHALLENGE_REQUEST_CODE = 9
private const val DIRECT_TRANSFER_TIMEOUT_MILLIS = 45_000L
private const val PARENT_DIRECTED_TIMEOUT_MILLIS = 150_000L
private const val ANY_SESSION_ID = -1L
private const val CHALLENGE_KEY_NAME = "name"
private const val CHALLENGE_KEY_URL = "url"
private const val CHALLENGE_KEY_TARGET_SESSION_ID = "targetSessionId"
private const val USER_VERIFICATION_ORIGIN = "Quick Start"
private const val CLASSIC_ACCOUNT_TRANSFER_FLOW_TYPE = 1
private const val TARGET_MANAGED_ACCOUNTS_MIN_GMS_VERSION = 11_800_000

class DirectTransferSession(
    private val context: Context,
    private val sessionHandler: Handler,
    readerExecutor: Executor,
    private val configurations: BootstrapConfigurations,
    input: D2DInputStream,
    output: D2DOutputStream,
    private val transferListener: IDirectTransferListener,
    private val enrollmentChecker: DirectTransferEnrollmentChecker,
    private val startSourceFidoTransfer: StartSourceFidoTransfer,
    private val onComplete: (DirectTransferSession) -> Unit,
) {
    private val createdAtElapsedRealtime = SystemClock.elapsedRealtime()
    private val closed = AtomicBoolean(false)
    private val terminal = AtomicBoolean(false)
    private val globalSessionGateAcquired = AtomicBoolean(false)
    private val sessionId = AtomicLong(ANY_SESSION_ID)
    private val accountResults = ArrayList<AccountTransferResult>()
    private val uiMessages = QueuedMessageSender<UiMessage>()
    private val fidoStartupExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "D2D-FIDO-Startup").apply { isDaemon = true }
    }
    private var sourceFidoController: SourceFidoController? = null

    @Volatile private var bootstrapConfigurationSent = false
    @Volatile private var transferStarted = false
    @Volatile private var targetOptions: BootstrapOptions? = null
    @Volatile private var pendingIntent: PendingIntent? = null
    @Volatile private var lockScreenConfirmed: Boolean? = null
    @Volatile private var lockScreenAuthenticationType = 0
    @Volatile private var transferAccounts: List<BootstrapAccount>? = null
    @Volatile private var pendingChallengeRoute: ChallengeRoute? = null
    @Volatile private var selectedParentId: String? = null
    @Volatile private var supervisedAccountInfo: SupervisedAccountInfo? = null
    @Volatile private var parentDirectedPickerSelection = false

    private var accountTransferController: SourceAccountTransferController? = null
    private val inactivityTimeout = DirectTransferTimeout(
        initialTimeoutMillis = DIRECT_TRANSFER_TIMEOUT_MILLIS,
    ) { timeoutMillis ->
        failFromInactivityTimeout(timeoutMillis)
    }
    private val channel = DirectTransferChannel(
        sessionHandler = sessionHandler,
        readerExecutor = readerExecutor,
        input = input,
        output = output,
        supportsUnencrypted = configurations.supportsUnencryptedCommunication,
        onMessageActivity = inactivityTimeout::markMessageActivity,
        onPayload = ::handlePayload,
        onFailure = { code, message, cause ->
            if (cause != null) Log.e(TAG, message, cause)
            failWithoutReply(code, message)
        },
    )

    private val uiResultReceiver = object : ResultReceiver(sessionHandler) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {

            resultData?.classLoader = SourceDirectTransferActivity::class.java.classLoader
            when (resultCode) {
                SOURCE_ACTIVITY_EVENT_ACTIVITY_STARTED -> {
                    val activityReceiver = resultData.readResultReceiver(
                        SourceDirectTransferActivity.RESULT_RESULT_RECEIVER,
                    )
                    if (activityReceiver == null) {
                        fail(D2D_STATUS_ACCOUNT_TRANSFER_SETUP_ERROR, "Source UI did not publish its ResultReceiver")
                        return
                    }
                    uiMessages.attach { message -> activityReceiver.send(message.code, message.data) }
                    val pendingIntentData = Bundle()
                    pendingIntentData.putParcelable(
                        SourceDirectTransferActivity.EXTRA_PENDING_INTENT,
                        pendingIntent,
                    )
                    sendUiMessage(SOURCE_UI_EVENT_PENDING_INTENT_REFERENCE, pendingIntentData)
                }
                SOURCE_ACTIVITY_EVENT_ACTIVITY_FINISHED -> uiMessages.detach()
                SOURCE_ACTIVITY_EVENT_ACCOUNT_SELECTED -> {
                    val accounts = resultData.readBootstrapAccounts(SourceDirectTransferActivity.RESULT_SELECTED_ACCOUNTS)
                    val selectedChildId = resultData?.getString(SourceDirectTransferActivity.RESULT_SELECTED_CHILD_ID)
                    val accountSelectionToken = resultData?.getString(
                        SourceDirectTransferActivity.RESULT_ACCOUNT_SELECTION_TOKEN,
                    )
                    if (accounts.isNotEmpty() && accountSelectionToken != null) {
                        supervisedAccountInfo = SupervisedAccountInfo(
                            accounts.first().name,
                            accountSelectionToken,

                            null,
                        )
                    }
                    if (selectedChildId != null) {
                        parentDirectedPickerSelection = true

                        configurations.setAuthenticatingUser(AuthenticatingUser(selectedChildId, null))
                        if (targetOptions?.hasOptionFlag(18) == true) {
                            configurations.setUnicornChallengeDeduplicationEnabled(true)
                        }
                        inactivityTimeout.updateTimeout(PARENT_DIRECTED_TIMEOUT_MILLIS)
                        inactivityTimeout.resume()
                    }
                    if (accounts.isEmpty()) {
                        fail(D2D_STATUS_NO_ACCOUNTS, "No accounts selected")
                    } else {
                        recordSelectedAccounts(accounts)
                    }
                }
                SOURCE_ACTIVITY_EVENT_CHALLENGE_RECEIVED -> {
                    val checkpoints = resultData.readBundles(SourceDirectTransferActivity.RESULT_ACCOUNT_CHALLENGE_DATA)
                    val challengeRoute = pendingChallengeRoute
                    pendingChallengeRoute = null
                    when (challengeRoute) {
                        ChallengeRoute.CLASSIC_ACCOUNT_TRANSFER ->
                            accountTransferController?.submitSessionCheckpoints(checkpoints)
                                ?: fail(
                                    D2D_STATUS_ACCOUNT_TRANSFER_SETUP_ERROR,
                                    "Account transfer controller is unavailable",
                                )
                        ChallengeRoute.SECOND_DEVICE_AUTH -> submitSecondDeviceAuthFallback(checkpoints)
                        null -> fail(
                            D2D_STATUS_ACCOUNT_TRANSFER_SETUP_ERROR,
                            "Source UI returned an unexpected account challenge",
                        )
                    }
                    inactivityTimeout.resume()
                }
                SOURCE_ACTIVITY_EVENT_LOCK_SCREEN_CONFIRMED -> {
                    val confirmed = resultData?.getBoolean(SourceDirectTransferActivity.RESULT_LOCK_SCREEN_CONFIRMED)
                        ?: false
                    val reportedType = resultData?.getInt(SourceDirectTransferActivity.RESULT_LOCK_SCREEN_AUTH_TYPE) ?: 0
                    lockScreenAuthenticationType = if (!confirmed) 4 else reportedType.takeUnless { it == 0 } ?: 3

                    lockScreenConfirmed = confirmed
                    inactivityTimeout.resume()
                    startAccountTransferIfReady()
                }
                SOURCE_ACTIVITY_EVENT_CHALLENGE_WEBVIEW_ERROR -> {
                    val dependencyCode = resultData?.getInt(
                        SourceDirectTransferActivity.RESULT_DEPENDENCY_ERROR_CODE,
                    ) ?: 0
                    fail(D2D_STATUS_WEBVIEW_ERROR, "Account challenge WebView failed ($dependencyCode)")
                }
                SOURCE_ACTIVITY_EVENT_PARENT_ID_RECEIVED -> {
                    selectedParentId = resultData?.getString(SourceDirectTransferActivity.RESULT_PARENT_ID)
                }
                SOURCE_ACTIVITY_EVENT_USER_CANCELED_BOOTSTRAP ->
                    fail(D2D_STATUS_USER_CANCELED, "Bootstrap canceled by user")
                SOURCE_ACTIVITY_EVENT_DEVICE_OWNER_ERROR ->
                    fail(D2D_STATUS_MANAGED_OWNER, "Source device is managed by a device owner")
                SOURCE_ACTIVITY_EVENT_MANAGED_BACKUP_ERROR ->
                    fail(D2D_STATUS_MANAGED_BACKUP, "Source backup account is managed")
                SOURCE_ACTIVITY_EVENT_BOOTSTRAP_SKIPPED ->
                    fail(D2D_STATUS_USER_SKIPPED, "Bootstrap skipped by user")
                SOURCE_ACTIVITY_EVENT_NO_TRANSFERABLE_ACCOUNTS ->
                    fail(D2D_STATUS_NO_TRANSFERABLE, "No transferable accounts")
                // Keep the UI timeout paused so the user can retry or cancel from this screen.
                SOURCE_ACTIVITY_EVENT_SCREEN_LOCK_FAILED_OR_SKIPPED -> Unit
                SOURCE_ACTIVITY_EVENT_FIDO_PENDING_INTENT_RESULT -> {
                    val statusCode = resultData?.getInt(SOURCE_ACTIVITY_KEY_PENDING_INTENT_RESULT)



                    inactivityTimeout.resume()
                    if (statusCode == CommonStatusCodes.CANCELED) {
                        fail(D2D_STATUS_FIDO_CANCELED, "User canceled during FIDO")
                    }
                }
                SOURCE_ACTIVITY_EVENT_KIDS_FLOW_STARTED -> {
                    inactivityTimeout.pause()
                }
                else -> fail(
                    D2D_STATUS_ACCOUNT_TRANSFER_SETUP_ERROR,
                    "Source UI returned an unsupported event",
                )
            }
        }
    }

    fun start() {
        if (closed.get()) return
        if (!ACTIVE_SESSION_GATE.tryAcquire()) {
            Log.e(TAG, "There is another transfer session in progress")
            val reportConflict = Runnable {
                fail(D2D_STATUS_BUSY, "There is another transfer session in progress.")
            }
            if (!sessionHandler.post(reportConflict)) reportConflict.run()
            return
        }
        globalSessionGateAcquired.set(true)
        try {
            inactivityTimeout.start()
            channel.start()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start DirectTransfer session", e)
            fail(D2D_STATUS_ENCRYPT_MESSAGE_ERROR, "Unable to start DirectTransfer session")
        }
    }

    @Synchronized
    fun abort(onAborted: () -> Unit = {}) {
        if (!terminal.compareAndSet(false, true)) {
            onAborted()
            return
        }

        channel.stopReading()
        sendUiMessage(SOURCE_UI_EVENT_ABORT, Bundle.EMPTY)
        if (channel.isReady) {
            runCatching {
                sendState(BOOTSTRAP_STATE_CANCELED, null)
            }.onFailure {
                Log.w(TAG, "Unable to send cancel state", it)
            }
        }
        finishAbort(onAborted)
    }

    private fun finishAbort(onAborted: () -> Unit) {
        val finish = Runnable {
            destroyInternal()
            onAborted()
        }
        if (Looper.myLooper() == sessionHandler.looper) {
            finish.run()
        } else if (!sessionHandler.post(finish)) {
            finish.run()
        }
    }

    fun destroy() {
        if (closed.get()) return

        terminal.compareAndSet(false, true)
        destroyInternal()
    }

    private fun postFailureWithoutReply(code: Int, message: String, cause: Throwable? = null) {
        if (closed.get() || terminal.get()) return
        val action = Runnable {
            if (closed.get() || terminal.get()) return@Runnable
            if (cause != null) Log.e(TAG, message, cause)
            failWithoutReply(code, message)
        }
        if (Looper.myLooper() == sessionHandler.looper) {
            action.run()
        } else if (!sessionHandler.post(action)) {
            action.run()
        }
    }

    private fun handlePayload(payload: MessagePayload) {
        when (payload.bootstrapState) {
            BOOTSTRAP_STATE_CANCELED -> {
                failWithoutReply(
                    payload.deviceStatus?.errorCode ?: D2D_STATUS_CANCELED,
                    "Bootstrap canceled by target",
                )
                return
            }
            BOOTSTRAP_STATE_REMOTE_ERROR -> {
                failWithoutReply(
                    payload.deviceStatus?.errorCode ?: D2D_STATUS_REMOTE_DEVICE_ERROR,
                    "Remote device error",
                )
                return
            }
            else -> Unit
        }

        payload.accountTransferResults?.takeIf { it.isNotEmpty() }?.let { results ->
            Log.i(TAG, "Received account transfer results")
            processAccountTransferResults(results)
            if (targetOptions?.wantsFidoTransfer() == true) {
                completeFromFidoResults()
                return
            }
        }

        when (payload.bootstrapState) {
            BOOTSTRAP_STATE_COMPLETED -> {
                completeFromTarget()
                return
            }
            BOOTSTRAP_STATE_IN_PROGRESS,
            BOOTSTRAP_STATE_SECOND_DEVICE_AUTH_STARTED,
            BOOTSTRAP_STATE_SECOND_DEVICE_AUTH_CHALLENGE_COMPLETED -> Unit
            else -> {
                failWithoutReply(D2D_STATUS_REMOTE_DEVICE_ERROR, "Unknown remote device state ${payload.bootstrapState}")
                return
            }
        }

        payload.bootstrapOptions?.let(::processBootstrapOptions)
        payload.accountBootstrapPayload?.let(::processAccountBootstrapPayload)
        payload.secondDeviceAuthPayload?.let(::processSecondDeviceAuthPayload)
    }

    private fun processSecondDeviceAuthPayload(payload: SecondDeviceAuthPayload) {
        payload.fidoMessage?.let { message ->
            if (targetOptions?.wantsFidoTransfer() != true) {
                fail(D2D_STATUS_FIDO_API_ERROR, "Target sent a FIDO message without negotiating FIDO")
                return
            }
            handleFidoMessage(message)
            return
        }

        val responses = payload.startSessionResponses
        if (responses.isNullOrEmpty()) {
            Log.w(TAG, "SecondDeviceAuthPayload contained neither a FIDO message nor source fallback challenges")
            return
        }
        if (targetOptions?.acceptsReturnedUserCredentials() != true) {
            fail(
                D2D_STATUS_ACCOUNT_TRANSFER_SETUP_ERROR,
                "Target sent source fallback challenges for a flow that did not negotiate them",
            )
            return
        }

        val challengeData = ArrayList<Bundle>(responses.size)
        responses.forEach { response ->
            val challenge = Bundle()
            challenge.putString(CHALLENGE_KEY_NAME, response.accountIdentifier)
            val sourceFallbackUrl = response.sourceFallbackUrl
            if (!sourceFallbackUrl.isNullOrEmpty()) {
                challenge.putString(CHALLENGE_KEY_URL, sourceFallbackUrl)
            }
            val targetFallbackUrl = response.targetFallbackUrl
            if (!targetFallbackUrl.isNullOrEmpty()) {
                challenge.putString(CHALLENGE_KEY_URL, targetFallbackUrl)
            }
            val targetSessionId = response.targetSessionId
            if (!targetSessionId.isNullOrEmpty()) {
                challenge.putString(CHALLENGE_KEY_TARGET_SESSION_ID, targetSessionId)
            }
            challengeData += challenge
        }
        requestSourceChallenges(challengeData, ChallengeRoute.SECOND_DEVICE_AUTH)
    }

    private fun handleFidoMessage(message: ByteArray) {
        runCatching { checkNotNull(sourceFidoController).write(message) }
            .onFailure {
                Log.w(TAG, "Unable to write source FIDO request", it)
                fail(D2D_STATUS_FIDO_PIPE_ERROR, "Writing to FIDO pipe failed")
            }
    }

    private fun processBootstrapOptions(options: BootstrapOptions) {
        if (targetOptions != null) {
            Log.w(TAG, "Ignoring duplicate BootstrapOptions")
            return
        }
        targetOptions = options
        if (!isValidSessionId(options.sessionId)) {
            options.sessionId = generateSessionId()
        }
        sessionId.set(options.sessionId)
        ACTIVE_SESSION_ID.set(options.sessionId)

        val localAccounts = localBootstrapAccounts()
        if (localAccounts.isEmpty()) {
            fail(D2D_STATUS_NO_ACCOUNTS, "No accounts found")
            return
        }

        val authenticatingUser = configurations.authenticatingUser
        val targetAccounts = options.accountsOnTarget.orEmpty()
        val parentDirectedInitialSignIn =
            authenticatingUser?.obfuscatedGaiaID != null ||
                configurations.accountPickerOptions?.kidsOnboardingEnforced == true
        val parentDirectedReauthentication =
            configurations.accountPickerEnabled && authenticatingUser?.email != null
        if (parentDirectedInitialSignIn && targetAccounts.isNotEmpty()) {
            fail(
                D2D_STATUS_UNICORN_ACCOUNT_ERROR,
                "Cannot start parent-directed initial sign-in because target already has accounts",
            )
            return
        }
        if (
            parentDirectedReauthentication &&
            targetAccounts.isNotEmpty() &&
            targetAccounts.first().name != authenticatingUser?.email
        ) {
            fail(
                D2D_STATUS_UNICORN_ACCOUNT_ERROR,
                "Cannot reauthenticate a different supervised account than the one on target",
            )
            return
        }
        if (options.hasOptionFlag(18) && authenticatingUser != null && !parentDirectedReauthentication) {
            configurations.setUnicornChallengeDeduplicationEnabled(true)
        }

        if (authenticatingUser != null) {
            inactivityTimeout.updateTimeout(PARENT_DIRECTED_TIMEOUT_MILLIS)
        }

        val useV2Ui = options.usesV2DirectTransferUi()
        val unicornChallengeDeduplicationEnabled = configurations.unicornChallengeDeduplicationEnabled

        if (configurations.hasUserConfirmed) {

            lockScreenConfirmed = configurations.isLockScreenShown
            lockScreenAuthenticationType = 0
        }

        if (configurations.accountPickerEnabled) {
            requestUserConfirmation(
                useV2Ui,
                unicornChallengeDeduplicationEnabled,
            )
        } else {


            val preselected = configurations.bootstrapAccounts?.takeIf { it.isNotEmpty() }
            if (preselected == null) {
                fail(D2D_STATUS_NO_ACCOUNTS, "Account picker is disabled but no source accounts were configured")
                return
            }


            if (unicornChallengeDeduplicationEnabled) {
                lockScreenConfirmed = false
                lockScreenAuthenticationType = 0
            }
            if (!recordSelectedAccounts(preselected)) return


            requestUserConfirmation(
                useV2Ui,
                unicornChallengeDeduplicationEnabled,
            )
        }
    }

    private fun requestUserConfirmation(
        useV2Ui: Boolean,
        unicornChallengeDeduplicationEnabled: Boolean,
    ) {
        if (pendingIntent != null || terminal.get()) return


        if (configurations.accountPickerEnabled || !unicornChallengeDeduplicationEnabled) {
            inactivityTimeout.pause()
        }
        val intent = Intent(context, sourceActivityClass(useV2Ui)).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            putExtra(
                SourceDirectTransferActivity.EXTRA_RESULT_RECEIVER,
                uiResultReceiver.asRemoteResultReceiver(),
            )
            putExtra(SourceDirectTransferActivity.EXTRA_SESSION_ID, sessionId.get())
            putExtra(
                SourceDirectTransferActivity.EXTRA_ACCOUNT_PICKER_ENABLED,
                configurations.accountPickerEnabled,
            )
            putExtra(
                SourceDirectTransferActivity.EXTRA_HAS_USER_CONFIRMED,
                configurations.hasUserConfirmed,
            )
            if (useV2Ui) putExtra(SourceDirectTransferActivity.EXTRA_IS_FIDO, true)
            putExtra(
                SourceDirectTransferActivity.EXTRA_UNICORN_CHALLENGE_DEDUPLICATION_ENABLED,
                unicornChallengeDeduplicationEnabled,
            )
            putExtra(
                SourceDirectTransferActivity.EXTRA_TARGET_ACCEPTS_MANAGED_ACCOUNTS,
                checkNotNull(targetOptions) { "BootstrapOptions missing" }.targetAcceptsManagedAccounts(),
            )
            if (!useV2Ui) {
                putExtra(
                    SourceDirectTransferActivity.EXTRA_SHOW_SKIP_ACCOUNT,
                    !configurations.hideSkipAccount,
                )
            }
            if (configurations.accountPickerEnabled) {
                configurations.accountPickerOptions?.let { options ->

                    putExtra(
                        SourceDirectTransferActivity.EXTRA_ACCOUNT_PICKER_OPTIONS,
                        SafeParcelableSerializer.serializeToBytes(options),
                    )
                }
            }
            putExtra(SourceDirectTransferActivity.EXTRA_UI_CUSTOMIZATION, configurations.uiCustomization)
            configurations.extraParameters?.let { extras ->
                putExtra(
                    SourceDirectTransferActivity.EXTRA_CONFIRMATION_TITLE,
                    extras.getString("directTransferConfirmationTitleText"),
                )
                putExtra(
                    SourceDirectTransferActivity.EXTRA_CONFIRMATION_BODY,
                    extras.getString("directTransferConfirmationBodyText"),
                )
                putExtra(
                    SourceDirectTransferActivity.EXTRA_CONFIRMATION_BUTTON,
                    extras.getString("directTransferConfirmationButton"),
                )
                putExtra(
                    SourceDirectTransferActivity.EXTRA_TRANSITION_TITLE,
                    extras.getString("directTransferTransitionTitle"),
                )
                putExtra(
                    SourceDirectTransferActivity.EXTRA_TRANSITION_DESCRIPTION,
                    extras.getString("directTransferTransitionDescription"),
                )
            }
        }
        val action = createSourceActivityPendingIntent(UI_REQUEST_CODE, intent, useV2Ui)
        pendingIntent = action
        deliver("onUserActionRequired") { transferListener.onUserActionRequired(action) }
    }

    @Synchronized
    private fun recordSelectedAccounts(accounts: List<BootstrapAccount>): Boolean {
        if (terminal.get()) return false
        if (transferAccounts != null) {
            Log.w(TAG, "Ignoring duplicate source account selection")
            return true
        }
        if (accounts.isEmpty()) {
            fail(D2D_STATUS_NO_ACCOUNTS, "No accounts selected")
            return false
        }
        val parentDirectedReauthentication =
            configurations.accountPickerEnabled && configurations.authenticatingUser?.email != null
        val skipUnicornBlocking = if (configurations.accountPickerEnabled) {
            parentDirectedPickerSelection || parentDirectedReauthentication
        } else {
            configurations.authenticatingUser != null
        }
        if (
            !skipUnicornBlocking &&
            hasUnicornAccountConflict(accounts)
        ) {
            fail(
                D2D_STATUS_UNICORN_ACCOUNT_ERROR,
                "Cannot transfer supervised account because target already has accounts",
            )
            return false
        }


        transferAccounts = accounts
        configurations.setBootstrapAccounts(ArrayList(accounts))
        populateDeviceDetails()
        val options = checkNotNull(targetOptions) { "BootstrapOptions missing" }
        if (!options.wantsFidoTransfer()) {
            prepareAccountTransferController(accounts, options)
        }

        configurations.setMaxPacketSize(if (options.supportsPacketMode) options.maxPacketSize else 0)
        if (!bootstrapConfigurationSent) {
            try {
                val bootstrapConfigurationMessage = MessagePayload()
                bootstrapConfigurationMessage.setBootstrapConfigurations(
                    configurations.copyForDirectTransferWire(),
                )
                sendPayload(bootstrapConfigurationMessage)
                enablePacketMode(options)
                bootstrapConfigurationSent = true
            } catch (e: Exception) {
                Log.e(TAG, "Unable to send BootstrapConfigurations", e)
                fail(D2D_STATUS_MESSAGE_SEND_ERROR, "Unable to send BootstrapConfigurations")
                return false
            }
        }
        startAccountTransferIfReady()
        return !terminal.get()
    }

    @Synchronized
    private fun startAccountTransferIfReady() {
        if (transferStarted || terminal.get()) return
        val accounts = transferAccounts
        val confirmed = lockScreenConfirmed
        val options = targetOptions
        val controller = accountTransferController
        if (!channel.isReady || !bootstrapConfigurationSent || accounts == null || confirmed == null || options == null) {
            return
        }
        if (!options.wantsFidoTransfer() && controller == null) return
        transferStarted = true
        if (options.wantsFidoTransfer()) {
            startFidoTransfer()
        } else {
            checkNotNull(controller).start()
        }
    }

    private fun startFidoTransfer() {
        val accounts = transferAccounts.orEmpty().mapNotNull { bootstrapAccount ->
            val name = bootstrapAccount.name?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val type = bootstrapAccount.type?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            Account(name, type)
        }.distinct()
        if (accounts.isEmpty()) {
            fail(D2D_STATUS_NO_ACCOUNTS, "No valid account identifiers were selected")
            return
        }
        inactivityTimeout.pause()
        fidoStartupExecutor.execute {
            val registered = runCatching {
                enrollmentChecker.forceRegister(accounts, fidoFlow = true)
                enrollmentChecker.awaitCompletion()
            }.getOrDefault(false)
            if (!sessionHandler.post {
                    if (terminal.get() || closed.get()) return@post
                    inactivityTimeout.resume()
                    if (!registered) {
                        fail(D2D_STATUS_FIDO_API_ERROR, "FIDO CryptAuth registration failed")
                        return@post
                    }
                    startRegisteredFidoTransfer(accounts)
                }
            ) {
                postFailureWithoutReply(D2D_STATUS_FIDO_API_ERROR, "Unable to dispatch FIDO startup")
            }
        }
    }

    private fun startRegisteredFidoTransfer(accounts: List<Account>) {
        val verified = lockScreenConfirmed == true
        val options = if (verified) {
            SourceStartDirectTransferOptions(
                SourceStartDirectTransferOptions.CALLER_TYPE_SOURCE_DEVICE,
                true,
                accounts,
                true,
                USER_VERIFICATION_ORIGIN,
            )
        } else {
            SourceStartDirectTransferOptions(accounts)
        }
        val controller = SourceFidoController(
            mainHandler = sessionHandler,
            starter = startSourceFidoTransfer,
            onResponse = { response -> sendFidoResponse(response) },
            onError = { code, message -> fail(code, message) },
        )
        sourceFidoController = controller
        try {
            controller.start(options)
        } catch (e: IOException) {
            Log.e(TAG, "Unable to create FIDO pipes", e)
            fail(D2D_STATUS_FIDO_PIPE_ERROR, "Creating pipe failed")
            return
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start FIDO transfer", e)
            fail(D2D_STATUS_FIDO_API_ERROR, "Unable to start FIDO transfer")
            return
        }

        try {
            val fidoStartMessage = MessagePayload()
            fidoStartMessage.setBootstrapState(BOOTSTRAP_STATE_SECOND_DEVICE_AUTH_STARTED)
            fidoStartMessage.setSecondDeviceAuthPayload(SecondDeviceAuthPayload(false))
            sendPayload(fidoStartMessage)
            val blockstoreMessage = MessagePayload()
            blockstoreMessage.setBlockstorePayload(BlockstorePayload())
            sendPayload(blockstoreMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to send FIDO start payloads", e)
            fail(D2D_STATUS_MESSAGE_SEND_ERROR, "Unable to send FIDO start payloads")
        }
    }

    private fun sendFidoResponse(response: ByteArray) {
        if (terminal.get() || closed.get()) return
        runCatching {
            val fidoResponseMessage = MessagePayload()
            fidoResponseMessage.setSecondDeviceAuthPayload(SecondDeviceAuthPayload(response))
            sendPayload(fidoResponseMessage)
        }.onFailure {
            Log.w(TAG, "Unable to send FIDO response", it)
            fail(D2D_STATUS_MESSAGE_SEND_ERROR, "Unable to send FIDO response")
        }
    }

    private fun prepareAccountTransferController(accounts: List<BootstrapAccount>, options: BootstrapOptions) {
        if (accountTransferController != null) return
        accountTransferController = SourceAccountTransferController(
            context = context,
            accounts = accounts,
            enrollmentChecker = enrollmentChecker,

            lockScreenShown = {
                lockScreenConfirmed ?: configurations.isLockScreenShown
            },

            acceptsReturnedUserCredentials = options.acceptsReturnedUserCredentials(),
            send = { accountPayload, displayText ->
                if (!terminal.get()) {
                    val accountMessage = MessagePayload()
                    accountMessage.setAccountBootstrapPayload(accountPayload)
                    accountMessage.setDisplayText(displayText)
                    sendPayload(accountMessage)
                } else {
                    Log.w(TAG, "Dropping account payload after DirectTransfer became terminal")
                }
            },
            requestUserCredentialChallenge = ::requestUserCredentialChallenge,
            fail = ::fail,
        ).also(SourceAccountTransferController::prepareEnrollment)
    }

    private fun processAccountBootstrapPayload(payload: AccountBootstrapPayload) {
        accountTransferController?.process(payload)
            ?: fail(D2D_STATUS_ACCOUNT_TRANSFER_SETUP_ERROR, "Account transfer controller is unavailable")
    }

    private fun requestUserCredentialChallenge(credentials: List<UserCredential>) {
        if (terminal.get()) return
        val challengeData = ArrayList<Bundle>(credentials.size)
        credentials.forEach { credential ->
            val hasFallbackUrl = !credential.fallbackUrl.isNullOrEmpty()
            val challenge = Bundle()
            challenge.putString("name", credential.accountIdentifier)
            if (!hasFallbackUrl) {
                challenge.putString("credential", credential.credential)
            } else {
                challenge.putString("url", credential.fallbackUrl)
            }
            val firstName = credential.firstName
            if (!firstName.isNullOrEmpty()) {
                challenge.putString("firstName", firstName)
            }
            val lastName = credential.lastName
            if (!lastName.isNullOrEmpty()) {
                challenge.putString("lastName", lastName)
            }
            challengeData += challenge
        }
        requestSourceChallenges(challengeData, ChallengeRoute.CLASSIC_ACCOUNT_TRANSFER)
    }

    private fun requestSourceChallenges(challengeData: ArrayList<Bundle>, route: ChallengeRoute) {
        if (terminal.get()) return
        if (pendingChallengeRoute != null) {
            fail(D2D_STATUS_ACCOUNT_TRANSFER_SETUP_ERROR, "A source account challenge is already pending")
            return
        }
        pendingChallengeRoute = route
        inactivityTimeout.pause()
        val challengeDataResult = Bundle()
        challengeDataResult.putParcelableArrayList(
            SourceDirectTransferActivity.EXTRA_ACCOUNT_CHALLENGE_DATA,
            challengeData,
        )
        sendUiMessage(SOURCE_UI_EVENT_CHALLENGE_SOURCE, challengeDataResult)
        if (uiMessages.isAttached() || pendingIntent != null) return



        val useV2Ui = checkNotNull(targetOptions) { "BootstrapOptions missing" }.usesV2DirectTransferUi()
        val intent = Intent(context, sourceActivityClass(useV2Ui)).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(SourceDirectTransferActivity.EXTRA_WAIT_FOR_CHALLENGE, true)
            putExtra(SourceDirectTransferActivity.EXTRA_RESULT_RECEIVER, uiResultReceiver)
            putExtra(SourceDirectTransferActivity.EXTRA_SESSION_ID, sessionId.get())
            if (useV2Ui) {
                putExtra(SourceDirectTransferActivity.EXTRA_IS_FIDO, true)
            } else {
                putExtra(
                    SourceDirectTransferActivity.EXTRA_SHOW_SKIP_ACCOUNT,
                    !configurations.hideSkipAccount,
                )
            }
            putExtra(
                SourceDirectTransferActivity.EXTRA_TARGET_ACCEPTS_MANAGED_ACCOUNTS,
                checkNotNull(targetOptions).targetAcceptsManagedAccounts(),
            )
        }
        val action = createSourceActivityPendingIntent(UI_CHALLENGE_REQUEST_CODE, intent, useV2Ui)
        pendingIntent?.cancel()
        pendingIntent = action
        deliver("onUserActionRequired") { transferListener.onUserActionRequired(action) }
    }

    private fun submitSecondDeviceAuthFallback(checkpointBundles: List<Bundle>) {
        val responses = checkpointBundles.map { bundle ->
            val response = StartSessionResponse()
            val targetSessionId = bundle.getString(CHALLENGE_KEY_TARGET_SESSION_ID)
            if (!targetSessionId.isNullOrEmpty()) {
                response.setTargetSessionId(targetSessionId)
            }
            val accountIdentifier = bundle.getString(CHALLENGE_KEY_NAME)
            if (!accountIdentifier.isNullOrEmpty()) {
                response.setAccountIdentifier(accountIdentifier)
            }
            response
        }
        try {
            val fallbackMessage = MessagePayload()
            fallbackMessage.setBootstrapState(BOOTSTRAP_STATE_SECOND_DEVICE_AUTH_CHALLENGE_COMPLETED)
            fallbackMessage.setSecondDeviceAuthPayload(SecondDeviceAuthPayload(ArrayList(responses)))
            sendPayload(fallbackMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to send second-device-auth fallback responses", e)
            fail(D2D_STATUS_MESSAGE_SEND_ERROR, "Unable to send source fallback challenge responses")
        }
    }

    private fun sourceActivityClass(useV2Ui: Boolean): Class<out SourceDirectTransferActivity> =
        if (useV2Ui) SourceDirectTransferActivityV2::class.java else SourceDirectTransferActivityV1::class.java

    private fun createSourceActivityPendingIntent(
        requestCode: Int,
        intent: Intent,
        useV2Ui: Boolean,
    ): PendingIntent {
        val activityOptions = if (useV2Ui && Build.VERSION.SDK_INT >= 34) {
            ActivityOptions.makeBasic().apply {
                pendingIntentCreatorBackgroundActivityStartMode =
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }.toBundle()
        } else {
            null
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            activityOptions,
        )
    }

    private fun completeFromTarget() {
        completeTransfer(sendCompletionState = targetOptions?.wantsFidoTransfer() != true)
    }

    private fun completeFromFidoResults() {
        completeTransfer(sendCompletionState = true)
    }

    private fun completeTransfer(sendCompletionState: Boolean) {
        if (!transferStarted) {
            val message = "handleOnCompleted called before transfer started."
            Log.e(TAG, message)
            fail(D2D_STATUS_BAD_COMPLETE_ORDER, message)
            return
        }
        if (!terminal.compareAndSet(false, true)) return

        channel.stopReading()

        sendUiMessage(SOURCE_UI_EVENT_BOOTSTRAP_COMPLETE, Bundle.EMPTY)
        if (!sendCompletionState) {
            finishCompletion(null)
            return
        }
        try {
            sendState(BOOTSTRAP_STATE_COMPLETED, null) { failure ->
                val finish = Runnable { finishCompletion(failure) }
                if (!sessionHandler.post(finish)) {
                    Log.e(TAG, "Unable to post completion to DirectTransfer Handler")
                    finish.run()
                }
            }
        } catch (e: Exception) {
            finishCompletion(e)
        }
    }

    private fun finishCompletion(failure: Throwable?) {
        if (closed.get()) return
        if (failure != null) {
            Log.e(TAG, "Unable to acknowledge transfer completion", failure)
            sendUiMessage(SOURCE_UI_EVENT_ERROR, Bundle.EMPTY)
            val encryptionFailed = failure is SecurityException
            val errorCode = if (encryptionFailed) D2D_STATUS_ENCRYPT_MESSAGE_ERROR else D2D_STATUS_MESSAGE_SEND_ERROR
            val errorMessage = if (encryptionFailed) "Error encrypting MessagePayload" else "Unable to acknowledge transfer completion"
            deliver("onTransferError") {
                transferListener.onTransferError(errorCode, errorMessage)
            }
        } else {
            deliver("onAccountsTransferred") {
                transferListener.onAccountsTransferred(accountResults.toTypedArray())
            }
        }
        destroyInternal()
    }

    private fun processAccountTransferResults(results: List<AccountTransferResult>) {
        results.forEach { result ->
            result.setLockScreenAuthenticationType(lockScreenAuthenticationType)
            selectedParentId?.let { parentId ->
                result.bootstrapAccount?.setParentId(parentId)
            }
            supervisedAccountInfo?.let { info ->
                result.setSupervisedAccountInfo(info)
            }
        }
        accountResults.addAll(results)
    }

    private fun sendState(
        state: Int,
        errorCode: Int?,
        onComplete: ((Throwable?) -> Unit)? = null,
    ): Future<Unit> {
        val payload = MessagePayload()
        payload.setBootstrapState(state)
        if (errorCode != null) {
            val deviceStatus = DeviceStatus()
            deviceStatus.setErrorCode(errorCode)
            payload.setDeviceStatus(deviceStatus)
        }
        return sendPayload(payload, onComplete)
    }

    private fun sendPayload(
        payload: MessagePayload,
        onComplete: ((Throwable?) -> Unit)? = null,
    ): Future<Unit> = channel.send(payload, onComplete)

    private fun enablePacketMode(options: BootstrapOptions) {
        if (!options.supportsPacketMode) return
        val packetSize = options.maxPacketSize
        channel.enablePacketMode(packetSize)
    }

    private fun fail(code: Int, message: String) {
        failInternal(code, message, sendTerminalState = true)
    }

    private fun failFromInactivityTimeout(timeoutMillis: Long) {
        val code = D2D_STATUS_MESSAGE_TIMEOUT
        val message = "A message was not received or sent for $timeoutMillis millis"
        val fail = Runnable { fail(code, message) }
        if (Looper.myLooper() == sessionHandler.looper) {
            fail.run()
        } else if (!sessionHandler.post(fail)) {
            fail.run()
        }
    }

    private fun failWithoutReply(code: Int, message: String) {
        failInternal(code, message, sendTerminalState = false)
    }

    private fun failInternal(code: Int, message: String, sendTerminalState: Boolean) {
        if (!terminal.compareAndSet(false, true)) return
        Log.e(TAG, "DirectTransfer failed code=$code sessionId=${sessionId.get()} elapsedMs=${elapsedRealtime()} message=$message")
        channel.stopReading()
        sendUiMessage(SOURCE_UI_EVENT_ERROR, Bundle.EMPTY)
        if (sendTerminalState && channel.isReady) {
            runCatching {
                val state = if (code == D2D_STATUS_CANCELED || code == D2D_STATUS_USER_CANCELED) {
                    BOOTSTRAP_STATE_CANCELED
                } else {
                    BOOTSTRAP_STATE_REMOTE_ERROR
                }
                sendState(state, code)
            }.onFailure { Log.w(TAG, "Unable to send terminal state", it) }
        }
        deliver("onTransferError") { transferListener.onTransferError(code, message) }
        destroyInternal()
    }

    @SuppressLint("MissingPermission")
    private fun populateDeviceDetails() {

        val androidId = LastCheckinInfo.read(context).androidId
        val gmsVersion = Constants.GMS_VERSION_CODE
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        @Suppress("DEPRECATION")
        val hasConnectivity = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
            ?.activeNetworkInfo
            ?.isConnected == true
        val screenConfiguration = context.resources.configuration
        val screenSize = screenConfiguration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        val isTablet = screenSize > Configuration.SCREENLAYOUT_SIZE_LARGE ||
            screenConfiguration.smallestScreenWidthDp >= 600
        val deviceType: Byte = when {
            context.packageManager.hasSystemFeature(GOOGLE_DESKTOP_GMS_FEATURE) -> 8
            isTablet -> 2
            else -> 1
        }
        val deviceDetails = DeviceDetails(androidId, gmsVersion)
        deviceDetails.setHasConnectivity(hasConnectivity)

        @Suppress("DEPRECATION")
        deviceDetails.setHasLockscreen(keyguard?.isKeyguardSecure == true)
        deviceDetails.setModel(Build.MODEL)
        deviceDetails.setDeviceType(deviceType)
        deviceDetails.setDeviceFingerprint(Build.FINGERPRINT)
        configurations.setDeviceDetails(deviceDetails)
    }

    private fun localBootstrapAccounts(): List<BootstrapAccount> {
        return AccountManager.get(context).getAccountsByType(GOOGLE_ACCOUNT_TYPE).map { account ->
            BootstrapAccount(account.name, account.type)
        }
    }

    private fun hasUnicornAccountConflict(accounts: List<BootstrapAccount>): Boolean {
        if (accounts.isEmpty()) return false
        val accountsOnTarget = targetOptions?.accountsOnTarget.orEmpty()
        if (accountsOnTarget.isNotEmpty()) {
            val selected = accounts.first()
            val target = accountsOnTarget.first()
            if (target.isSupervised) {
                if (selected.name == target.name) {
                    inactivityTimeout.updateTimeout(PARENT_DIRECTED_TIMEOUT_MILLIS)
                    return false
                }
                return true
            }
            return isUnicornAccount(selected.name)
        }
        accounts.forEach { account ->
            if (isUnicornAccount(account.name)) {
                inactivityTimeout.updateTimeout(PARENT_DIRECTED_TIMEOUT_MILLIS)
            }
        }
        return false
    }

    private fun isUnicornAccount(accountName: String?): Boolean {
        if (accountName == null) return false
        val unicornAccounts = runCatching {
            AccountManager.get(context).getAccountsByTypeAndFeatures(
                GOOGLE_ACCOUNT_TYPE,
                arrayOf(UNICORN_ACCOUNT_FEATURE),
                null,
                null,
            ).result
        }.onFailure {
            Log.w(TAG, "Unable to query supervised-account feature", it)
        }.getOrDefault(emptyArray())
        val isUnicorn = unicornAccounts.any { account -> account.name == accountName }
        return isUnicorn
    }

    private fun destroyInternal() {
        if (!closed.compareAndSet(false, true)) return
        inactivityTimeout.close()
        sourceFidoController?.close()
        sourceFidoController = null
        fidoStartupExecutor.shutdownNow()
        pendingIntent?.cancel()
        pendingIntent = null
        runCatching { channel.close() }
        if (globalSessionGateAcquired.compareAndSet(true, false)) {
            ACTIVE_SESSION_ID.set(ANY_SESSION_ID)
            ACTIVE_SESSION_GATE.release()
        }
        onComplete(this)
    }

    private fun elapsedRealtime(): Long =
        (SystemClock.elapsedRealtime() - createdAtElapsedRealtime).coerceAtLeast(0L)

    private fun deliver(callback: String, action: () -> Unit) {
        runCatching(action).onFailure { Log.w(TAG, "$callback failed", it) }
    }

    private fun sendUiMessage(code: Int, data: Bundle) {
        uiMessages.send(UiMessage(code, data))
    }

    companion object {
        private val ACTIVE_SESSION_GATE = Semaphore(1)
        private val ACTIVE_SESSION_ID = AtomicLong(ANY_SESSION_ID)

        internal fun activeSessionId(): Long = ACTIVE_SESSION_ID.get()

        private fun isValidSessionId(value: Long): Boolean = value != ANY_SESSION_ID && value != 0L

        private fun generateSessionId(): Long {
            var value: Long
            do {
                value = SecureRandom().nextLong()
            } while (!isValidSessionId(value))
            return value
        }
    }

    private data class UiMessage(val code: Int, val data: Bundle)

    private enum class ChallengeRoute {
        CLASSIC_ACCOUNT_TRANSFER,
        SECOND_DEVICE_AUTH,
    }
}

internal fun BootstrapOptions.acceptsReturnedUserCredentials(): Boolean =
    flowType == CLASSIC_ACCOUNT_TRANSFER_FLOW_TYPE

internal fun BootstrapOptions.targetAcceptsManagedAccounts(): Boolean =
    gmsVersion < TARGET_MANAGED_ACCOUNTS_MIN_GMS_VERSION || hasOptionFlag(1)

private fun BootstrapOptions.hasOptionFlag(index: Int): Boolean =
    index in 1..Long.SIZE_BITS && optionFlags and (1L shl (index - 1)) != 0L

private fun BootstrapOptions.usesV2DirectTransferUi(): Boolean =
    wantsFidoTransfer()

private fun BootstrapOptions.wantsFidoTransfer(): Boolean =
    hasOptionFlag(12) && hasOptionFlag(14)

@Suppress("DEPRECATION")
private fun Bundle?.readBootstrapAccounts(key: String): ArrayList<BootstrapAccount> {
    val bytes = this?.getByteArray(key) ?: return arrayListOf()
    val parcel = Parcel.obtain()
    return try {
        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0)
        val accounts = ArrayList<BootstrapAccount>()
        parcel.readTypedList(accounts, BootstrapAccount.CREATOR)
        accounts
    } finally {
        parcel.recycle()
    }
}

@Suppress("DEPRECATION")
private fun Bundle?.readBundles(key: String): ArrayList<Bundle> =
    this?.getParcelableArrayList(key) ?: arrayListOf()

@Suppress("DEPRECATION")
private fun Bundle?.readResultReceiver(key: String): ResultReceiver? = this?.getParcelable(key)

private fun ResultReceiver.asRemoteResultReceiver(): ResultReceiver {
    val parcel = Parcel.obtain()
    return try {
        writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        ResultReceiver.CREATOR.createFromParcel(parcel)
    } finally {
        parcel.recycle()
    }
}
