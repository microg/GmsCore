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

package org.microg.gms.games;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.R;

public class UpgradeActivity extends Activity {
    public static final String ACTION_PLAY_GAMES_UPGRADE = "com.google.android.gms.games.PLAY_GAMES_UPGRADE";
    public static final String EXTRA_GAME_PACACKE_NAME = "com.google.android.gms.games.GAME_PACKAGE_NAME";

    private static final String TAG = "GmsUpgActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.games_info);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);

        String packageName = getIntent().getStringExtra(EXTRA_GAME_PACACKE_NAME);

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

        ((ImageView) findViewById(R.id.app_icon)).setImageDrawable(appIcon);
        ((TextView) findViewById(R.id.title)).setText(getString(R.string.games_info_title, appLabel));
        findViewById(android.R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
