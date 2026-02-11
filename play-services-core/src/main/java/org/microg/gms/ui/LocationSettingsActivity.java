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

import static org.microg.gms.accountsettings.ui.ExtensionsKt.ACTION_LOCATION_SHARING;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import org.microg.gms.accountsettings.ui.MainActivity;

public class LocationSettingsActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (ACTION_LOCATION_SHARING.equals(getIntent().getAction())) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setAction(ACTION_LOCATION_SHARING);
                startActivity(intent);
            }
        } catch (Exception ignore) {
        }
        finish();
    }
}
