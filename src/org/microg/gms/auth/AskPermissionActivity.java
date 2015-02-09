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
import android.app.Activity;
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

import org.microg.gms.userinfo.ProfileManager;

public class AskPermissionActivity extends Activity {
    private static final String TAG = "GmsAuthAskPermission";
    private Account account;
    private String packageName;
    private String service;

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
        ((TextView) findViewById(R.id.title)).setText(getString(R.string.ask_permission_title, appLabel));
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
        finish();
    }

    public void onDeny() {
        finish();
    }

    private class PermissionAdapter extends BaseAdapter {

        private boolean isOAuth() {
            return service.startsWith("oauth2:") || service.startsWith("oauth:");
        }

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
            if (!isOAuth()) {
                int stringId = getResources().getIdentifier("permission_service_" + item + "_label", "string", getPackageName());
                if (stringId != 0) {
                    label = getString(stringId);
                }
            }
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(AskPermissionActivity.this).inflate(R.layout.ask_permission_list_entry, null);
            }
            ((TextView)view.findViewById(android.R.id.text1)).setText(label);
            return view;
        }
    }
}
