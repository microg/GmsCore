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

package com.google.android.gms.appdatasearch;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class SuggestionResults extends AutoSafeParcelable {
    @SafeParceled(1000)
    private final int versionCode;
    @SafeParceled(1)
    public final String errorMessage;

    @SafeParceled(2)
    public final String[] s1;
    @SafeParceled(3)
    public final String[] s2;

    private SuggestionResults() {
        versionCode = 2;
        errorMessage = null;
        s1 = s2 = null;
    }

    public SuggestionResults(String errorMessage) {
        versionCode = 2;
        this.errorMessage = errorMessage;
        this.s1 = null;
        this.s2 = null;
    }

    public SuggestionResults(String[] s1, String[] s2) {
        versionCode = 2;
        this.errorMessage = null;
        this.s1 = s1;
        this.s2 = s2;
    }

    public static final Creator<SuggestionResults> CREATOR = new AutoCreator<SuggestionResults>(SuggestionResults.class);
}
