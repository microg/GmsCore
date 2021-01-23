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
    private Account account;
    private String packageName;
    private String service;
    private AuthManager authManager;
    private ConsentData consentData;
    private boolean fromAccountManager = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ask_permission);

        // This makes the dialog take up the full width
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);

        account = new Account(getIntent().getStringExtra(KEY_ACCOUNT_NAME),
                getIntent().getStringExtra(KEY_ACCOUNT_TYPE));
        packageName = getIntent().getStringExtra(KEY_ANDROID_PACKAGE_NAME);
        service = getIntent().getStringExtra(KEY_AUTHTOKEN);
        if (getIntent().hasExtra(EXTRA_CONSENT_DATA)) {
            try {
                consentData = ConsentData.ADAPTER.decode(getIntent().getByteArrayExtra(EXTRA_CONSENT_DATA));
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        } else {
            Log.d(TAG, "No Consent details attached");
        }

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(packageName.hashCode());

        if (getIntent().hasExtra(EXTRA_FROM_ACCOUNT_MANAGER)) fromAccountManager = true;
        int callerUid = getIntent().getIntExtra(KEY_CALLER_UID, 0);
        packageName = PackageUtils.getAndCheckPackage(this, packageName, getIntent().getIntExtra(KEY_CALLER_UID, 0), getIntent().getIntExtra(KEY_CALLER_PID, 0));
        authManager = new AuthManager(this, account.name, packageName, service);

        // receive package info
        PackageManager packageManager = getPackageManager();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Failed to find package " + packageName, e);
            finish();
            return;
        }
        CharSequence appLabel = packageManager.getApplicationLabel(applicationInfo);
        Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
        Bitmap profileIcon = PeopleManager.getOwnerAvatarBitmap(this, account.name, false);

        // receive profile icon
        if (profileIcon != null) {
            ((ImageView) findViewById(R.id.account_photo)).setImageBitmap(profileIcon);
        } else {
            new Thread(() -> {
                final Bitmap profileIcon1 = PeopleManager.getOwnerAvatarBitmap(AskPermissionActivity.this, account.name, true);
                runOnUiThread(() -> ((ImageView) findViewById(R.id.account_photo)).setImageBitmap(profileIcon1));
            }).start();
        }

        ((ImageView) findViewById(R.id.app_icon)).setImageDrawable(appIcon);
        if (isOAuth()) {
            ((TextView) findViewById(R.id.title)).setText(getString(R.string.ask_scope_permission_title, appLabel));
        } else {
            ((TextView) findViewById(R.id.title)).setText(getString(R.string.ask_service_permission_title, appLabel));
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
                AuthResponse response = authManager.requestAuth(fromAccountManager);
                Bundle result = new Bundle();
                result.putString(KEY_AUTHTOKEN, response.auth);
                result.putString(KEY_ACCOUNT_NAME, account.name);
                result.putString(KEY_ACCOUNT_TYPE, account.type);
                result.putString(KEY_ANDROID_PACKAGE_NAME, packageName);
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
        if (packageName != null) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(packageName.hashCode());
        }
        super.finish();
    }

    private boolean isOAuth() {
        return service.startsWith("oauth2:") || service.startsWith("oauth:");
    }

    private String getScopeLabel(String scope) {
        if (consentData != null) {
            for (ConsentData.ScopeDetails scopeDetails : consentData.scopes) {
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
        if (consentData != null) {
            for (ConsentData.ScopeDetails scopeDetails : consentData.scopes) {
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
                return service.split(" ").length;
            }
            return 1;
        }

        @Override
        public String getItem(int position) {
            if (isOAuth()) {
                String tokens = service.split(":", 2)[1];
                return tokens.split(" ")[position];
            }
            return service;
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
