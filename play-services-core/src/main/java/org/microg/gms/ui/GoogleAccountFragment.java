/*
 * Copyright (C) 2019 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.preference.Preference;

import com.google.android.gms.R;

import org.microg.gms.auth.AuthConstants;
import org.microg.gms.auth.AuthManager;
import org.microg.tools.ui.ResourceSettingsFragment;
import org.microg.tools.ui.AbstractSettingsActivity;

import static android.accounts.AccountManager.PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE;
import static android.accounts.AccountManager.VISIBILITY_USER_MANAGED_NOT_VISIBLE;
import static android.accounts.AccountManager.VISIBILITY_USER_MANAGED_VISIBLE;
import static org.microg.gms.auth.AuthManager.PREF_AUTH_VISIBLE;

public class GoogleAccountFragment extends ResourceSettingsFragment {

        public GoogleAccountFragment() {
            preferencesResource = R.xml.preferences_google_account;
        }

        @Override
        public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
            super.onCreatePreferencesFix(savedInstanceState, rootKey);
            Preference pref = findPreference(PREF_AUTH_VISIBLE);
            if (pref != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    pref.setVisible(false);
                } else {
                    pref.setOnPreferenceChangeListener((preference, newValue) -> {
                        if (newValue instanceof Boolean) {
                            AccountManager am = AccountManager.get(getContext());
                            for (Account account : am.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)) {
                                am.setAccountVisibility(account, PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE, (Boolean) newValue ? VISIBILITY_USER_MANAGED_VISIBLE : VISIBILITY_USER_MANAGED_NOT_VISIBLE);
                            }
                        }
                        return true;
                    });
                }
            }
        }

        public static class AsActivity extends AbstractSettingsActivity {
        public AsActivity() {
            showHomeAsUp = true;
        }

        @Override
        protected Fragment getFragment() {
            return new GoogleAccountFragment();
        }
    }
}
