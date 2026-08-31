/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wearable.consent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import org.microg.gms.wearable.WearablePreferences;
import org.microg.gms.wearable.core.R;

/**
 * Shown by Galaxy Watch / Wear OS companion apps via {@code com.google.android.gms.wearable.TOS}.
 * Accepting stores the consent so pairing can continue; declining cancels the companion flow.
 */
public class TermsOfServiceActivity extends Activity {

    public static final String EXTRA_TOS_ACCEPTED = "tosAccepted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (WearablePreferences.isTosAccepted(this)) {
            finishAccepted();
            return;
        }
        setContentView(R.layout.activity_wearable_tos);
        TextView body = findViewById(R.id.wearable_tos_body);
        body.setText(R.string.wearable_tos_body);
        Button accept = findViewById(R.id.wearable_tos_accept);
        Button decline = findViewById(R.id.wearable_tos_decline);
        accept.setOnClickListener(v -> {
            WearablePreferences.setTosAccepted(this, true);
            finishAccepted();
        });
        decline.setOnClickListener(v -> {
            WearablePreferences.setTosAccepted(this, false);
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private void finishAccepted() {
        Intent result = new Intent();
        result.putExtra(EXTRA_TOS_ACCEPTED, true);
        setResult(RESULT_OK, result);
        finish();
    }
}
