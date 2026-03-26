/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.manage.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.R
import com.google.android.gms.common.images.ImageManager
import com.google.android.gms.family.model.MemberDataModel
import com.google.android.gms.family.v2.manage.EXTRA_KEY_ACCOUNT_NAME
import com.google.android.gms.family.v2.manage.EXTRA_KEY_APP_ID
import com.google.android.gms.family.v2.manage.EXTRA_KEY_MEMBER_MODEL
import com.google.android.gms.family.v2.manage.model.FamilyViewModel
import com.google.android.gms.family.v2.manage.showToast
import com.google.android.gms.family.v2.manage.ui.MemberDetailItem
import kotlinx.coroutines.launch

class MemberDetailFragment : Fragment() {
    private val familyViewModel by activityViewModels<FamilyViewModel>()

    private val callerAppId: String?
        get() = arguments?.getString(EXTRA_KEY_APP_ID)
    private val accountName: String?
        get() = arguments?.getString(EXTRA_KEY_ACCOUNT_NAME)
    private val memberModel: MemberDataModel
        get() = arguments?.getParcelable(EXTRA_KEY_MEMBER_MODEL) ?: MemberDataModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MemberDetailItem(
                    viewModel = familyViewModel,
                    member = memberModel,
                    onMemberClick = ::onMemberClick,
                    loadImage = ::loadMemberAvatar
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        familyViewModel.updateUIState(false, memberModel.displayName)
    }

    private fun onMemberClick(member: MemberDataModel) {
        if (member.isInvited) {
            lifecycleScope.launch {
                val deleteInvitationState = familyViewModel.deleteInvitationMember(
                    requireContext(),
                    accountName!!,
                    callerAppId!!,
                    member.invitationId
                )
                runCatching {
                    if (deleteInvitationState){
                        requireActivity().showToast(getString(R.string.family_management_cancel_invite_success))
                        requireActivity().onBackPressed()
                        return@launch
                    }
                    requireActivity().showToast(getString(R.string.family_management_cancel_invite_error))
                }
            }
            return
        }
        lifecycleScope.launch {
            familyViewModel.selectMember(null, member)
        }
    }

    private fun loadMemberAvatar(url: String?, view: ImageView) {
        runCatching {
            ImageManager.create(requireContext()).loadImage(url, view)
        }
    }

    companion object {
        const val TAG = "MemberDetailFragment"
        fun newInstance(member: MemberDataModel, accountName: String, appId: String): MemberDetailFragment {
            val memberDetailFragment = MemberDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(EXTRA_KEY_MEMBER_MODEL, member)
                    putString(EXTRA_KEY_ACCOUNT_NAME, accountName)
                    putString(EXTRA_KEY_APP_ID, appId)
                }
            }
            return memberDetailFragment
        }
    }
}