/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.googlehelp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.googlehelp.GoogleHelp

private const val TAG = "GoogleHelpRedirect"

class GoogleHelpRedirectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callingPackage = callingActivity?.packageName ?: return finish()
        val intent = intent ?: return finish()
        val googleHelp = intent.getParcelableExtra<GoogleHelp>(EXTRA_GOOGLE_HELP) ?: return finish()
        Log.d(TAG, "Using GoogleHelp: $googleHelp")
        val uri = googleHelp.uri ?: return finish()
        // TODO: Not all Google apps send proper URI values, as these are in fact not used by Google's original implementation.
        //       As a work-around we should get the proper URL by retrieving top_level_topic_url:$callingPackage
        //       from https://www.google.com/tools/feedback/mobile/get-configurations endpoint.
        //       Long-term best is to not redirect to web but instead implement the thing properly, allowing us also to show
        //       option items, do proper theming for better integration, etc.
        Log.d(TAG, "Open $uri for $callingPackage/${googleHelp.appContext} in Browser")
        // noinspection UnsafeImplicitIntentLaunch
        startActivity(Intent(Intent.ACTION_VIEW, uri))
        finish()
    }

    companion object {
        const val EXTRA_GOOGLE_HELP = "EXTRA_GOOGLE_HELP"
    }
}