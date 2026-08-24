/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.huawei.signature.diff;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This is to make sure the process is initialized at boot.
 */
public class InitReceiver extends BroadcastReceiver {
    private static final String TAG = "InitReceiver";

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        try {
            context.startService(new Intent(context, SignatureService.class));
        } catch (Exception ignored) {
        }
    }
}
