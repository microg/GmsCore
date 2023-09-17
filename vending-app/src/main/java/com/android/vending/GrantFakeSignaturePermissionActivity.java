/*
 * SPDX-FileCopyrightText: 2016 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

@TargetApi(Build.VERSION_CODES.M)
public class GrantFakeSignaturePermissionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkSelfPermission("android.permission.FAKE_PACKAGE_SIGNATURE") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.FAKE_PACKAGE_SIGNATURE"}, 1);
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1 && grantResults.length == 1) {
            setResult(grantResults[0] == PackageManager.PERMISSION_GRANTED ? RESULT_OK : RESULT_CANCELED);
            finish();
        }
    }
}
