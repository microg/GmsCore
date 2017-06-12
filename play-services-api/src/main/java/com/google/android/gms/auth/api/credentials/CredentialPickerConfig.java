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

package com.google.android.gms.auth.api.credentials;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class CredentialPickerConfig extends AutoSafeParcelable {

    @SafeParceled(1000)
    private int versionCode = 1;

    @SafeParceled(1)
    private boolean showAddAccountButton;

    @SafeParceled(2)
    private boolean showCancelButton;

    @SafeParceled(3)
    private boolean forNewAccount;

    private CredentialPickerConfig() {
    }

    public CredentialPickerConfig(boolean showAddAccountButton, boolean showCancelButton, boolean forNewAccount) {
        this.showAddAccountButton = showAddAccountButton;
        this.showCancelButton = showCancelButton;
        this.forNewAccount = forNewAccount;
    }

    public boolean isForNewAccount() {
        return forNewAccount;
    }

    public boolean shouldShowAddAccountButton() {
        return showAddAccountButton;
    }

    public boolean shouldShowCancelButton() {
        return showCancelButton;
    }

    public class Builder {
        private boolean showAddAccountButton;
        private boolean showCancelButton;
        private boolean forNewAccount;

        public CredentialPickerConfig build() {
            return new CredentialPickerConfig(showAddAccountButton, showCancelButton, forNewAccount);
        }

        /**
         * Sets whether the hint request is for a new account sign-up flow.
         */
        public Builder setForNewAccount(boolean forNewAccount) {
            this.forNewAccount = forNewAccount;
            return this;
        }

        /**
         * Sets whether the add account button should be shown in credential picker dialog.
         */
        public Builder setShowAddAccountButton(boolean showAddAccountButton) {
            this.showAddAccountButton = showAddAccountButton;
            return this;
        }

        /**
         * Sets whether the cancel button should be shown in credential picker dialog.
         */
        public Builder setShowCancelButton(boolean showCancelButton) {
            this.showCancelButton = showCancelButton;
            return this;
        }
    }

    public static final Creator<CredentialPickerConfig> CREATOR = new AutoCreator<CredentialPickerConfig>(CredentialPickerConfig.class);
}
