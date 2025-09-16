/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.appinivite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.os.bundleOf
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.firebase.dynamiclinks.internal.DynamicLinkData
import org.microg.gms.appinvite.MutateAppInviteLinkResponse
import org.microg.gms.appinivite.utils.DynamicLinkUtils
import org.microg.gms.utils.singleInstanceOf

private const val TAG = "AppInviteActivity"

private const val APPINVITE_DEEP_LINK = "com.google.android.gms.appinvite.DEEP_LINK"
private const val APPINVITE_INVITATION_ID = "com.google.android.gms.appinvite.INVITATION_ID"
private const val APPINVITE_OPENED_FROM_PLAY_STORE = "com.google.android.gms.appinvite.OPENED_FROM_PLAY_STORE"
private const val APPINVITE_REFERRAL_BUNDLE = "com.google.android.gms.appinvite.REFERRAL_BUNDLE"
private const val DYNAMIC_LINK_DATA = "com.google.firebase.dynamiclinks.DYNAMIC_LINK_DATA"

class AppInviteActivity : AppCompatActivity() {
    private val queue by lazy { singleInstanceOf { Volley.newRequestQueue(applicationContext) } }

    private val Int.px: Int get() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(ProgressBar(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(20.px)
            isIndeterminate = true
        })
        val extras = intent.extras
        extras?.keySet()
        Log.d(TAG, "Intent: $intent $extras")
        if (intent?.data == null) return finish()
        lifecycleScope.launchWhenStarted {
            val response = DynamicLinkUtils.requestLinkResponse(intent.data.toString(), queue) ?: return@launchWhenStarted redirectToBrowser()
            open(response)
        }
    }

    private fun redirectToBrowser() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                data = intent.data
            })
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
        finish()
    }

    private fun open(appInviteLink: MutateAppInviteLinkResponse) {
        val minAppVersion = appInviteLink.data_?.app?.minAppVersion
        val dynamicLinkData = DynamicLinkData(appInviteLink.metadata?.info?.url, appInviteLink.data_?.intentData,
            (minAppVersion ?: 0).toInt(), System.currentTimeMillis(), null, null)
        val linkPackageName = appInviteLink.data_?.packageName
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = appInviteLink.data_?.intentData?.let { Uri.parse(it) }
            `package` = linkPackageName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(
                APPINVITE_REFERRAL_BUNDLE, bundleOf(
                    APPINVITE_DEEP_LINK to appInviteLink,
                    APPINVITE_INVITATION_ID to "",
                    APPINVITE_OPENED_FROM_PLAY_STORE to false
                )
            )
            putExtra(DYNAMIC_LINK_DATA, SafeParcelableSerializer.serializeToBytes(dynamicLinkData))
        }
        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = appInviteLink.data_?.fallbackUrl?.let { Uri.parse(it) }
        }
        val installedVersionCode = runCatching {
            if (linkPackageName != null) {
                PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(linkPackageName, 0))
            } else {
                null
            }
        }.getOrNull()
        if (installedVersionCode != null && (minAppVersion == null || installedVersionCode >= minAppVersion)) {
            val componentName = intent.resolveActivity(packageManager)
            if (componentName == null) {
                Log.w(TAG, "open resolve activity is null")
                if (linkPackageName != null) {
                    val intentLaunch =
                        packageManager.getLaunchIntentForPackage(linkPackageName)
                    if (intentLaunch != null) {
                        intent.setComponent(intentLaunch.component)
                    }
                }
            }
            startActivity(intent)
            finish()
        } else {
            try {
                startActivity(fallbackIntent)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
            finish()
        }
    }
}