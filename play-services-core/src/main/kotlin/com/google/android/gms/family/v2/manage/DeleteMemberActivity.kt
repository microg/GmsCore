/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.manage

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.google.android.gms.family.v2.manage.fragment.FamilyDeleteFragment
import com.google.android.gms.family.v2.manage.model.FamilyViewModel
import com.google.android.gms.family.v2.manage.ui.FamilyActivityScreen

class DeleteMemberActivity : AppCompatActivity() {

    private val familyViewModel by viewModels<FamilyViewModel>()

    private val themeType: String?
        get() = intent?.getStringExtra(EXTRA_KEY_PREDEFINED_THEME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callingPackageName = callingPackage ?: callingActivity?.packageName
        if (callingPackageName.isNullOrEmpty()) {
            errorResult("DeleteMemberActivity: callingPackageName is empty", -3)
            return finish()
        }
        val extras = intent.extras
        if (extras == null) {
            errorResult("DeleteMemberActivity: extras is empty", -3)
            return finish()
        }
        setContent {
            FamilyActivityScreen(
                viewModel = familyViewModel,
                type = themeType,
                addFragment = { addDeleteFragment(it, extras) },
                onBackClick = { onBackPressed() }
            )
        }
    }

    private fun addDeleteFragment(container: View, bundle: Bundle) {
        val activity = container.context as? AppCompatActivity
        val fragmentManager = activity?.supportFragmentManager
        val containerId = container.id
        fragmentManager?.apply {
            if (findFragmentByTag(FamilyDeleteFragment.TAG) == null) {
                val deleteFragment = FamilyDeleteFragment.newInstance(bundle)
                commit {
                    setReorderingAllowed(true)
                    replace(containerId, deleteFragment, FamilyDeleteFragment.TAG)
                }
            }
        }
    }
}