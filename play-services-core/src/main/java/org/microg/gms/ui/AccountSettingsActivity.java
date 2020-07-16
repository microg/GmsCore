/*
 * Copyright (C) 2013-2017 microG Project Team
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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.google.android.gms.R;

import org.microg.gms.auth.AuthConstants;
import org.microg.gms.auth.AuthManager;
import org.microg.tools.ui.AbstractSettingsActivity;
import org.microg.tools.ui.ResourceSettingsFragment;

import static android.accounts.AccountManager.PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE;
import static android.accounts.AccountManager.VISIBILITY_USER_MANAGED_NOT_VISIBLE;
import static android.accounts.AccountManager.VISIBILITY_USER_MANAGED_VISIBLE;
import static org.microg.gms.auth.AuthManager.PREF_AUTH_VISIBLE;

public class AccountSettingsActivity extends AbstractSettingsActivity {

    @Override
    protected Fragment getFragment() {
        return new AccountSettingsFragment();
    }

    public static class AccountSettingsFragment extends ResourceSettingsFragment {
        public AccountSettingsFragment() {
            preferencesResource =  R.xml.preferences_account;
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
            super.onCreatePreferences(savedInstanceState, rootKey);
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
    }
}
