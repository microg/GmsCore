/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.stats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.microg.gms.common.Hide;

/**
 * TODO: This should end up in play-services-stats eventually
 */
@Hide
public abstract class GCoreWakefulBroadcastReceiver extends BroadcastReceiver {
    public static boolean completeWakefulIntent(@NonNull Context context, @Nullable Intent intent) {
        return false;
    }
}
