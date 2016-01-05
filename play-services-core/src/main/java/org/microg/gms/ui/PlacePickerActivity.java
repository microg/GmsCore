/*
 * Copyright 2013-2016 microG Project Team
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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.android.gms.R;

public class PlacePickerActivity extends AppCompatActivity {
    private static final String TAG = "GmsPlacePicker";

    private static final String EXTRA_PRIMARY_COLOR = "primary_color";
    private static final String EXTRA_PRIMARY_COLOR_DARK = "primary_color_dark";
    private static final String EXTRA_CLIENT_VERSION = "gmscore_client_jar_version";
    private static final String EXTRA_BOUNDS = "latlng_bounds";

    private static final String EXTRA_ATTRIBUTION = "third_party_attributions";
    private static final String EXTRA_FINAL_BOUNDS = "final_latlng_bounds";
    private static final String EXTRA_PLACE = "selected_place";
    private static final String EXTRA_STATUS = "status";

    private int resultCode;
    private Intent resultIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resultCode = RESULT_CANCELED;
        resultIntent = new Intent();
        if (getIntent().hasExtra(EXTRA_BOUNDS))
            resultIntent.putExtra(EXTRA_FINAL_BOUNDS, getIntent().getParcelableExtra(EXTRA_BOUNDS));

        setContentView(R.layout.pick_place);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setBackgroundColor(getIntent().getIntExtra(EXTRA_PRIMARY_COLOR, 0));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setStatusBarColor(getIntent().getIntExtra(EXTRA_PRIMARY_COLOR_DARK, 0));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setResult(resultCode, resultIntent);
    }
}
