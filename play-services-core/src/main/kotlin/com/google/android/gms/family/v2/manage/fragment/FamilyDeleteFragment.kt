/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.manage.fragment

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.R
import com.google.android.gms.family.model.MemberDataModel
import com.google.android.gms.family.v2.manage.EXTRA_KEY_ACCOUNT_NAME
import com.google.android.gms.family.v2.manage.EXTRA_KEY_APP_ID
import com.google.android.gms.family.v2.manage.EXTRA_KEY_MEMBER_GIVEN_NAME
import com.google.android.gms.family.v2.manage.EXTRA_KEY_MEMBER_ID
import com.google.android.gms.family.v2.manage.EXTRA_KEY_MEMBER_LEAVE_FAMILY
import com.google.android.gms.family.v2.manage.errorResult
import com.google.android.gms.family.v2.manage.model.FamilyChangedState
import com.google.android.gms.family.v2.manage.model.FamilyViewModel
import com.google.android.gms.family.v2.manage.onResult
import com.google.android.gms.family.v2.manage.showToast
import com.google.android.gms.family.v2.manage.ui.FamilyDeleteFragmentScreen
import com.google.android.gms.family.v2.model.HelpData
import com.google.android.gms.googlehelp.GoogleHelp
import kotlinx.coroutines.launch
import org.microg.gms.auth.AuthConstants
import org.microg.gms.family.FamilyRole
import org.microg.gms.googlehelp.ui.GoogleHelpRedirectActivity

class FamilyDeleteFragment : Fragment() {
    private val familyViewModel by activityViewModels<FamilyViewModel>()

    private val callerAppId: String?
        get() = arguments?.getString(EXTRA_KEY_APP_ID)
    private val accountName: String?
        get() = arguments?.getString(EXTRA_KEY_ACCOUNT_NAME)
    private val memberId: String?
        get() = arguments?.getString(EXTRA_KEY_MEMBER_ID)
    private val memberGivenName: String?
        get() = arguments?.getString(EXTRA_KEY_MEMBER_GIVEN_NAME)
    private val leaveFamily: Boolean
        get() = arguments?.getBoolean(EXTRA_KEY_MEMBER_LEAVE_FAMILY, false) ?: false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (accountName == null) {
            requireActivity().errorResult("FamilyDeleteFragment: accountName is empty", -2)
            return requireActivity().finish()
        }
        if (AccountManager.get(requireContext()).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).find { it.name == accountName } == null) {
            requireActivity().errorResult("FamilyDeleteFragment: accountName is invalid", -2)
            return requireActivity().finish()
        }
        if (callerAppId == null) {
            requireActivity().errorResult("FamilyDeleteFragment: callerAppId is empty", -2)
            return requireActivity().finish()
        }
        if (memberId == null) {
            requireActivity().errorResult("FamilyDeleteFragment: memberId is null", -2)
            return requireActivity().finish()
        }
        if (memberGivenName.isNullOrEmpty()) {
            requireActivity().errorResult("FamilyDeleteFragment: memberGivenName is empty", -2)
            return requireActivity().finish()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            familyViewModel.familyChangedStateState.collect {
                when (it) {
                    is FamilyChangedState.Idle -> Unit
                    is FamilyChangedState.Changed -> onMemberChanged(it.token)
                    is FamilyChangedState.Error -> requireActivity().errorResult(it.message, it.code, accountName)
                }
            }
        }
        lifecycleScope.launch { familyViewModel.refreshing.collect { loadContentData() } }
    }

    private fun onMemberChanged(consistencyToken: String) {
        Log.d(TAG, "onMemberChanged: consistencyToken: $consistencyToken")
        requireActivity().onResult(accountName, consistencyToken)
    }

    private fun loadContentData() {
        familyViewModel.loadFamilyManagementPageContent(
            requireContext(), accountName!!, callerAppId!!, memberId!!, leaveFamily
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                FamilyDeleteFragmentScreen(
                    viewModel = familyViewModel,
                    onHelpClick = ::showHelpPage,
                    displayName = memberGivenName!!,
                    leaveFamily = leaveFamily,
                    onCancelDelete = { requireActivity().onBackPressed() },
                    onValidatePassword = ::validatePassword,
                    onCheckPasswordSuccess = ::executeDeleteOperation
                )
            }
        }
    }

    private fun executeDeleteOperation(currentMember: MemberDataModel) {
        lifecycleScope.launch {
            if (leaveFamily) {
                val deleted = familyViewModel.deleteMember(requireContext(), accountName!!, callerAppId!!, memberId!!)
                if (deleted) {
                    requireActivity().also {
                        val message = getString(R.string.family_management_member_removed_success, memberGivenName)
                        it.showToast(message)
                    }.finish()
                    return@launch
                }
                requireActivity().showToast(getString(R.string.family_management_member_remove_failed, memberGivenName))
                return@launch
            }
            if (currentMember.role == FamilyRole.HEAD_OF_HOUSEHOLD.value) {
                val deleted = familyViewModel.deleteFamily(requireContext(), accountName!!, callerAppId!!)
                if (deleted) {
                    requireActivity().also {
                        it.showToast(getString(R.string.family_management_delete_group_success))
                    }.finish()
                    return@launch
                }
                requireActivity().showToast(getString(R.string.family_management_delete_group_failure))
                return@launch
            }
            val deleted = familyViewModel.deleteMember(requireContext(), accountName!!, callerAppId!!, memberId!!)
            if (deleted) {
                requireActivity().also {
                    it.showToast(getString(R.string.family_management_exist_group_success))
                }.finish()
                return@launch
            }
            requireActivity().showToast(getString(R.string.family_management_leave_family_error_message))
        }
    }

    private fun showHelpPage(helpData: HelpData) {
        Intent(requireActivity(), GoogleHelpRedirectActivity::class.java).apply {
            val googleHelp = GoogleHelp().apply {
                appContext = helpData.appContext
                uri = helpData.linkUrl.toUri()
            }
            putExtra(GoogleHelpRedirectActivity.GOOGLE_HELP_KEY, googleHelp)
            putExtra(GoogleHelpRedirectActivity.KEY_PACKAGE_NAME, requireActivity().packageName)
        }.let { requireActivity().startActivity(it) }
    }

    private fun validatePassword(password: String, member: MemberDataModel) {
        familyViewModel.validatePassword(requireContext(), accountName!!, password, member)
    }

    companion object {
        const val TAG = "FamilyDeleteFragment"
        fun newInstance(bundle: Bundle): FamilyDeleteFragment {
            val fragment = FamilyDeleteFragment().apply {
                arguments = bundle
            }
            return fragment
        }
    }
}