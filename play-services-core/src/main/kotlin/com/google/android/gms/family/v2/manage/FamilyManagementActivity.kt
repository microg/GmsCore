/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.manage

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.family.model.MemberDataModel
import com.google.android.gms.family.v2.manage.fragment.FamilyManagementFragment
import com.google.android.gms.family.v2.manage.fragment.MemberDetailFragment
import com.google.android.gms.family.v2.manage.model.FamilyViewModel
import com.google.android.gms.family.v2.manage.ui.FamilyActivityScreen
import kotlinx.coroutines.launch
import org.microg.gms.profile.ProfileManager

class FamilyManagementActivity : AppCompatActivity() {
    private val familyViewModel by viewModels<FamilyViewModel>()
    private var deleteFamily = false
    private val callerAppId: String?
        get() = intent?.getStringExtra(EXTRA_KEY_APP_ID)
    private val accountName: String?
        get() = intent?.getStringExtra(EXTRA_KEY_ACCOUNT_NAME)
    private val themeType: String?
        get() = intent?.getStringExtra(EXTRA_KEY_PREDEFINED_THEME)

    private val deletedLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val consistencyToken = result.data?.getStringExtra(EXTRA_KEY_CONSISTENCY_TOKEN)
        Log.d(TAG, "consistencyToken: $consistencyToken")
        if (consistencyToken != null) {
            if (!deleteFamily && backToManagement()) {
                familyViewModel.refreshData()
                return@registerForActivityResult
            }
            onResult(accountName, consistencyToken)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action != null && intent.action != ACTION_FAMILY_MANAGEMENT) {
            errorResult("FamilyManagementActivity: Intent has unexpected action")
            return finish()
        }

        val callingPackageName = callingPackage ?: callingActivity?.packageName

        if (callingPackageName.isNullOrEmpty()) {
            errorResult("FamilyManagementActivity: callingPackageName is empty", -3)
            return finish()
        }

        if (accountName.isNullOrEmpty()) {
            errorResult("FamilyManagementActivity: accountName is empty", -2)
            return finish()
        }

        ProfileManager.ensureInitialized(this)
        setContent {
            FamilyActivityScreen(
                viewModel = familyViewModel,
                type = themeType,
                addFragment = { addManagementFragment(it, callingPackageName) },
                onBackClick = { onBackPressed() },
                onMoreClick = { currentMember, leave -> executeFamilyGroupByAction(currentMember, callingPackageName, leave) }
            )
        }
        lifecycleScope.launch {
            familyViewModel.selectedMember.collect {
                val containerId = it.first
                val member = it.second
                if (containerId != null) {
                    val detailFragment = MemberDetailFragment.newInstance(member, accountName!!, callerAppId!!)
                    val managementFragment = supportFragmentManager.findFragmentByTag(FamilyManagementFragment.TAG)
                    val transaction = supportFragmentManager.beginTransaction()
                    if (managementFragment != null) {
                        transaction.hide(managementFragment).add(containerId, detailFragment, MemberDetailFragment.TAG)
                    } else{
                        transaction.replace(containerId, detailFragment, MemberDetailFragment.TAG)
                    }
                    transaction.commit()
                    return@collect
                }
                executeFamilyGroupByAction(member, callingPackageName, true)
            }
        }
    }

    override fun onBackPressed() {
        if (backToManagement()) {
            return
        }
        super.onBackPressed()
    }

    private fun backToManagement(): Boolean {
        try {
            val memberDetailFragment = supportFragmentManager.findFragmentByTag(MemberDetailFragment.TAG)
            if (memberDetailFragment != null) {
                val managementFragment = supportFragmentManager.findFragmentByTag(FamilyManagementFragment.TAG)
                if (managementFragment != null) {
                    supportFragmentManager.beginTransaction().show(managementFragment).remove(memberDetailFragment).commit()
                    return true
                }
            }
        } catch (e: Exception){
            Log.d(TAG, "backToManagement: ", e)
        }
        return false
    }

    private fun addManagementFragment(container: View, callingPackage: String) {
        val activity = container.context as? AppCompatActivity
        val fragmentManager = activity?.supportFragmentManager
        val containerId = container.id

        fragmentManager?.apply {
            if (findFragmentByTag(FamilyManagementFragment.TAG) == null) {
                val managementFragment = FamilyManagementFragment.newInstance(accountName!!, callerAppId!!, callingPackage)
                commit {
                    setReorderingAllowed(true)
                    replace(containerId, managementFragment, FamilyManagementFragment.TAG)
                }
            }
        }
    }

    private fun executeFamilyGroupByAction(member: MemberDataModel, callingPackageName: String, leaveFamily: Boolean) {
        deleteFamily = !leaveFamily
        deletedLauncher.launch(Intent(this, DeleteMemberActivity::class.java).apply {
            putExtra(EXTRA_KEY_ACCOUNT_NAME, accountName)
            putExtra(EXTRA_KEY_APP_ID, callerAppId)
            putExtra(EXTRA_KEY_PREDEFINED_THEME, themeType)
            putExtra(EXTRA_KEY_CLIENT_CALLING_PACKAGE, callingPackageName)
            putExtra(EXTRA_KEY_MEMBER_ID, member.memberId)
            putExtra(EXTRA_KEY_MEMBER_GIVEN_NAME, member.displayName.ifEmpty { member.email })
            putExtra(EXTRA_KEY_MEMBER_HOH_GIVEN_NAME, member.hohGivenName.ifEmpty { member.roleName })
            putExtra(EXTRA_KEY_MEMBER_LEAVE_FAMILY, leaveFamily)
        })
    }

}

