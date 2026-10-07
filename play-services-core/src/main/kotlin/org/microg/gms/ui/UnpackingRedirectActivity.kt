/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.PendingIntentCompat
import androidx.core.content.IntentCompat
import java.util.UUID

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
        if (target != null && target.creatorPackage == packageName) {
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

        @JvmStatic
        fun createUniquePendingIntent(context: Context, intent: Intent): PendingIntent {
            return createUniquePendingIntent(context, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_CANCEL_CURRENT, false)
        }

        @JvmStatic
        fun createUniquePendingIntent(context: Context, intent: Intent, @PendingIntentCompat.Flags flags: Int = PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_CANCEL_CURRENT, isMutable: Boolean = false): PendingIntent {
            if (intent.data != null) throw IllegalArgumentException("Data URI must not be set")
            if (SDK_INT >= 29) {
                intent.identifier = UUID.randomUUID().toString()
            } else {
                intent.data = Uri.fromParts("identifier", UUID.randomUUID().toString(), null)
            }
            return PendingIntentCompat.getActivity(context, 0, intent, flags, isMutable)
                    ?: throw IllegalArgumentException("Unable to create pending intent")
        }

        @JvmStatic
        fun exposeUniqueIntent(context: Context, intent: Intent): Intent {
            return createIntent(context, createUniquePendingIntent(context, intent))
        }

        @JvmStatic
        fun exposeUniqueIntent(context: Context, intent: Intent, @PendingIntentCompat.Flags flags: Int = 0, isMutable: Boolean = false): Intent {
            return createIntent(context, createUniquePendingIntent(context, intent, flags, isMutable))
        }
    }
}