/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat

/**
 * Activity that unpacks and redirects to a pending intent
 *
 * We use this to be able to return pending intents (that can invoke unexported activities with
 * predefined action and extras) in places where the API requires an intent.
 *
 * Google took a similar approach in GMS
 */
class UnpackingRedirectActivity : AppCompatActivity() {
    private val redirectIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            setResult(
                it.resultCode,
                it.data
            )
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target =
            IntentCompat.getParcelableExtra(intent, EXTRA_TARGET, PendingIntent::class.java)
        if (target != null) {
            try {
                // Fill in any extras provided (other than target itself)
                // This will be ignored for pending intents with FLAG_IMMUTABLE
                val fillInIntent = Intent()
                fillInIntent.putExtras(intent)
                fillInIntent.removeExtra(EXTRA_TARGET)

                redirectIntentLauncher.launch(
                    IntentSenderRequest.Builder(target).setFillInIntent(fillInIntent).build()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Unable to start unpacked pending intent", e)
                setResult(RESULT_CANCELED)
                finish()
            }
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    companion object {
        private const val EXTRA_TARGET = "target"

        @JvmStatic
        fun createIntent(context: Context, target: PendingIntent): Intent {
            val intent = Intent(context, UnpackingRedirectActivity::class.java)
            intent.putExtra(EXTRA_TARGET, target)
            return intent
        }
    }
}