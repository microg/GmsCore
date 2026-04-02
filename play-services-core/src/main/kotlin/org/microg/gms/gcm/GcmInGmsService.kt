/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.gcm

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.util.Log
import androidx.core.app.PendingIntentCompat
import androidx.legacy.content.WakefulBroadcastReceiver
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthManager
import org.microg.gms.auth.AuthPrefs
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import org.microg.gms.common.ForegroundServiceContext
import org.microg.gms.gcm.registeration.ChimeGmsRegistrationHelper
import org.microg.gms.profile.ProfileManager

private const val TAG = "GcmInGmsService"

private const val KEY_GCM_REG_SENDER = "sender"
private const val KEY_GCM_REG_TIME = "reg_time"
private const val KEY_GCM_REG_ACCOUNT_LIST = "accountList"

private const val GMS_GCM_REGISTER_SCOPE = "GCM"
private const val GMS_GCM_REGISTER_SENDER = "745476177629"
private const val GMS_GCM_REGISTER_SUBTYPE = "745476177629"
private const val GMS_GCM_REGISTER_SUBSCRIPTION = "745476177629"
private const val GCM_GROUP_SENDER = "google.com"
private const val GCM_GMS_REG_REFRESH_S = 604800L
private const val GMS_GCM_OAUTH_SERVICE = "oauth2:https://www.googleapis.com/auth/gcm"

class GcmInGmsService : LifecycleService() {
    private var sp: SharedPreferences? = null
    private var accountManager: AccountManager? = null
    private val chimeGmsRegistrationHelper by lazy { ChimeGmsRegistrationHelper(this) }

    override fun onCreate() {
        super.onCreate()
        ProfileManager.ensureInitialized(this)
        sp = getSharedPreferences("com.google.android.gcm", MODE_PRIVATE) ?: throw RuntimeException("sp get error")
        accountManager = getSystemService(ACCOUNT_SERVICE) as AccountManager? ?: throw RuntimeException("accountManager is null")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            ForegroundServiceContext.completeForegroundService(this, intent, TAG)
            Log.d(TAG, "onStartCommand: $intent")
            lifecycleScope.launchWhenStarted {
                if (checkGcmStatus()) {
                    handleIntent(intent)
                } else {
                    val intent = Intent(ACTION_GCM_RECONNECT).apply {
                        setPackage(Constants.GMS_PACKAGE_NAME)
                    }
                    sendBroadcast(intent)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun checkGcmStatus(): Boolean {
        if (McsService.isConnected(this)) {
            Log.d(TAG, "checkGcmStatus: gcm isConnected")
            return true
        }
        Log.d(TAG, "checkGcmStatus: gcm need reconnect")
        return false
    }

    private suspend fun handleIntent(intent: Intent) {
        val action = intent.action
        if (checkGmsGcmStatus()) {
            Log.d(TAG, "handleIntent: checkGmsGcmStatus -> reset")
            runCatching { registerGcmInGms(this, intent) }.onFailure {
                Log.w(TAG, "handleIntent: registerGcmInGms error", it)
            }
            return
        }
        Log.d(TAG, "handleIntent: action: $action")
        when (action) {
            GcmConstants.ACTION_C2DM_RECEIVE -> {
                Log.d(TAG, "start handle gcm message")
                val callerIntent = Intent(ACTION_GCM_MESSAGE_RECEIVE)
                callerIntent.setPackage(Constants.GMS_PACKAGE_NAME)
                callerIntent.putExtras(intent)
                sendOrderedBroadcast(callerIntent, null)
            }
            ACTION_GCM_REGISTER_ALL_ACCOUNTS,
            ACTION_GCM_CONNECTED -> {
                updateLocalAccountGroups()
            }
            ACTION_GCM_REGISTER_ACCOUNT -> {
                val accountName = intent.getStringExtra(KEY_GCM_REGISTER_ACCOUNT_NAME) ?: return
                Log.d(TAG, "GCM groups update account name: $accountName")
                val account = accountManager?.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)?.find { it.name == accountName } ?: return
                updateLocalAccountGroups(account)
            }
        }
    }

    private suspend fun updateGroupsWithAccount(account: Account, regId: String) {
        Log.d(TAG, "updateGroupsWithAccount: account: ${account.name}")
        val authManager = AuthManager(this, account.name, Constants.GMS_PACKAGE_NAME, GMS_GCM_OAUTH_SERVICE).apply {
            setItCaveatTypes("2")
        }
        val authsToken = runCatching { withContext(Dispatchers.IO) { authManager.requestAuth(true) }.parseAuthsToken() }.getOrNull() ?: return
        val extras = Bundle().apply {
            putString(GcmConstants.EXTRA_ACCOUNT_NAME, account.name)
            putString(GcmConstants.EXTRA_REG_ID, regId)
            putString(GcmConstants.EXTRA_AUTHS_TOKEN, authsToken)
            putString(GcmConstants.EXTRA_SEND_TO, GCM_GROUP_SENDER)
            putString(GcmConstants.EXTRA_SEND_FROM, GCM_GROUP_SENDER)
            putString(GcmConstants.EXTRA_MESSAGE_ID, "${System.currentTimeMillis() / 1000}-0")
        }
        Log.d(TAG, "updateGroupsWithAccount extras: $extras")
        val intent = Intent(GcmConstants.ACTION_GCM_SEND).apply {
            setPackage(Constants.GMS_PACKAGE_NAME)
            putExtras(extras)
            putExtra(GcmConstants.EXTRA_APP, Intent().apply { setPackage(Constants.GMS_PACKAGE_NAME) }.let { PendingIntentCompat.getBroadcast(this@GcmInGmsService, 0, it, 0, false) })
        }.also {
            it.putExtra(GcmConstants.EXTRA_MESSENGER, Messenger(object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (Binder.getCallingUid() == Process.myUid()) {
                        val history = sp?.getString(KEY_GCM_REG_ACCOUNT_LIST, "")
                        if (history?.contains(account.name) == true) {
                            Log.d(TAG, "updateGroupsWithAccount handleMessage history<$history> contains: ${account.name}")
                            return
                        }
                        val saveStr = if (history.isNullOrEmpty()) account.name else "${account.name}/$history"
                        sp?.edit()?.putString(KEY_GCM_REG_ACCOUNT_LIST, saveStr)?.apply()
                        Log.d(TAG, "updateGroupsWithAccount handleMessage save: $saveStr")
                    }
                }
            }))
        }
        sendOrderedBroadcast(intent, null)
    }

    private suspend fun updateLocalAccountGroups(account: Account? = null) {
        Log.d(TAG, "GMS $GMS_GCM_REGISTER_SENDER already registered, start updateLocalAccount")
        val regId = sp?.getString(KEY_GCM_REG_ID, null) ?: return
        val accounts = chimeGmsRegistrationHelper.handleRegistration(regId)
        if (accounts.isNotEmpty()) {
            Log.d(TAG, "updateLocalAccountGroups: handleRegistration done")
            accounts.forEach { updateGroupsWithAccount(it, regId) }
            return
        }
        val localGoogleAccounts = accountManager?.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE) ?: return
        val accountList = sp?.getString(KEY_GCM_REG_ACCOUNT_LIST, null)
        if (account != null && accountList?.contains(account.name) != true) {
            Log.d(TAG, "updateLocalAccountGroups: single account: ${account.name}")
            updateGroupsWithAccount(account, regId)
            return
        }
        Log.d(TAG, "updateLocalAccountGroups: registrationAccountList: $accountList")
        val needRegisterAccounts = if (accountList == null) localGoogleAccounts.toList() else localGoogleAccounts.filter { !accountList.contains(it.name) }
        Log.d(TAG, "updateLocalAccountGroups: needRegisterAccounts: ${needRegisterAccounts.joinToString("|") { it.name }}")
        if (needRegisterAccounts.isEmpty()) return
        needRegisterAccounts.forEach { updateGroupsWithAccount(it, regId) }
    }

    private suspend fun registerGcmInGms(context: Context, intent: Intent) {
        Log.i(TAG, "Registering GMS $GMS_GCM_REGISTER_SENDER")
        val regId = withContext(Dispatchers.IO) {
            val request = RegisterRequest().build(context)
                .checkin(LastCheckinInfo.read(context))
                .app(Constants.GMS_PACKAGE_NAME, Constants.GMS_PACKAGE_SIGNATURE_SHA1, BuildConfig.VERSION_CODE)
                .sender(GMS_GCM_REGISTER_SENDER)
                .extraParam("subscription", GMS_GCM_REGISTER_SUBSCRIPTION)
                .extraParam("X-subscription", GMS_GCM_REGISTER_SUBSCRIPTION)
                .extraParam("subtype", GMS_GCM_REGISTER_SUBTYPE)
                .extraParam("X-subtype", GMS_GCM_REGISTER_SUBTYPE)
                .extraParam("scope", GMS_GCM_REGISTER_SCOPE)
            val gcmDatabase = GcmDatabase(context)
            ensureAppRegistrationAllowed(context, gcmDatabase, request.app)
            completeRegisterRequest(context, gcmDatabase, request).getString(GcmConstants.EXTRA_REGISTRATION_ID)
        }
        Log.d(TAG, "GCM IN GMS regId: $regId")
        if (regId == null) {
            Log.w(TAG, "registerGcmInGms reg id is null")
            return
        }
        val sharedPreferencesEditor = sp?.edit()
        sharedPreferencesEditor?.putLong(KEY_GCM_ANDROID_ID, LastCheckinInfo.read(context).androidId)
        sharedPreferencesEditor?.putString(KEY_GCM_REG_ID, regId)
        sharedPreferencesEditor?.putString(KEY_GCM_REG_SENDER, GMS_GCM_REGISTER_SENDER)
        sharedPreferencesEditor?.putLong(KEY_GCM_REG_TIME, System.currentTimeMillis())
        sharedPreferencesEditor?.remove(KEY_GCM_REG_ACCOUNT_LIST)
        chimeGmsRegistrationHelper.resetAllData()
        if (sharedPreferencesEditor?.commit() == false) {
            Log.d(TAG, "Failed to write GMS registration")
        } else {
            Log.d(TAG, "registerGcmInGms: sendBroadcast: ${intent.action}")
            Intent(intent.action).apply {
                setPackage(Constants.GMS_PACKAGE_NAME)
                putExtras(intent)
            }.let { sendBroadcast(it) }
        }
    }

    private fun checkGmsGcmStatus(): Boolean {
        val targetId = LastCheckinInfo.read(this).androidId
        val regSender = sp?.getString(KEY_GCM_REG_SENDER, null)
        val regId = sp?.getString(KEY_GCM_REG_ID, null)
        val androidId = sp?.getLong(KEY_GCM_ANDROID_ID, 0)
        val regTime = sp?.getLong(KEY_GCM_REG_TIME, 0) ?: 0L
        return targetId != androidId || regSender == null || regId == null || regTime + GCM_GMS_REG_REFRESH_S * 1000 < System.currentTimeMillis()
    }
}

class GcmRegistrationReceiver : WakefulBroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val shouldReceiveTwoStepVerification = AuthPrefs.shouldReceiveTwoStepVerification(context)
        val allowedFindDevicesRequest = AuthPrefs.allowedFindDevices(context)
        if (!shouldReceiveTwoStepVerification && !allowedFindDevicesRequest) {
            Log.d(TAG, "GcmRegistrationReceiver onReceive: <Two-Step> Switch not allowed & <Find-Device> Switch not allowed")
            return
        }
        Log.d(TAG, "GcmRegistrationReceiver onReceive: action: ${intent.action}")
        val callIntent = Intent(context, GcmInGmsService::class.java)
        callIntent.action = intent.action
        if (ACTION_GCM_REGISTER_ACCOUNT == intent.action || GcmConstants.ACTION_C2DM_RECEIVE == intent.action) {
            callIntent.putExtras(intent.extras!!)
        }
        ForegroundServiceContext(context).startService(callIntent)
    }
}
