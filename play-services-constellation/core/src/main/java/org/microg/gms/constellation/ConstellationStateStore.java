/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Small persistent store for Constellation verification state.
 *
 * <p>Remembers which phone numbers have been verified (number, time, method) so
 * {@code getVerifiedPhoneNumbers}-style reads can answer from local state, and keeps a
 * short history of verification attempts (policy id, requested methods, outcome) for
 * debugging via dumpsys. Phone numbers are stored because the caller needs them back;
 * nothing else identifying (no IMSI, no tokens) is kept here.
 */
public class ConstellationStateStore {
    private static final String PREFS_NAME = "constellation_state";
    private static final String KEY_VERIFIED_PREFIX = "verified_";
    private static final String KEY_ATTEMPTS = "attempts";
    private static final int MAX_ATTEMPTS = 20;

    private final SharedPreferences prefs;

    public ConstellationStateStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Records a verified phone number with the time and method it was verified with. */
    public void putVerified(String phoneNumber, long timestampMillis, int verificationMethod) {
        prefs.edit()
                .putString(KEY_VERIFIED_PREFIX + phoneNumber, timestampMillis + ":" + verificationMethod)
                .apply();
    }

    /** Returns all verified numbers as "number|timestampMillis|method" entries. */
    public List<String> getVerified() {
        List<String> result = new ArrayList<>();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith(KEY_VERIFIED_PREFIX)) {
                String number = key.substring(KEY_VERIFIED_PREFIX.length());
                result.add(number + "|" + prefs.getString(key, ""));
            }
        }
        return result;
    }

    /** Appends one line to the bounded attempt history (newest last). */
    public void recordAttempt(String line) {
        String history = prefs.getString(KEY_ATTEMPTS, "");
        String[] lines = history.isEmpty() ? new String[0] : history.split("\n");
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, lines.length - (MAX_ATTEMPTS - 1));
        for (int i = start; i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        sb.append(System.currentTimeMillis()).append(" ").append(line);
        prefs.edit().putString(KEY_ATTEMPTS, sb.toString()).apply();
    }

    /** Returns the attempt history, oldest first, one line per attempt. */
    public List<String> getAttempts() {
        String history = prefs.getString(KEY_ATTEMPTS, "");
        List<String> result = new ArrayList<>();
        if (!history.isEmpty()) {
            for (String line : history.split("\n")) {
                result.add(line);
            }
        }
        return result;
    }

    /** Drops all stored state. */
    public void clear() {
        prefs.edit().clear().apply();
    }
}
