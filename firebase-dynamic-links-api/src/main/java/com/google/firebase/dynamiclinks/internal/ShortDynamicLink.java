/*
 * Copyright (C) 2019 e Foundation
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

package com.google.firebase.dynamiclinks.internal;


import com.google.firebase.dynamiclinks.internal.Warning;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import android.net.Uri;
import java.util.List;
import java.util.ArrayList;


public class ShortDynamicLink extends AutoSafeParcelable {
    @SafeParceled(1)
    public final Uri shortLink;

    @SafeParceled(2)
    public final Uri previewLink;

    @SafeParceled(3)
    public final List<Warning> warnings;


    public ShortDynamicLink() {
        shortLink = Uri.EMPTY;
        previewLink = Uri.EMPTY;

        warnings = new ArrayList<>();
    }


    public static final Creator<ShortDynamicLink> CREATOR = new AutoCreator<ShortDynamicLink>(ShortDynamicLink.class);
}