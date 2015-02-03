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

package org.microg.gms.auth.login;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;

import com.google.android.gms.R;

public abstract class BaseActivity extends Activity {
    private static final int TITLE_MIN_HEIGHT = 64;
    private static final double TITLE_WIDTH_FACTOR = (8.0 / 18.0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_base);
        formatTitle();
    }

    private void formatTitle() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            double widthPixels = (double) (getResources().getDisplayMetrics().widthPixels);
            findViewById(R.id.title_container).getLayoutParams().height =
                    (int) (dpToPx(TITLE_MIN_HEIGHT) + (TITLE_WIDTH_FACTOR * widthPixels));
        } else {
            findViewById(R.id.title_container).getLayoutParams().height = dpToPx(TITLE_MIN_HEIGHT);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        formatTitle();
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
