/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MarketIntentRedirect extends Activity {
    private static final String TAG = "IntentForwarder";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            try {
                processIntent(intent);
            } catch (Exception e) {
                Log.w(TAG, "Failed forwarding", e);
            }
        } else {
            Log.w(TAG, "Intent is null, ignoring");
        }
        finish();
    }

    private boolean isNonSelfIntent(@NonNull Intent intent) {
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, 0);
        return resolveInfo != null && resolveInfo.activityInfo != null && !getPackageName().equals(resolveInfo.activityInfo.packageName);
    }

    private void processIntent(@NonNull Intent intent) {
        Log.d(TAG, "Received " + intent);
        Intent newIntent = new Intent(intent);
        newIntent.setPackage(null);
        newIntent.setComponent(null);
        if ("market".equals(newIntent.getScheme())) {
            try {
                if (isNonSelfIntent(newIntent)) {
                    Log.d(TAG, "Redirect to " + newIntent);
                    startActivity(newIntent);
                    return;
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            // Rewrite to market.android.com as there is no handler for market://
            // This allows to always still open in a web browser
            newIntent.setData(newIntent.getData().buildUpon()
                    .scheme("https").authority("market.android.com")
                    .encodedPath(newIntent.getData().getAuthority() + newIntent.getData().getEncodedPath())
                    .build());
            Log.d(TAG, "Rewrote as " + newIntent + " (" + newIntent.getDataString() + ")");
        }
        if ("market.android.com".equals(newIntent.getData().getAuthority()) && newIntent.getData().getPath().startsWith("/details")) {
            // Rewrite to play.google.com for better compatibility
            newIntent.setData(newIntent.getData().buildUpon()
                    .scheme("https").authority("play.google.com")
                    .encodedPath("/store/apps" + newIntent.getData().getEncodedPath())
                    .build());
            Log.d(TAG, "Rewrote as " + newIntent + " (" + newIntent.getDataString() + ")");
        }
        try {
            if (isNonSelfIntent(newIntent)) {
                Log.d(TAG, "Redirect to " + newIntent);
                startActivity(newIntent);
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
            Toast.makeText(this, "Unable to open", Toast.LENGTH_SHORT).show();
        }
        Log.w(TAG, "Not forwarded " + intent);
    }
}
