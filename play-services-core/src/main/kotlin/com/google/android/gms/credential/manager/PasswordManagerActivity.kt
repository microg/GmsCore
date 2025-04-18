/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.credential.manager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

const val PASSWORD_MANAGER_CLASS_NAME = "com.google.android.gms.credential.manager.PasswordManagerActivity"

const val EXTRA_KEY_ACCOUNT_NAME = "pwm.DataFieldNames.accountName"

private const val TAG = "PasswordManagerActivity"

private const val PSW_MANAGER_PATH = "https://passwords.google.com/"

private const val CHROME_PACKAGE_NAME = "com.android.chrome"

class PasswordManagerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: start")
        val targetIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PSW_MANAGER_PATH))
        val resolveInfoList = packageManager.queryIntentActivities(targetIntent, 0)
        Log.d(TAG, "resolveInfoList: $resolveInfoList")
        val hasChrome = resolveInfoList.any { it.activityInfo.packageName == CHROME_PACKAGE_NAME }
        if (hasChrome) {
            targetIntent.setPackage(CHROME_PACKAGE_NAME)
            if (targetIntent.resolveActivity(packageManager) != null) {
                startActivity(targetIntent)
                finish()
                return
            }
        }
        if (resolveInfoList.isNotEmpty()) {
            targetIntent.setPackage(null)
            startActivity(targetIntent)
        }
        finish()
    }

}