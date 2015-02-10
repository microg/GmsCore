/*
 * Copyright 2013-2015 µg Project Team
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
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import org.microg.gms.common.Utils;
import org.microg.gms.userinfo.ProfileManager;

import java.io.IOException;

public class AskPermissionActivity extends AccountAuthenticatorActivity {
    public static final String EXTRA_FROM_ACCOUNT_MANAGER = "from_account_manager";

    private static final String TAG = "GmsAuthAskPermission";
    private Account account;
    private String packageName;
    private String service;
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

        account = new Account(getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_NAME),
                getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
        packageName = getIntent().getStringExtra(AccountManager.KEY_ANDROID_PACKAGE_NAME);
        service = getIntent().getStringExtra(AccountManager.KEY_AUTHTOKEN);
        if (getIntent().hasExtra(EXTRA_FROM_ACCOUNT_MANAGER)) fromAccountManager = true;
        int callerUid = getIntent().getIntExtra(AccountManager.KEY_CALLER_UID, 0);
        Utils.checkPackage(this, packageName, callerUid);

        // receive package info
        PackageManager packageManager = getPackageManager();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e);
            finish();
            return;
        }
        CharSequence appLabel = packageManager.getApplicationLabel(applicationInfo);
        Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
        Bitmap profileIcon = ProfileManager.getProfilePicture(this, account, false);

        // receive profile icon
        if (profileIcon != null) {
            ((ImageView) findViewById(R.id.account_photo)).setImageBitmap(profileIcon);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Bitmap profileIcon = ProfileManager.getProfilePicture(AskPermissionActivity.this, account, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((ImageView) findViewById(R.id.account_photo)).setImageBitmap(profileIcon);
                        }
                    });

                }
            }).start();
        }

        ((ImageView) findViewById(R.id.app_icon)).setImageDrawable(appIcon);
        if (isOAuth()) {
            ((TextView) findViewById(R.id.title)).setText(getString(R.string.ask_scope_permission_title, appLabel));
        } else {
            ((TextView) findViewById(R.id.title)).setText(getString(R.string.ask_service_permission_title, appLabel));
        }
        findViewById(android.R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAllow();
            }
        });
        findViewById(android.R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDeny();
            }
        });
        ((ListView) findViewById(R.id.permissions)).setAdapter(new PermissionAdapter());
    }

    public void onAllow() {
        AuthManager.storePermission(this, account, packageName, service);
        findViewById(android.R.id.button1).setEnabled(false);
        findViewById(android.R.id.button2).setEnabled(false);
        findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
        findViewById(R.id.no_progress_bar).setVisibility(View.GONE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Context context = AskPermissionActivity.this;
                String sig = Utils.getFirstPackageSignatureDigest(context, packageName);
                AuthRequest request = new AuthRequest().fromContext(context)
                        .email(account.name)
                        .token(AccountManager.get(context).getPassword(account))
                        .service(service)
                        .app(packageName, sig)
                        .hasPermission();
                if (fromAccountManager) {
                    request.callerIsGms().calledFromAccountManager();
                } else {
                    request.callerIsApp();
                }
                try {
                    AuthResponse response = request.getResponse();
                    AuthManager.storeResponse(context, account, packageName, sig, service, response);
                    Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_AUTHTOKEN, response.auth);
                    result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                    result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                    result.putString(AccountManager.KEY_ANDROID_PACKAGE_NAME, packageName);
                    result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
                    setAccountAuthenticatorResult(result);
                } catch (IOException e) {
                    Log.w(TAG, e);
                }
                finish();

            }
        }).start();
    }

    public void onDeny() {
        finish();
    }

    @Override
    public void finish() {
        super.finish();
    }

    private boolean isOAuth() {
        return service.startsWith("oauth2:") || service.startsWith("oauth:");
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
            String label = "unknown";
            String labelResourceId;
            if (isOAuth()) {
                if (item.startsWith("https://")) {
                    labelResourceId = "permission_scope_" + item.substring(8).replace("/", "_").replace("-", "_");
                } else {
                    labelResourceId = "permission_scope_" + item.replace("/", "_").replace("-", "_");
                }
            } else {
                labelResourceId = "permission_service_" + item + "_label";
            }
            int labelResource = getResources().getIdentifier(labelResourceId, "string", getPackageName());
            if (labelResource != 0) {
                label = getString(labelResource);
            }
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(AskPermissionActivity.this).inflate(R.layout.ask_permission_list_entry, null);
            }
            ((TextView) view.findViewById(android.R.id.text1)).setText(label);
            return view;
        }
    }
}
