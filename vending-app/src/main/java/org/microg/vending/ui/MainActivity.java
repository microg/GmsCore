/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.android.vending.R;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final String GMS_PACKAGE_NAME = "com.google.android.gms";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setPackage(GMS_PACKAGE_NAME);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "MAIN activity is not DEFAULT. Trying to resolve instead.");
                intent.setClassName(GMS_PACKAGE_NAME, getPackageManager().resolveActivity(intent, 0).activityInfo.name);
                startActivity(intent);
            }
            Toast.makeText(this, R.string.toast_installed, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.w(TAG, "Failed launching microG Settings", e);
            Toast.makeText(this, R.string.toast_not_installed, Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
