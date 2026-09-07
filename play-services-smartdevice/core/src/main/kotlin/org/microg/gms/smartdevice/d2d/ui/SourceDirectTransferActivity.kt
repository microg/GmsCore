/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d.ui

import android.accounts.AccountManager
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.ResultReceiver
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.smartdevice.d2d.BootstrapAccount
import com.google.android.gms.smartdevice.directtransfer.AccountPickerOptions
import org.microg.gms.smartdevice.d2d.R
import org.microg.gms.smartdevice.d2d.SOURCE_ACTIVITY_EVENT_ACCOUNT_SELECTED
import org.microg.gms.smartdevice.d2d.SOURCE_ACTIVITY_EVENT_ACTIVITY_FINISHED
import org.microg.gms.smartdevice.d2d.SOURCE_ACTIVITY_EVENT_ACTIVITY_STARTED
import org.microg.gms.smartdevice.d2d.SOURCE_ACTIVITY_EVENT_BOOTSTRAP_SKIPPED
import org.microg.gms.smartdevice.d2d.SOURCE_ACTIVITY_EVENT_CHALLENGE_RECEIVED
import org.microg.gms.smartdevice.d2d.SOURCE_ACTIVITY_EVENT_CHALLENGE_WEBVIEW_ERROR
import org.microg.gms.smartdevice.d2d.SOURCE_ACTIVITY_EVENT_DEVICE_OWNER_ERROR
import org.microg.gms.smartdevice.d2d.SOURCE_ACTIVITY_EVENT_LOCK_SCREEN_CONFIRMED
import org.microg.gms.smartdevice.d2d.SOURCE_ACTIVITY_EVENT_NO_TRANSFERABLE_ACCOUNTS
import org.microg.gms.smartdevice.d2d.SOURCE_ACTIVITY_EVENT_PARENT_ID_RECEIVED
import org.microg.gms.smartdevice.d2d.SOURCE_ACTIVITY_EVENT_SCREEN_LOCK_FAILED_OR_SKIPPED
import org.microg.gms.smartdevice.d2d.SOURCE_ACTIVITY_EVENT_USER_CANCELED_BOOTSTRAP
import org.microg.gms.smartdevice.d2d.SOURCE_UI_EVENT_ABORT
import org.microg.gms.smartdevice.d2d.SOURCE_UI_EVENT_BOOTSTRAP_COMPLETE
import org.microg.gms.smartdevice.d2d.SOURCE_UI_EVENT_CHALLENGE_SOURCE
import org.microg.gms.smartdevice.d2d.SOURCE_UI_EVENT_ERROR
import org.microg.gms.smartdevice.d2d.SOURCE_UI_EVENT_PENDING_INTENT_REFERENCE
import java.lang.ref.WeakReference

private const val GOOGLE_CORP_ACCOUNT_SUFFIX = "@google.com"

open class SourceDirectTransferActivity : FragmentActivity(),
    AccountChallengeFragment.Listener,
    SourceConfirmationFragment.Listener,
    SourceAccountPickerFragment.Listener,
    LockScreenFragment.Listener {
    private var receiver: ResultReceiver? = null
    private var ownPendingIntent: PendingIntent? = null
    private val uiState = SourceTransferUiState()
    private var cancelSetupDialog: AlertDialog? = null
    private var accountPickerEnabled = false
    private var isFidoFlow = false
    private var managedAccountTransferBlocked = false
    private var accountPickerOptions: AccountPickerOptions? = null
    private var pendingContentFragment: Fragment? = null
    private val serviceResultReceiver = ProxyResultReceiver(Handler(Looper.getMainLooper()), this)

    internal fun onServiceResult(resultCode: Int, resultData: Bundle?) {
        when (resultCode) {
            SOURCE_UI_EVENT_CHALLENGE_SOURCE -> {
                if (managedAccountTransferBlocked) {
                    Log.w(TAG, "Ignoring source challenge while managed-account transfer is blocked")
                    return
                }
                val challengeData = if (resultData == null) {
                    arrayListOf()
                } else {
                    resultData.readAccountChallengeData()
                }
                if (challengeData == null) {
                    reportChallengeError(0)
                    finish()
                } else {
                    showAccountChallengeFragment(challengeData)
                }
            }
            SOURCE_UI_EVENT_PENDING_INTENT_REFERENCE -> {
                @Suppress("DEPRECATION")
                ownPendingIntent = resultData?.getParcelable(EXTRA_PENDING_INTENT)
            }
            SOURCE_UI_EVENT_BOOTSTRAP_COMPLETE, SOURCE_UI_EVENT_ABORT, SOURCE_UI_EVENT_ERROR ->
                finishAndRemoveTask()
            else -> Log.w(TAG, "Ignoring unknown source-service result $resultCode")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureWindow()
        setContentView(R.layout.d2d_fragment_container)
        @Suppress("DEPRECATION")
        run {
            uiState.pendingSelectedAccounts =
                savedInstanceState?.getParcelableArrayList(STATE_PENDING_SELECTED_ACCOUNTS)
        }
        uiState.selectionDispatched =
            savedInstanceState?.getBoolean(STATE_SELECTION_DISPATCHED) == true
        uiState.accountLoadPending =
            savedInstanceState?.getBoolean(STATE_ACCOUNT_LOAD_PENDING) == true
        accountPickerEnabled = intent.getBooleanExtra(EXTRA_ACCOUNT_PICKER_ENABLED, false)
        val hasUserConfirmed = intent.getBooleanExtra(EXTRA_HAS_USER_CONFIRMED, false)
        isFidoFlow = intent.getBooleanExtra(EXTRA_IS_FIDO, false)
        val unicornChallengeDeduplicationEnabled =
            intent.getBooleanExtra(EXTRA_UNICORN_CHALLENGE_DEDUPLICATION_ENABLED, false)
        val targetAcceptsManagedAccounts =
            intent.getBooleanExtra(EXTRA_TARGET_ACCEPTS_MANAGED_ACCOUNTS, true)
        accountPickerOptions = intent.readAccountPickerOptions()
        val sourceDeviceManaged = isSourceDeviceManaged()
        managedAccountTransferBlocked = !targetAcceptsManagedAccounts && sourceDeviceManaged

        receiver = intent.readResultReceiver()
        if (receiver == null) {
            Log.e(TAG, "Missing result receiver")
            finish()
            return
        }
        if (managedAccountTransferBlocked) {
            Log.w(TAG, "Source device has a device owner but target does not accept managed accounts")
            publishActivityStarted()
            finishWithManagedDeviceError()
            return
        }

        // FragmentManager restores the visible state; redispatching would reset in-progress UI.
        if (savedInstanceState != null) {
            publishActivityStarted()
            if (uiState.accountLoadPending && accountPickerEnabled && !uiState.selectionDispatched) {
                loadAccountPickerAccounts()
            }
            return
        }

        intent.readAccountChallengeData()?.let { challengeData ->
            showAccountChallengeFragment(challengeData)
            publishActivityStarted()
            return
        }

        uiState.pendingSelectedAccounts?.takeIf { it.isNotEmpty() }?.let {
            showSourceConfirmation()
            publishActivityStarted()
            return
        }

        if (uiState.selectionDispatched) {
            showTransferProgress()
            publishActivityStarted()
            return
        }

        if (intent.getBooleanExtra(EXTRA_WAIT_FOR_CHALLENGE, false)) {
            showInProgress()
            publishActivityStarted()
            return
        }

        if (isFidoFlow && (hasUserConfirmed || unicornChallengeDeduplicationEnabled)) {
            showTransferProgress()
            publishActivityStarted()
            return
        }

        if (hasUserConfirmed && !accountPickerEnabled) {
            showTransferProgress()
            publishActivityStarted()
            return
        }

        if (accountPickerEnabled) {
            showInProgress()
            loadAccountPickerAccounts()
        } else {
            showSourceConfirmation()
        }
        publishActivityStarted()
    }

    private fun publishActivityStarted() {
        val result = Bundle()
        result.putParcelable(RESULT_RESULT_RECEIVER, serviceResultReceiver)
        sendResult(SOURCE_ACTIVITY_EVENT_ACTIVITY_STARTED, result)
    }

    private fun showAccountPicker(accounts: List<BootstrapAccount>) {
        // Account support flags are opt-in; absent options use their false defaults.
        val options = accountPickerOptions
        val selectable = BooleanArray(accounts.size) { index ->
            isAccountSelectable(accounts[index], options)
        }
        if (selectable.none { it }) {
            Log.w(TAG, "No accounts satisfy the source account-picker policy")
            sendResult(SOURCE_ACTIVITY_EVENT_NO_TRANSFERABLE_ACCOUNTS)
            finish()
            return
        }
        showAccountPickerFragment(accounts, selectable)
    }

    private fun showAccountPickerFragment(
        accounts: List<BootstrapAccount>,
        selectable: BooleanArray,
    ) {
        // Skip remains visible unless explicitly disabled.
        val skipVisible = accountPickerOptions?.skipButtonVisible != false
        val kidsOnboardingEnforced = accountPickerOptions?.kidsOnboardingEnforced == true
        val title = accountPickerOptions?.title?.takeIf(String::isNotBlank)
        val subtitle = accountPickerOptions?.description?.takeIf(String::isNotBlank)
        val fragment = SourceAccountPickerFragment.newInstance(
            accounts = ArrayList(accounts),
            selectable = selectable,
            title = title,
            subtitle = subtitle,
            skipButtonVisible = skipVisible,
            kidsOnboardingEnforced = kidsOnboardingEnforced,
        )
        replaceFragment(fragment)
    }

    private fun loadLocalBootstrapAccounts(onLoaded: (ArrayList<BootstrapAccount>) -> Unit) {
        val accountManager = AccountManager.get(this)
        val localAccounts = accountManager.getAccountsByType(GOOGLE_ACCOUNT_TYPE)
        val bootstrapAccounts = localAccounts.mapTo(ArrayList()) { account ->
            BootstrapAccount(account.name, account.type)
        }
        if (localAccounts.isEmpty() || accountPickerOptions?.supervisedAccountsSupported == true) {
            onLoaded(bootstrapAccounts)
            return
        }

        accountManager.getAccountsByTypeAndFeatures(
            GOOGLE_ACCOUNT_TYPE,
            arrayOf(SUPERVISED_ACCOUNT_FEATURE),
            { future ->
                val supervisedNames = runCatching { future.result.mapTo(HashSet()) { it.name } }
                    .getOrElse { error ->
                        // Treat an unavailable feature query as no supervised accounts.
                        Log.w(TAG, "Unable to query supervised-account feature", error)
                        hashSetOf()
                    }
                bootstrapAccounts.forEach { account ->
                    account.setSupervised(account.name in supervisedNames)
                }
                if (!isFinishing && !isDestroyed) onLoaded(bootstrapAccounts)
            },
            Handler(mainLooper),
        )
    }

    private fun loadAccountPickerAccounts() {
        uiState.accountLoadPending = true
        loadLocalBootstrapAccounts { accounts ->
            uiState.accountLoadPending = false
            showAccountPicker(accounts)
        }
    }

    private fun beginPickerSelection(accounts: ArrayList<BootstrapAccount>) {
        if (isSourceDeviceSecure() && accountPickerOptions?.kidsOnboardingEnforced != true) {
            uiState.pendingSelectedAccounts = accounts
            showHeadlessLockScreenConfirmation()
            return
        }
        completePickerSelection(accounts)
    }

    private fun showSourceConfirmation() {
        if (uiState.selectionDispatched) {
            Log.w(TAG, "Ignoring source confirmation after result dispatch")
            return
        }
        if (!isSourceDeviceSecure()) {
            showTransferConfirmation()
            return
        }
        if (Build.VERSION.SDK_INT < 21) {
            Log.w(TAG, "Secure screen lock cannot be confirmed through public APIs below Android 5.0")
            onLockScreenCancelled()
            return
        }
        showVisibleLockScreenConfirmation()
    }

    private fun isSourceDeviceSecure(): Boolean {
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguard?.isKeyguardSecure == true
    }

    private fun isSourceDeviceManaged(): Boolean {
        val devicePolicy = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        return runCatching {

            devicePolicy.activeAdmins.orEmpty().any { admin ->
                devicePolicy.isDeviceOwnerApp(admin.packageName)
            }
        }.onFailure {
            Log.w(TAG, "Unable to query device-owner state", it)
        }.getOrDefault(false)
    }

    private fun finishWithManagedDeviceError() {
        if (isFinishing || isDestroyed) return
        sendResult(SOURCE_ACTIVITY_EVENT_DEVICE_OWNER_ERROR)
        setResult(RESULT_CANCELED)
        finishAndRemoveTask()
    }

    private fun showTransferConfirmation() {
        val (confirmationTitle, confirmationBody, confirmationButton) = confirmationContent()
        val fragment = SourceConfirmationFragment.newCopyConfirmation(
            title = confirmationTitle,
            message = confirmationBody,
            continueText = confirmationButton,
        )
        replaceFragment(fragment)
    }

    private fun showVisibleLockScreenConfirmation() {
        if (uiState.selectionDispatched || uiState.userCancellationSent) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            onLockScreenCancelled()
            return
        }

        val (confirmationTitle, confirmationBody, confirmationButton) = confirmationContent()
        replaceFragment(
            LockScreenFragment.newConfirmationInstance(
                title = confirmationTitle,
                message = confirmationBody,
                continueText = confirmationButton,
            ),
        )
    }

    private fun showHeadlessLockScreenConfirmation() {
        if (uiState.selectionDispatched || uiState.userCancellationSent) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            onLockScreenCancelled()
            return
        }

        val existing = supportFragmentManager.findFragmentByTag(LOCK_SCREEN_FRAGMENT_TAG)
            as? LockScreenFragment
        if (existing != null) {
            existing.requestAuthentication()
            return
        }
        if (supportFragmentManager.isStateSaved) {
            Log.w(TAG, "Cannot attach lock-screen coordinator after Fragment state was saved")
            onLockScreenCancelled()
            return
        }

        supportFragmentManager.beginTransaction()
            .add(LockScreenFragment.newHeadlessInstance(), LOCK_SCREEN_FRAGMENT_TAG)
            .commitNow()
    }

    private fun confirmationContent(): Triple<String, String, String> = Triple(
        intent.getStringExtra(EXTRA_CONFIRMATION_TITLE)?.takeIf(String::isNotBlank)
            ?: getString(R.string.d2d_copy_confirmation_title),
        intent.getStringExtra(EXTRA_CONFIRMATION_BODY)?.takeIf(String::isNotBlank)
            ?: getString(R.string.d2d_copy_confirmation_message),
        intent.getStringExtra(EXTRA_CONFIRMATION_BUTTON)?.takeIf(String::isNotBlank)
            ?: getString(R.string.d2d_copy_confirmation_action),
    )

    override fun onLockScreenSuccess(authenticationType: Int) {
        if (uiState.userCancellationSent || isFinishing || isDestroyed) return
        if (accountPickerEnabled && uiState.pendingSelectedAccounts != null) {
            removeHeadlessLockScreenCoordinator()
            completeAuthenticatedPickerSelection(authenticationType)
        } else {
            completeUserConfirmation(
                lockScreenConfirmed = true,
                authenticationType = authenticationType,
            )
        }
    }

    override fun onLockScreenCancelled() {
        if (uiState.userCancellationSent || isFinishing || isDestroyed) return
        Log.w(TAG, "Screen lock confirmation cancelled or failed")
        if (accountPickerEnabled && uiState.pendingSelectedAccounts != null) {
            removeHeadlessLockScreenCoordinator()
            sendResult(SOURCE_ACTIVITY_EVENT_SCREEN_LOCK_FAILED_OR_SKIPPED)
            uiState.pendingSelectedAccounts = null
            val picker = supportFragmentManager.findFragmentById(R.id.d2d_fragment_container)
                as? SourceAccountPickerFragment
            if (picker?.restoreAfterCredentialCancellation() != true) {
                showInProgress()
                loadAccountPickerAccounts()
            }
            return
        }
        // Dismissing system authentication is retryable; keep the confirmation state active.
        sendResult(SOURCE_ACTIVITY_EVENT_SCREEN_LOCK_FAILED_OR_SKIPPED)
    }

    override fun onLockScreenExplicitlyCancelled() {
        if (uiState.userCancellationSent) return
        cancelTransfer()
    }

    private fun completePickerSelection(accounts: ArrayList<BootstrapAccount>) {
        if (uiState.selectionDispatched) {
            Log.w(TAG, "Ignoring duplicate completed account selection")
            return
        }
        uiState.selectionDispatched = true

        sendResult(
            SOURCE_ACTIVITY_EVENT_ACCOUNT_SELECTED,
            Bundle().apply {
                putByteArray(RESULT_SELECTED_ACCOUNTS, accounts.marshallTypedList())
            },
        )
        sendLockScreenResult(
            lockScreenConfirmed = false,
            authenticationType = 0,
        )
        setResult(RESULT_OK)
        showTransferProgress()
    }

    private fun completeAuthenticatedPickerSelection(authenticationType: Int) {
        val accounts = uiState.pendingSelectedAccounts
        if (uiState.selectionDispatched) {
            Log.w(TAG, "Ignoring duplicate authenticated account selection")
            return
        }
        if (accounts.isNullOrEmpty()) {
            Log.e(TAG, "Missing selected accounts after V2 screen-lock confirmation")
            cancelTransfer()
            return
        }
        uiState.selectionDispatched = true

        val selectedAccountsResult = Bundle().apply {
            putByteArray(RESULT_SELECTED_ACCOUNTS, accounts.marshallTypedList())
        }
        if (isFidoFlow) {
            sendLockScreenResult(lockScreenConfirmed = true, authenticationType = authenticationType)
            sendResult(SOURCE_ACTIVITY_EVENT_ACCOUNT_SELECTED, selectedAccountsResult)
        } else {
            sendResult(SOURCE_ACTIVITY_EVENT_ACCOUNT_SELECTED, selectedAccountsResult)
            sendLockScreenResult(lockScreenConfirmed = true, authenticationType = authenticationType)
        }
        uiState.pendingSelectedAccounts = null
        setResult(RESULT_OK)
        showTransferProgress()
    }

    private fun completeUserConfirmation(
        lockScreenConfirmed: Boolean,
        authenticationType: Int,
    ) {
        if (uiState.selectionDispatched) {
            Log.w(TAG, "Ignoring duplicate completed user confirmation")
            return
        }
        uiState.selectionDispatched = true
        uiState.pendingSelectedAccounts = null
        sendLockScreenResult(lockScreenConfirmed, authenticationType)
        setResult(RESULT_OK)
        showTransferProgress()
    }

    private fun sendLockScreenResult(lockScreenConfirmed: Boolean, authenticationType: Int) {
        sendResult(
            SOURCE_ACTIVITY_EVENT_LOCK_SCREEN_CONFIRMED,
            Bundle().apply {
                putBoolean(RESULT_LOCK_SCREEN_CONFIRMED, lockScreenConfirmed)
                putInt(RESULT_LOCK_SCREEN_AUTH_TYPE, authenticationType)
            },
        )
    }

    private fun removeHeadlessLockScreenCoordinator() {
        val coordinator = supportFragmentManager.findFragmentByTag(LOCK_SCREEN_FRAGMENT_TAG)
            as? LockScreenFragment ?: return
        // Authentication has completed; remove stale coordinator state before recreation.
        supportFragmentManager.beginTransaction()
            .remove(coordinator)
            .commitNowAllowingStateLoss()
    }

    private fun showAccountChallengeFragment(challengeData: ArrayList<Bundle>) {
        val showSkip = intent.getBooleanExtra(EXTRA_SHOW_SKIP_ACCOUNT, true)
        val transitionTitle = intent.getStringExtra(EXTRA_TRANSITION_TITLE)
        val transitionDescription = intent.getStringExtra(EXTRA_TRANSITION_DESCRIPTION)
        // Preserve the active queue when follow-up challenges arrive.
        val existing = supportFragmentManager.findFragmentById(R.id.d2d_fragment_container)
        if (existing is AccountChallengeFragment) {
            existing.updateChallengeData(challengeData)
            return
        }
        val fragment = AccountChallengeFragment.newInstance(
            challengeData = challengeData,
            showSkipAccount = showSkip,
            isFidoFlow = isFidoFlow,
            transitionTitle = transitionTitle,
            transitionDescription = transitionDescription,
        )
        replaceFragment(fragment)
    }

    override fun onChallengesComplete(checkpoints: ArrayList<Bundle>) {
        if (isFinishing || isDestroyed) return
        val result = Bundle()
        result.putParcelableArrayList(RESULT_ACCOUNT_CHALLENGE_DATA, checkpoints)
        sendResult(SOURCE_ACTIVITY_EVENT_CHALLENGE_RECEIVED, result)
        setResult(RESULT_OK)
        showInProgress()
    }

    override fun onChallengeError(errorCode: Int) {
        if (isFinishing || isDestroyed) return
        Log.w(TAG, "Account challenge WebView reported error code=$errorCode")
        reportChallengeError(errorCode)
        finish()
    }

    override fun onRestoreAccount(account: String?) {
        val parentId = account?.takeIf(String::isNotBlank) ?: return
        val result = Bundle()
        result.putString(RESULT_PARENT_ID, parentId)
        sendResult(SOURCE_ACTIVITY_EVENT_PARENT_ID_RECEIVED, result)
    }

    override fun onCancelRequested() {
        if (uiState.userCancellationSent) return
        cancelTransfer()
    }

    override fun onConfirmed() {
        if (uiState.userCancellationSent || isFinishing || isDestroyed) return
        completeUserConfirmation(lockScreenConfirmed = false, authenticationType = 0)
    }

    override fun onCancelled() {
        if (uiState.userCancellationSent) return
        val pickerVisible = supportFragmentManager
            .findFragmentById(R.id.d2d_fragment_container) is SourceAccountPickerFragment
        if (pickerVisible && accountPickerOptions?.kidsOnboardingEnforced == true) {
            showCancelSetupDialog()
        } else {
            cancelTransfer()
        }
    }

    override fun onAccountsSelected(selected: ArrayList<BootstrapAccount>) {
        if (uiState.userCancellationSent || uiState.selectionDispatched) return
        beginPickerSelection(selected)
    }

    override fun onSkipped() {
        if (uiState.userCancellationSent || uiState.selectionDispatched) return
        uiState.selectionDispatched = true
        sendResult(SOURCE_ACTIVITY_EVENT_BOOTSTRAP_SKIPPED)
        setResult(RESULT_CANCELED)
        finishAndRemoveTask()
    }

    override fun finishAndRemoveTask() {
        ownPendingIntent?.cancel()
        super.finishAndRemoveTask()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (accountPickerOptions?.kidsOnboardingEnforced == true) {
            showCancelSetupDialog()
        } else {
            cancelTransfer()
        }
    }

    override fun onDestroy() {
        cancelSetupDialog?.setOnDismissListener(null)
        cancelSetupDialog?.dismiss()
        cancelSetupDialog = null
        pendingContentFragment = null
        super.onDestroy()
        sendResult(SOURCE_ACTIVITY_EVENT_ACTIVITY_FINISHED)
        ownPendingIntent = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        uiState.pendingSelectedAccounts?.let {
            outState.putParcelableArrayList(STATE_PENDING_SELECTED_ACCOUNTS, ArrayList(it))
        }
        outState.putBoolean(STATE_SELECTION_DISPATCHED, uiState.selectionDispatched)
        outState.putBoolean(STATE_ACCOUNT_LOAD_PENDING, uiState.accountLoadPending)
        super.onSaveInstanceState(outState)
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        val fragment = pendingContentFragment ?: return
        pendingContentFragment = null
        replaceFragment(fragment)
    }

    private fun cancelTransfer() {
        if (uiState.userCancellationSent) return
        uiState.userCancellationSent = true
        sendResult(SOURCE_ACTIVITY_EVENT_USER_CANCELED_BOOTSTRAP)
        setResult(RESULT_CANCELED)
        finishAndRemoveTask()
    }

    private fun showCancelSetupDialog() {
        if (isFinishing || isDestroyed || cancelSetupDialog?.isShowing == true) return
        cancelSetupDialog = AlertDialog.Builder(this)
            .setTitle(R.string.d2d_cancel_setup_title)
            .setMessage(R.string.d2d_cancel_setup_message)
            .setNegativeButton(R.string.d2d_go_back, null)
            .setPositiveButton(R.string.d2d_cancel_setup_action) { _, _ -> cancelTransfer() }
            .create()
            .also { dialog ->
                dialog.setOnDismissListener {
                    if (cancelSetupDialog === dialog) cancelSetupDialog = null
                }
                dialog.show()
            }
    }

    private fun sendResult(code: Int, data: Bundle = Bundle.EMPTY) {
        receiver?.send(code, data)
    }

    private fun reportChallengeError(errorCode: Int) {
        val result = Bundle()
        result.putInt(RESULT_DEPENDENCY_ERROR_CODE, errorCode)
        sendResult(SOURCE_ACTIVITY_EVENT_CHALLENGE_WEBVIEW_ERROR, result)
    }

    private fun showTransferProgress() {
        val transitionTitle = intent.getStringExtra(EXTRA_TRANSITION_TITLE)?.takeIf(String::isNotBlank)
        val transitionDescription = intent.getStringExtra(EXTRA_TRANSITION_DESCRIPTION)?.takeIf(String::isNotBlank)
        showInProgress(transitionTitle, transitionDescription)
    }

    private fun showInProgress(
        titleText: CharSequence? = null,
        messageText: CharSequence? = null,
    ) {
        val fragment = SourceProgressFragment.newInstance(
            title = titleText?.toString()?.takeIf(String::isNotBlank),
            message = messageText?.toString()?.takeIf(String::isNotBlank),
        )
        replaceFragment(fragment)
    }

    private fun replaceFragment(fragment: Fragment) {
        if (isFinishing || isDestroyed) return
        if (supportFragmentManager.isStateSaved) {
            pendingContentFragment = fragment
            return
        }
        pendingContentFragment = null
        supportFragmentManager.beginTransaction()
            .replace(R.id.d2d_fragment_container, fragment)
            .commit()
    }

    protected fun handleV2ConfigurationChanged() {
        // Leave the picker intact while system authentication is in flight.
        if (supportFragmentManager.findFragmentByTag(LOCK_SCREEN_FRAGMENT_TAG) is LockScreenFragment) {
            return
        }
        val current = supportFragmentManager.findFragmentById(R.id.d2d_fragment_container)
            ?: return
        when (current) {
            is AccountChallengeFragment -> current.reload()
            is LockScreenFragment -> Unit
            is SourceConfirmationFragment -> showTransferConfirmation()
            is SourceAccountPickerFragment -> showAccountPicker(current.accountSnapshot())
            is SourceProgressFragment -> showTransferProgress()
        }
    }

    @Suppress("DEPRECATION")
    private fun configureWindow() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        if (Build.VERSION.SDK_INT >= 21) {
            @Suppress("DEPRECATION")
            val surface = resources.getColor(R.color.d2d_surface)
            window.statusBarColor = surface
            window.navigationBarColor = surface
        }
        if (Build.VERSION.SDK_INT >= 23) {
            var flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= 26) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            @Suppress("DEPRECATION")
            run { window.decorView.systemUiVisibility = flags }
        }
    }

    companion object {
        private const val TAG = "SourceDirectTransferUI"
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
        private const val SUPERVISED_ACCOUNT_FEATURE = "service_usm"
        const val EXTRA_ACCOUNT_CHALLENGE_DATA = "extraAccountChallengeData"
        const val EXTRA_RESULT_RECEIVER = "resultReceiver"
        const val EXTRA_WAIT_FOR_CHALLENGE = "org.microg.gms.smartdevice.extra.WAIT_FOR_CHALLENGE"
        const val EXTRA_PENDING_INTENT = "pendingIntent"
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_SHOW_SKIP_ACCOUNT = "showSkipAccount"
        const val EXTRA_ACCOUNT_PICKER_ENABLED = "accountPickerEnabled"
        const val EXTRA_ACCOUNT_PICKER_OPTIONS = "accountPickerOptions"
        const val EXTRA_HAS_USER_CONFIRMED = "hasUserConfirmed"
        const val EXTRA_IS_FIDO = "isFido"
        const val EXTRA_UNICORN_CHALLENGE_DEDUPLICATION_ENABLED =
            "unicornChallengeDeduplicationEnabled"
        const val EXTRA_TARGET_ACCEPTS_MANAGED_ACCOUNTS = "targetAcceptsManagedAccounts"
        const val EXTRA_UI_CUSTOMIZATION = "uiCustomization"
        const val EXTRA_CONFIRMATION_TITLE = "confirmationTitle"
        const val EXTRA_CONFIRMATION_BODY = "styledConfirmationText"
        const val EXTRA_CONFIRMATION_BUTTON = "confirmButton"
        const val EXTRA_TRANSITION_TITLE = "transitionTitle"
        const val EXTRA_TRANSITION_DESCRIPTION = "transitionDescription"
        const val RESULT_RESULT_RECEIVER = "resultReceiver"
        const val RESULT_SELECTED_ACCOUNTS = "selectedAccounts"
        const val RESULT_SELECTED_CHILD_ID = "selectedChildId"
        const val RESULT_ACCOUNT_SELECTION_TOKEN = "accountSelectionToken"
        const val RESULT_ACCOUNT_CHALLENGE_DATA = "accountChallengeData"
        const val RESULT_LOCK_SCREEN_CONFIRMED = "lockScreenConfirmed"
        const val RESULT_LOCK_SCREEN_AUTH_TYPE = "lockScreenAuthType"
        const val RESULT_DEPENDENCY_ERROR_CODE = "dependencyErrorCode"
        const val RESULT_PARENT_ID = "parentId"
        private const val STATE_PENDING_SELECTED_ACCOUNTS = "pendingSelectedAccounts"
        private const val STATE_SELECTION_DISPATCHED = "selectionDispatched"
        private const val STATE_ACCOUNT_LOAD_PENDING = "accountLoadPending"
        private const val LOCK_SCREEN_FRAGMENT_TAG = "sourceLockScreenCoordinator"
    }
}

class SourceDirectTransferActivityV1 : SourceDirectTransferActivity()

class SourceDirectTransferActivityV2 : SourceDirectTransferActivity() {
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleV2ConfigurationChanged()
    }
}

private class SourceTransferUiState {
    var pendingSelectedAccounts: ArrayList<BootstrapAccount>? = null
    var selectionDispatched = false
    var accountLoadPending = false
    var userCancellationSent = false
}

private fun isAccountSelectable(
    account: BootstrapAccount,
    options: AccountPickerOptions?,
): Boolean {
    val supervisedSupported = options?.supervisedAccountsSupported == true
    val corpSupported = options?.corpAccountsSupported == true
    val isGoogleCorpAccount = account.name?.endsWith(GOOGLE_CORP_ACCOUNT_SUFFIX) == true
    return (supervisedSupported || !account.isSupervised) &&
        (corpSupported || !isGoogleCorpAccount)
}

private class ProxyResultReceiver(
    handler: Handler?,
    activity: SourceDirectTransferActivity,
) : ResultReceiver(handler) {
    private val activity = WeakReference(activity)

    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
        activity.get()?.onServiceResult(resultCode, resultData)
    }
}

@Suppress("DEPRECATION")
private fun android.content.Intent.readResultReceiver(): ResultReceiver? =
    getParcelableExtra(SourceDirectTransferActivity.EXTRA_RESULT_RECEIVER)

@Suppress("DEPRECATION")
private fun android.content.Intent.readAccountPickerOptions(): AccountPickerOptions? =
    getByteArrayExtra(SourceDirectTransferActivity.EXTRA_ACCOUNT_PICKER_OPTIONS)?.let { bytes ->
        SafeParcelableSerializer.deserializeFromBytes(bytes, AccountPickerOptions.CREATOR)
    }

@Suppress("DEPRECATION")
private fun android.content.Intent.readAccountChallengeData(): ArrayList<Bundle>? =
    getParcelableArrayListExtra(SourceDirectTransferActivity.EXTRA_ACCOUNT_CHALLENGE_DATA)

@Suppress("DEPRECATION")
private fun Bundle?.readAccountChallengeData(): ArrayList<Bundle>? =
    this?.getParcelableArrayList(SourceDirectTransferActivity.EXTRA_ACCOUNT_CHALLENGE_DATA)

private fun List<BootstrapAccount>.marshallTypedList(): ByteArray {
    val parcel = Parcel.obtain()
    return try {
        parcel.writeTypedList(this)
        parcel.marshall()
    } finally {
        parcel.recycle()
    }
}
