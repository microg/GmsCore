/*
 * Copyright (C) 2013-2019 microG Project Team
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

package com.google.android.gms.common.images;

import android.net.Uri;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Locale;

public class WebImage extends AutoSafeParcelable {
    public static final Creator<WebImage> CREATOR = new AutoCreator<>(WebImage.class);

    public WebImage () {
        this.uri = null;
    }

    public WebImage (Uri uri) {
        this.uri = uri;
    }

    @SafeParceled(1)
    private final int versionCode = 1;

    @SafeParceled(2)
    private final Uri uri;

    @SafeParceled(3)
    private final int width = 0;

    @SafeParceled(4)
    private final int height = 0;

    public Uri getUrl() {
        return uri;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String toString() {
        return String.format(Locale.getDefault(), "Image %dx%d %s", width, height, uri.toString());
    }
}
