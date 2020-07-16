/*
 * Copyright (C) 2017 microG Project Team
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

package org.microg.tools.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;

public class ResourceSettingsFragment extends AbstractSettingsFragment {

    public static final String EXTRA_PREFERENCE_RESOURCE = "preferencesResource";

    protected int preferencesResource;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        Bundle b = getArguments();
        if (b != null) {
            preferencesResource = b.getInt(EXTRA_PREFERENCE_RESOURCE, preferencesResource);
        }
        if (preferencesResource != 0) {
            addPreferencesFromResource(preferencesResource);
        }
    }
}
