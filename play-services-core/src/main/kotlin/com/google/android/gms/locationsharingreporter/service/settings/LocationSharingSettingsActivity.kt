/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter.service.settings

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_FORWARD_RESULT
import android.graphics.Bitmap
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.R
import com.google.android.gms.locationsharingreporter.service.ReportingRequestStore
import com.google.android.gms.locationsharingreporter.service.ReportingRequestStoreFile
import com.google.android.gms.locationsharingreporter.service.readSharesResponseDetail
import com.google.android.gms.locationsharingreporter.service.requestReadShares
import com.google.android.gms.locationsharingreporter.service.sendLocationSharingEnable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.accountsettings.ui.EXTRA_ACCOUNT_NAME
import org.microg.gms.accountsettings.ui.EXTRA_URL
import org.microg.gms.accountsettings.ui.MainActivity
import org.microg.gms.auth.AuthConstants
import org.microg.gms.people.PeopleManager

class LocationSharingSettingsActivity : AppCompatActivity() {
    companion object {
        private val TAG = LocationSharingSettingsActivity::class.simpleName
        private const val LOCATION_SHARING_URL = "https://myaccount.google.com/locationsharing"
        private const val LEARN_MORE_LOCATION_SHARING_URL = "https://support.google.com/accounts/answer/9363497?co=GENIE.Platform=Android&visit_id=638923749787833764-2515936560&rd=1"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_sharing_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val accountName = intent.getStringExtra("account_name")
        Log.d(TAG, "account name: $accountName")
        if (accountName.isNullOrEmpty()) {
            Log.w(TAG, "Missing account name")
            finish()
        }
        val ivAvatar = findViewById<ImageView>(R.id.avatar_iv)
        if (accountName != null) {
            lifecycleScope.launchWhenStarted {
                withContext(Dispatchers.IO) {
                    PeopleManager.getOwnerAvatarBitmap(this@LocationSharingSettingsActivity, accountName, true)
                }?.let { ivAvatar.setImageDrawable(getCircleBitmapDrawable(it)) }
            }
        }

        findViewById<TextView>(R.id.account_email_tv).text = accountName

        val currentAccount = AccountManager.get(this).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).firstOrNull { it.name == accountName }

        if (currentAccount == null) {
            Log.w(TAG, "Missing account")
            finish()
        }

        val locationSharingLayout = findViewById<View>(R.id.location_sharing_layout)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        lifecycleScope.launch {
            val reportingRequestStore = withContext(Dispatchers.IO) {
                val readSharesResponse = requestReadShares(this@LocationSharingSettingsActivity, currentAccount!!)
                readSharesResponseDetail(readSharesResponse, this@LocationSharingSettingsActivity, currentAccount)
            }
            Log.d(TAG, "readSharesResponse: $reportingRequestStore")
            updateUI(reportingRequestStore, currentAccount!!)
            locationSharingLayout.visibility = View.VISIBLE
            progressBar.visibility = View.INVISIBLE
        }
    }


    private fun updateUI(reportingRequestStore: ReportingRequestStore, account: Account) {
        val switchShare = findViewById<SwitchCompat>(R.id.location_share_switch)
        val locationShareListEmpty = reportingRequestStore.accountLocationSharingMap.isEmpty()
        val locationSharingEnabled = ReportingRequestStoreFile.isLocationSharingEnabled(this, account.name)
        switchShare.isChecked = !locationShareListEmpty && locationSharingEnabled
        switchShare.isEnabled = !locationShareListEmpty
        switchShare.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sendLocationSharingEnable(isChecked, account,  this)
            } else {
                showDialog(switchShare, account)
            }
        }

        findViewById<View>(R.id.location_sharing_desc_tv).visibility = if (locationShareListEmpty) View.GONE else View.VISIBLE

        val locationSharingLinkView = findViewById<CardView>(R.id.location_sharing_link_view)
        locationSharingLinkView.visibility = if (locationShareListEmpty) View.GONE else View.VISIBLE
        locationSharingLinkView.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = "android.intent.action.VIEW"
                putExtra(EXTRA_ACCOUNT_NAME, account.name)
                putExtra(EXTRA_URL, LOCATION_SHARING_URL)
            }
            startActivity(intent)
        }
        val locationSharePrimaryHintTv = findViewById<TextView>(R.id.location_share_primary_hint_tv)
        val locationShareSecondaryHintTv = findViewById<TextView>(R.id.location_share_secondary_hint_tv)
        val locationShareWebLinkTv =findViewById<TextView>(R.id.location_share_web_lint_tv)
        if (locationShareListEmpty) {
            locationSharePrimaryHintTv.text = getString(R.string.location_sharing_disabled)
            locationShareSecondaryHintTv.text = getString(R.string.location_sharing_turn_on_hint)
            locationShareWebLinkTv.visibility = View.GONE
        } else {
            locationSharePrimaryHintTv.text = getString(R.string.location_sharing_turn_off_notify_hint)
            locationShareSecondaryHintTv.text = getString(R.string.location_sharing_off_previous_hint)
            locationShareWebLinkTv.visibility = View.VISIBLE
        }
        val moreAboutStr = getString(R.string.location_sharing_learn_more)
        val spannable = SpannableString(moreAboutStr)
        spannable.setSpan(
                UnderlineSpan(),
                0,
                moreAboutStr.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        locationShareWebLinkTv.text = spannable
        locationShareWebLinkTv.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, LEARN_MORE_LOCATION_SHARING_URL.toUri())
            intent.addFlags(FLAG_ACTIVITY_FORWARD_RESULT)
            startActivity(intent)
        }
    }


    private fun showDialog(switchShare: SwitchCompat, account: Account) {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.location_sharing_turn_off_title)
                .setMessage(R.string.location_sharing_turn_off_hint)
                .setCancelable(false)
                .setPositiveButton(R.string.location_sharing_turn_off_confirm) { dialog, _ ->
                    dialog.dismiss()
                    sendLocationSharingEnable(false, account, this)
                }
                .setNegativeButton(R.string.location_sharing_turn_off_cancel) { dialog, _ ->
                    dialog.dismiss()
                    switchShare.isChecked = true
                }
                .show()
    }


    private fun getCircleBitmapDrawable(bitmap: Bitmap?) =
        if (bitmap != null) RoundedBitmapDrawableFactory.create(resources, bitmap.let {
            bitmap.scale(100, 100)
        }).also { it.isCircular = true } else null

}