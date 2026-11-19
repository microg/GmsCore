/*
 * Copyright (C) 2026 microG Project Team
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

package org.microg.tools.selfcheck;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.fragment.app.Fragment;

class DocumentationResolver implements SelfCheckGroup.CheckResolver {
    private static final String TAG = "SelfCheck";
    private static final String WIKI_BASE_URL = "https://github.com/microg/GmsCore/wiki/";

    private final String page;

    DocumentationResolver(String page) {
        this.page = page;
    }

    @Override
    public void tryResolve(Fragment fragment) {
        try {
            fragment.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WIKI_BASE_URL + page)));
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }
}
