/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.signin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import org.microg.gms.common.Hide;

@Hide
@SuppressLint("StaticFieldLeak")
public class Storage {
    private static Object LOCK = new Object();
    private static Storage INSTANCE;

    public static Storage getInstance(Context context) {
        synchronized (LOCK) {
            if (INSTANCE == null) {
                INSTANCE = new Storage(context.getApplicationContext());
            }
        }
        return INSTANCE;
    }

    private static final String PREF_DEFAULT_ACCOUNT = "defaultGoogleSignInAccount";
    private static final String PREF_PREFIX_ACCOUNT = "googleSignInAccount:";
    private static final String PREF_PREFIX_OPTIONS = "googleSignInOptions:";
    private final SharedPreferences sharedPreferences;

    public Storage(Context context) {
        this.sharedPreferences = context.getSharedPreferences("com.google.android.gms.signin", Context.MODE_PRIVATE);
    }

    @Nullable
    public GoogleSignInAccount getSavedDefaultGoogleSignInAccount() {
        synchronized (sharedPreferences) {
            String defaultGoogleSignInAccountName = sharedPreferences.getString(PREF_DEFAULT_ACCOUNT, null);
            if (defaultGoogleSignInAccountName == null) return null;
            String googleSignInAccountJson = sharedPreferences.getString(PREF_PREFIX_ACCOUNT + defaultGoogleSignInAccountName, null);
            if (googleSignInAccountJson == null) return null;
            try {
                return GoogleSignInAccount.fromJson(googleSignInAccountJson);
            } catch (Exception e) {
                return null;
            }
        }
    }

    @Nullable
    public GoogleSignInOptions getSavedDefaultGoogleSignInOptions() {
        synchronized (sharedPreferences) {
            String defaultGoogleSignInAccountName = sharedPreferences.getString(PREF_DEFAULT_ACCOUNT, null);
            if (defaultGoogleSignInAccountName == null) return null;
            String googleSignInOptionsJson = sharedPreferences.getString(PREF_PREFIX_OPTIONS + defaultGoogleSignInAccountName, null);
            if (googleSignInOptionsJson == null) return null;
            try {
                return GoogleSignInOptions.fromJson(googleSignInOptionsJson);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public void saveDefaultGoogleSignInAccount(@NonNull GoogleSignInAccount googleSignInAccount, @NonNull GoogleSignInOptions googleSignInOptions) {
        synchronized (sharedPreferences) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(PREF_DEFAULT_ACCOUNT, googleSignInAccount.getObfuscatedIdentifier());
            editor.putString(PREF_PREFIX_ACCOUNT + googleSignInAccount.getObfuscatedIdentifier(), googleSignInAccount.toJson());
            editor.putString(PREF_PREFIX_OPTIONS + googleSignInAccount.getObfuscatedIdentifier(), googleSignInOptions.toJson());
            editor.apply();
        }
    }
}
