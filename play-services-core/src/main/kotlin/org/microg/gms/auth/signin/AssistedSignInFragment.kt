/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.signin

import android.accounts.Account
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.people.PeopleManager
import org.microg.gms.utils.getApplicationLabel

class AssistedSignInFragment(
    private val options: GoogleSignInOptions,
    private val beginSignInRequest: BeginSignInRequest,
    private val accounts: Array<Account>,
    private val clientPackageName: String,
    private val errorBlock: (Status) -> Unit,
    private val loginBlock: (GoogleSignInAccount?) -> Unit
) : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "AssistedSignInFragment"
    }

    private var cancelBtn: ImageView? = null
    private var container: FrameLayout? = null
    private var loginJob: Job? = null
    private var isSigningIn = false

    private var lastChooseAccount: Account? = null
    private var lastChooseAccountPermitted = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "onActivityCreated start")
        lifecycleScope.launch {
            filterAccountsLogin({
                prepareMultiSignIn(it)
            }, { account, permitted ->
                autoSingleSignIn(account, permitted)
            })
        }
    }

    private fun autoSingleSignIn(account: Account, permitted: Boolean = false) {
        if (beginSignInRequest.isAutoSelectEnabled) {
            prepareSignInLoading(account, permitted = permitted) { cancelLogin(true) }
        } else {
            prepareChooseLogin(account, permitted = permitted)
        }
    }

    private fun filterAccountsLogin(multiMethod: (List<Account>) -> Unit, loginMethod: (Account, Boolean) -> Unit) {
        lifecycleScope.launch {
            val allowAutoLoginAccounts = mutableListOf<Account>()
            accounts.forEach { account ->
                val authStatus = checkAppAuthStatus(requireContext(), clientPackageName, options, account)
                if (authStatus) {
                    allowAutoLoginAccounts.add(account)
                }
            }
            if (accounts.size == 1) {
                loginMethod(accounts.first(), allowAutoLoginAccounts.isNotEmpty())
                return@launch
            }
            val filterByAuthorizedAccounts = beginSignInRequest.googleIdTokenRequestOptions.filterByAuthorizedAccounts()
            if (!filterByAuthorizedAccounts) {
                multiMethod(allowAutoLoginAccounts)
                return@launch
            }
            if (allowAutoLoginAccounts.size == 1) {
                loginMethod(allowAutoLoginAccounts.first(), true)
                return@launch
            }
            multiMethod(allowAutoLoginAccounts)
        }
    }

    private fun prepareMultiSignIn(allowAutoLoginAccounts: List<Account>) {
        lifecycleScope.launch {
            notifyCancelBtn(true)
            container?.removeAllViews()
            val chooseView = LayoutInflater.from(requireContext()).inflate(R.layout.assisted_signin_choose_layout, null)
            val clientAppLabel = requireContext().packageManager.getApplicationLabel(clientPackageName)
            chooseView.findViewById<TextView>(R.id.sign_multi_description).text =
                String.format(getString(R.string.credentials_assisted_choose_account_subtitle), clientAppLabel)
            val accountViews = chooseView.findViewById<LinearLayout>(R.id.sign_multi_account_container)
            accounts.forEachIndexed { index, account ->
                val accountView =
                    LayoutInflater.from(requireContext()).inflate(R.layout.assisted_signin_multi_layout, null)
                accountView.findViewById<TextView>(R.id.account_email).text = account.name
                withContext(Dispatchers.IO) {
                    PeopleManager.getDisplayName(requireContext(), account.name)
                }.let { accountView.findViewById<TextView>(R.id.account_display_name).text = it }
                withContext(Dispatchers.IO) {
                    PeopleManager.getOwnerAvatarBitmap(requireContext(), account.name, false)
                        ?: PeopleManager.getOwnerAvatarBitmap(requireContext(), account.name, true)
                }.let { accountView.findViewById<ImageView>(R.id.account_photo).setImageBitmap(it) }
                accountView.findViewById<TextView>(R.id.account_description).text =
                    getString(R.string.credentials_assisted_signin_button_text_long)
                if (index == accounts.size - 1) {
                    accountView.findViewById<View>(R.id.multi_account_line).visibility = View.GONE
                }
                accountView.setOnClickListener {
                    chooseView.findViewById<ProgressBar>(R.id.sign_multi_progress).visibility = View.VISIBLE
                    prepareSignInLoading(account, permitted = allowAutoLoginAccounts.any { it == account })
                }
                accountViews.addView(accountView)
            }
            container?.addView(chooseView)
        }
    }

    private fun prepareSignInLoading(account: Account, permitted: Boolean = false, cancelBlock: (() -> Unit)? = null) {
        lifecycleScope.launch {
            notifyCancelBtn(false)
            container?.removeAllViews()
            val loadingView =
                LayoutInflater.from(requireContext()).inflate(R.layout.assisted_signin_loading_layout, null)
            loadingView.findViewById<TextView>(R.id.sign_account_email).text = account.name
            withContext(Dispatchers.IO) {
                PeopleManager.getDisplayName(requireContext(), account.name)
            }.let { loadingView.findViewById<TextView>(R.id.sign_account_display_name).text = it }
            withContext(Dispatchers.IO) {
                PeopleManager.getOwnerAvatarBitmap(requireContext(), account.name, false)
                    ?: PeopleManager.getOwnerAvatarBitmap(requireContext(), account.name, true)
            }.let { loadingView.findViewById<ImageView>(R.id.sign_account_photo).setImageBitmap(it) }
            if (cancelBlock != null) {
                loadingView.findViewById<TextView>(R.id.sign_cancel).visibility = View.VISIBLE
                loadingView.findViewById<TextView>(R.id.sign_cancel).setOnClickListener {
                    cancelBlock()
                }
            }
            container?.addView(loadingView)
            startLogin(account, permitted)
        }
    }

    private fun prepareChooseLogin(account: Account, showConsent: Boolean = false, permitted: Boolean = false) {
        lifecycleScope.launch {
            notifyCancelBtn(true)
            container?.removeAllViews()
            val reloadView =
                LayoutInflater.from(requireContext()).inflate(R.layout.assisted_signin_back_consent_layout, null)
            reloadView.findViewById<TextView>(R.id.sign_account_email).text = account.name
            withContext(Dispatchers.IO) {
                PeopleManager.getDisplayName(requireContext(), account.name)
            }.let { reloadView.findViewById<TextView>(R.id.sign_account_display_name).text = it }
            withContext(Dispatchers.IO) {
                PeopleManager.getOwnerAvatarBitmap(requireContext(), account.name, false)
                    ?: PeopleManager.getOwnerAvatarBitmap(requireContext(), account.name, true)
            }.let { reloadView.findViewById<ImageView>(R.id.sign_account_photo).setImageBitmap(it) }
            withContext(Dispatchers.IO) {
                PeopleManager.getGivenName(requireContext(), account.name)
            }.let {
                reloadView.findViewById<MaterialButton>(R.id.sign_reloading_back).text =
                    if (it.isNullOrEmpty()) getString(R.string.credentials_assisted_continue) else String.format(
                        getString(R.string.credentials_assisted_continue_as_user_button_label), it
                    )
            }
            reloadView.findViewById<MaterialButton>(R.id.sign_reloading_back).setOnClickListener {
                reloadView.findViewById<ProgressBar>(R.id.sign_reloading_progress).visibility = View.VISIBLE
                prepareSignInLoading(account, permitted)
            }
            val clientAppLabel = requireContext().packageManager.getApplicationLabel(clientPackageName)
            reloadView.findViewById<TextView>(R.id.sign_reloading_title).text = if (showConsent) String.format(
                getString(R.string.credentials_assisted_signin_consent_header), clientAppLabel
            ) else String.format(getString(R.string.credentials_assisted_sign_back_title), clientAppLabel)
            val consentTextView = reloadView.findViewById<TextView>(R.id.sign_reloading_consent)
            consentTextView.text =
                String.format(getString(R.string.credentials_assisted_signin_consent), clientAppLabel)
            consentTextView.visibility = if (showConsent) View.VISIBLE else View.GONE
            container?.addView(reloadView)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            dialog.behavior.skipCollapsed = true
            dialog.setCanceledOnTouchOutside(false)
        }
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dialog.dismiss()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView")
        return layoutInflater.inflate(R.layout.assisted_signin_google_dialog, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        cancelBtn = view.findViewById(R.id.cancel)
        cancelBtn?.setOnClickListener {
            dismiss()
        }
        container = view.findViewById(R.id.google_sign_in_container)
    }

    override fun onDismiss(dialog: DialogInterface) {
        val assistedSignInActivity = requireContext() as AssistedSignInActivity
        if (!assistedSignInActivity.isChangingConfigurations && !isSigningIn) {
            errorBlock(Status(CommonStatusCodes.CANCELED, "User cancelled."))
        }
        cancelLogin()
        super.onDismiss(dialog)
    }

    private fun notifyCancelBtn(visible: Boolean) {
        cancelBtn?.visibility = if (visible) View.VISIBLE else View.GONE
        cancelBtn?.isClickable = visible
    }

    private fun startLogin(account: Account, permitted: Boolean = false) {
        loginJob = lifecycleScope.launch {
            lastChooseAccount = account
            lastChooseAccountPermitted = permitted
            isSigningIn = true
            delay(3000)
            val googleSignInAccount = withContext(Dispatchers.IO) {
                performSignIn(requireContext(), clientPackageName, options, account, permitted)
            }
            if (googleSignInAccount == null) {
                isSigningIn = false
                prepareChooseLogin(account, showConsent = true, permitted = true)
                return@launch
            }
            loginBlock(googleSignInAccount)
        }
    }

    fun cancelLogin(showChoose: Boolean = false) {
        Log.d(TAG, "cancelLogin")
        isSigningIn = false
        loginJob?.cancel()
        if (showChoose && lastChooseAccount != null) {
            prepareChooseLogin(lastChooseAccount!!, permitted = lastChooseAccountPermitted)
        }
    }

}