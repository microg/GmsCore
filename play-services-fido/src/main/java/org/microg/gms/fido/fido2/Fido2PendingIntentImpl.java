/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.fido2;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.IntentSender;

import com.google.android.gms.fido.fido2.Fido2PendingIntent;

public class Fido2PendingIntentImpl implements Fido2PendingIntent {
    private PendingIntent pendingIntent;

    public Fido2PendingIntentImpl(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    @Override
    public boolean hasPendingIntent() {
        return pendingIntent != null;
    }

    @Override
    public void launchPendingIntent(Activity activity, int requestCode) throws IntentSender.SendIntentException {
        if (!hasPendingIntent()) throw new IllegalStateException("No PendingIntent available");
        activity.startIntentSenderForResult(pendingIntent.getIntentSender(), requestCode, null, 0, 0, 0);
    }
}
