/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.manage.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.R
import com.google.android.gms.common.images.ImageManager
import com.google.android.gms.family.model.MemberDataModel
import com.google.android.gms.family.v2.manage.EXTRA_KEY_ACCOUNT_NAME
import com.google.android.gms.family.v2.manage.EXTRA_KEY_APP_ID
import com.google.android.gms.family.v2.manage.EXTRA_KEY_CALLING_PACKAGE_NAME
import com.google.android.gms.family.v2.manage.FAMILY_LINK_MEMBER_BASE_URL
import com.google.android.gms.family.v2.manage.buildFamilyInviteUrl
import com.google.android.gms.family.v2.manage.model.FamilyViewModel
import com.google.android.gms.family.v2.manage.ui.FamilyManagementFragmentScreen
import kotlinx.coroutines.launch
import org.microg.gms.accountsettings.ui.EXTRA_ACCOUNT_NAME
import org.microg.gms.accountsettings.ui.EXTRA_CALLING_PACKAGE_NAME
import org.microg.gms.accountsettings.ui.EXTRA_URL
import org.microg.gms.accountsettings.ui.MainActivity
import org.microg.gms.family.FamilyRole

class FamilyManagementFragment : Fragment() {
    private val familyViewModel by activityViewModels<FamilyViewModel>()

    private val callerAppId: String?
        get() = arguments?.getString(EXTRA_KEY_APP_ID)
    private val accountName: String?
        get() = arguments?.getString(EXTRA_KEY_ACCOUNT_NAME)
    private val callingPackageName: String?
        get() = arguments?.getString(EXTRA_KEY_CALLING_PACKAGE_NAME)

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        loadFamilyData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                FamilyManagementFragmentScreen(
                    viewModel = familyViewModel,
                    onMemberClick = ::onClickFamilyMember,
                    loadImage = ::loadMemberAvatar
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch { familyViewModel.refreshing.collect { loadFamilyData() } }
        familyViewModel.updateUIState(true, getString(R.string.family_management_title))
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            familyViewModel.updateUIState(true, getString(R.string.family_management_title))
            familyViewModel.refreshData()
        }
    }

    private fun loadFamilyData() {
        familyViewModel.loadFamilyMembers(
            requireContext(),
            accountName!!,
            callerAppId!!
        )
    }

    private fun onClickFamilyMember(member: MemberDataModel) {
        if (member.isInviteEntry) {
            resultLauncher.launch(Intent(requireContext(), MainActivity::class.java).apply {
                putExtra(EXTRA_URL, buildFamilyInviteUrl(callerAppId))
                putExtra(EXTRA_ACCOUNT_NAME, accountName)
                putExtra(EXTRA_CALLING_PACKAGE_NAME, callingPackageName)
            })
            return
        }
        if (member.role == FamilyRole.CHILD.value) {
            resultLauncher.launch(Intent(requireContext(), MainActivity::class.java).apply {
                putExtra(EXTRA_URL, "$FAMILY_LINK_MEMBER_BASE_URL${member.memberId}")
                putExtra(EXTRA_ACCOUNT_NAME, accountName)
                putExtra(EXTRA_CALLING_PACKAGE_NAME, callingPackageName)
            })
            return
        }
        lifecycleScope.launch {
            familyViewModel.selectMember((view?.parent as View).id, member)
        }
    }

    private fun loadMemberAvatar(url: String?, view: ImageView) {
        runCatching {
            ImageManager.create(requireContext()).loadImage(url, view)
        }
    }

    companion object {
        const val TAG = "FamilyManagementFragment"
        fun newInstance(accountName: String, appId: String, callingPackageName: String): FamilyManagementFragment {
            val fragment = FamilyManagementFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_KEY_ACCOUNT_NAME, accountName)
                    putString(EXTRA_KEY_APP_ID, appId)
                    putString(EXTRA_KEY_CALLING_PACKAGE_NAME, callingPackageName)
                }
            }
            return fragment
        }
    }
}