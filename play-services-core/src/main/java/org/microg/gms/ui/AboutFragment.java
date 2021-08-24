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

import androidx.fragment.app.Fragment;

import com.google.android.gms.BuildConfig;

import org.microg.tools.ui.AbstractAboutFragment;
import org.microg.tools.ui.AbstractSettingsActivity;

import java.util.List;

public class AboutFragment extends AbstractAboutFragment {

    @Override
    protected void collectLibraries(List<AbstractAboutFragment.Library> libraries) {
        if (BuildConfig.FLAVOR.toLowerCase().contains("mapbox")) {
            libraries.add(new AbstractAboutFragment.Library("com.mapbox.mapboxsdk", "Mapbox Maps SDK for Android", "Three-Clause BSD, Mapbox"));
        } else {
            libraries.add(new AbstractAboutFragment.Library("org.oscim.android", "V™", "GNU LGPLv3, Hannes Janetzek and devemux86"));
        }
        libraries.add(new AbstractAboutFragment.Library("de.hdodenhof.circleimageview", "CircleImageView", "Apache License 2.0, Henning Dodenhof"));
        libraries.add(new AbstractAboutFragment.Library("su.litvak.chromecast.api.v2", "ChromeCast Java API v2", "Apache License 2.0, Vitaly Litvak"));
        libraries.add(new AbstractAboutFragment.Library("org.conscrypt", "Conscrypt", "Apache License 2.0, The Android Open Source Project"));
        libraries.add(new AbstractAboutFragment.Library("org.chromium.net", "Cronet", "BSD-style License, The Chromium Authors"));
        libraries.add(new AbstractAboutFragment.Library("org.microg.safeparcel", "SafeParcel", "Apache License 2.0, microG Team"));
        libraries.add(new AbstractAboutFragment.Library("org.slf4j", "SLF4J", "MIT License, QOS.ch"));
        libraries.add(new AbstractAboutFragment.Library("org.microg.nlp.service", "UnifiedNlp", "Apache License 2.0, microG Team"));
        libraries.add(new AbstractAboutFragment.Library("com.android.volley", "Volley", "Apache License 2.0, The Android Open Source Project"));
        libraries.add(new AbstractAboutFragment.Library("org.microg.wearable", "Wearable", "Apache License 2.0, microG Team"));
        libraries.add(new AbstractAboutFragment.Library("com.squareup.wire", "Wire Protocol Buffers", "Apache License 2.0, Square Inc."));
    }

    public static class AsActivity extends AbstractSettingsActivity {
        public AsActivity() {
            showHomeAsUp = true;
        }

        @Override
        protected Fragment getFragment() {
            return new AboutFragment();
        }
    }
}
