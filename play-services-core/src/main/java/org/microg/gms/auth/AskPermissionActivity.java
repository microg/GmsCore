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

package org.microg.gms.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.R;

import org.microg.gms.common.PackageUtils;
import org.microg.gms.people.PeopleManager;

import java.io.IOException;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static android.accounts.AccountManager.KEY_ANDROID_PACKAGE_NAME;
import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.accounts.AccountManager.KEY_CALLER_PID;
import static android.accounts.AccountManager.KEY_CALLER_UID;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class AskPermissionActivity extends AccountAuthenticatorActivity {
    public static final String EXTRA_FROM_ACCOUNT_MANAGER = "from_account_manager";
    public static final String EXTRA_CONSENT_DATA = "consent_data";

    private static final String TAG = "GmsAuthAskPermission";
    private AuthManager authManager;
    private IntentData data;

    private static class IntentData {
        private String accountName;
        private String accountType;
        private Account account;

        private String packageName;
        private String service;

        private int callerUid;
        private int callerPid;

        private ConsentData consentData;
        private boolean fromAccountManager = false;

        private CharSequence appLabel;
        private Drawable appIcon;

        private IntentData(Intent intent) {
            if (intent != null) {
                accountName = intent.getStringExtra(KEY_ACCOUNT_NAME);
                accountType = intent.getStringExtra(KEY_ACCOUNT_TYPE);
                packageName = intent.getStringExtra(KEY_ANDROID_PACKAGE_NAME);
                service = intent.getStringExtra(KEY_AUTHTOKEN);
                callerUid = intent.getIntExtra(KEY_CALLER_UID, 0);
                callerPid = intent.getIntExtra(KEY_CALLER_PID, 0);
                fromAccountManager = intent.hasExtra(EXTRA_FROM_ACCOUNT_MANAGER);
                if (intent.hasExtra(EXTRA_CONSENT_DATA)) {
                    try {
                        consentData = ConsentData.ADAPTER.decode(intent.getByteArrayExtra(EXTRA_CONSENT_DATA));
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
            if (accountName != null && accountType != null) {
                account = new Account(accountName, accountType);
            }
        }

        private void verify(Context context) throws Exception {
            if (accountName == null || accountType == null || account == null) throw new IllegalArgumentException("Required account information missing");
            if (packageName == null || service == null) throw new IllegalArgumentException("Required request information missing");
            if (callerUid == 0) throw new IllegalArgumentException("Required caller information missing");
            PackageUtils.getAndCheckPackage(context, packageName, callerUid, callerPid);

            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            appLabel = packageManager.getApplicationLabel(applicationInfo);
            appIcon = packageManager.getApplicationIcon(applicationInfo);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ask_permission);
        data = new IntentData(getIntent());
        try {
            data.verify(this);
        } catch (Exception e) {
            Log.w(TAG, "Verification failed", e);
            finish();
            return;
        }

        // This makes the dialog take up the full width
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(data.packageName.hashCode());

        authManager = new AuthManager(this, data.accountName, data.packageName, data.service);

        Bitmap profileIcon = PeopleManager.getOwnerAvatarBitmap(this, data.accountName, false);

        // receive profile icon
        if (profileIcon != null) {
            ((ImageView) findViewById(R.id.account_photo)).setImageBitmap(profileIcon);
        } else {
            new Thread(() -> {
                final Bitmap profileIcon1 = PeopleManager.getOwnerAvatarBitmap(AskPermissionActivity.this, data.accountName, true);
                runOnUiThread(() -> ((ImageView) findViewById(R.id.account_photo)).setImageBitmap(profileIcon1));
            }).start();
        }

        ((ImageView) findViewById(R.id.app_icon)).setImageDrawable(data.appIcon);
        if (isOAuth()) {
            ((TextView) findViewById(R.id.title)).setText(getString(R.string.ask_scope_permission_title, data.appLabel));
        } else {
            ((TextView) findViewById(R.id.title)).setText(getString(R.string.ask_service_permission_title, data.appLabel));
        }
        findViewById(android.R.id.button1).setOnClickListener(v -> onAllow());
        findViewById(android.R.id.button2).setOnClickListener(v -> onDeny());
        ((ListView) findViewById(R.id.permissions)).setAdapter(new PermissionAdapter());
    }

    public void onAllow() {
        authManager.setPermitted(true);
        findViewById(android.R.id.button1).setEnabled(false);
        findViewById(android.R.id.button2).setEnabled(false);
        findViewById(R.id.progress_bar).setVisibility(VISIBLE);
        findViewById(R.id.no_progress_bar).setVisibility(GONE);
        new Thread(() -> {
            try {
                AuthResponse response = authManager.requestAuth(data.fromAccountManager);
                Bundle result = new Bundle();
                result.putString(KEY_AUTHTOKEN, response.auth);
                result.putString(KEY_ACCOUNT_NAME, data.accountName);
                result.putString(KEY_ACCOUNT_TYPE, data.accountType);
                result.putString(KEY_ANDROID_PACKAGE_NAME, data.packageName);
                result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
                setAccountAuthenticatorResult(result);
            } catch (IOException e) {
                Log.w(TAG, e);
            }
            finish();
        }).start();
    }

    public void onDeny() {
        authManager.setPermitted(false);
        finish();
    }

    @Override
    public void finish() {
        if (data.packageName != null) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(data.packageName.hashCode());
        }
        super.finish();
    }

    private boolean isOAuth() {
        return data.service.startsWith("oauth2:") || data.service.startsWith("oauth:");
    }

    private String getScopeLabel(String scope) {
        if (data.consentData != null) {
            for (ConsentData.ScopeDetails scopeDetails : data.consentData.scopes) {
                if (scope.equals(scopeDetails.id)) {
                    return scopeDetails.title;
                }
            }
        }
        String labelResourceId = "permission_scope_";
        String escapedScope = scope.replace("/", "_").replace("-", "_");
        if (scope.startsWith("https://")) {
            labelResourceId += escapedScope.substring(8);
        } else {
            labelResourceId += escapedScope;
        }
        int labelResource = getResources().getIdentifier(labelResourceId, "string", getPackageName());
        if (labelResource != 0) {
            return getString(labelResource);
        }
        return "unknown";
    }

    private String getScopeDescription(String scope) {
        if (data.consentData != null) {
            for (ConsentData.ScopeDetails scopeDetails : data.consentData.scopes) {
                if (scope.equals(scopeDetails.id)) {
                    return scopeDetails.description;
                }
            }
        }
        return null;
    }

    private String getServiceLabel(String service) {
        int labelResource = getResources().getIdentifier("permission_service_" + service + "_label", "string", getPackageName());
        if (labelResource != 0) {
            return getString(labelResource);
        }
        return "unknown";
    }

    private class PermissionAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (isOAuth()) {
                return data.service.split(" ").length;
            }
            return 1;
        }

        @Override
        public String getItem(int position) {
            if (isOAuth()) {
                String tokens = data.service.split(":", 2)[1];
                return tokens.split(" ")[position];
            }
            return data.service;
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String item = getItem(position);
            String label;
            String description;
            if (isOAuth()) {
                label = getScopeLabel(item);
                description = getScopeDescription(item);
            } else {
                label = getServiceLabel(item);
                description = null;
            }
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(AskPermissionActivity.this)
                        .inflate(R.layout.ask_permission_list_entry, parent, false);
            }
            ((TextView) view.findViewById(android.R.id.text1)).setText(label);
            TextView textView = (TextView) view.findViewById(android.R.id.text2);
            if (description != null && !description.isEmpty()) {
                textView.setText(Html.fromHtml(description.trim().replace("\n", "<br>")));
                textView.setVisibility(VISIBLE);
            } else {
                textView.setVisibility(GONE);
            }
            return view;
        }
    }
}
