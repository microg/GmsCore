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

package org.microg.tools;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * This is just an activity that forwards to the systems native account picker
 */
public class AccountPickerActivity extends Activity {
    private static final int REQUEST_CODE = AccountPickerActivity.class.hashCode();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        Intent intent = new Intent();
        ComponentName componentName =
                ComponentName.unflattenFromString("android/.accounts.ChooseTypeAndAccountActivity");
        intent.setClassName(componentName.getPackageName(), componentName.getClassName());
        intent.putExtras(extras);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            setResult(resultCode, data);
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
