/*
 * Copyright 2013-2015 microG Project Team
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

package com.google.android.gms.cast;

import android.net.Uri;

import com.google.android.gms.common.images.WebImage;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.List;

@PublicApi
public class ApplicationMetadata extends AutoSafeParcelable {

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    private String applicationId;
    @SafeParceled(3)
    private String name;
    @SafeParceled(value = 4, subClass = WebImage.class)
    private List<WebImage> images;
    @SafeParceled(value = 5, subClass = String.class)
    private List<String> namespaces;
    @SafeParceled(6)
    private String senderAppIdentifier;
    @SafeParceled(7)
    private Uri senderAppLaunchUri;

    public String getApplicationId() {
        return applicationId;
    }

    public List<WebImage> getImages() {
        return images;
    }

    public String getName() {
        return name;
    }

    public String getSenderAppIdentifier() {
        return senderAppIdentifier;
    }

    public boolean isNamespaceSupported(String namespace) {
        return namespaces.contains(namespace);
    }

    public static final Creator<ApplicationMetadata> CREATOR = new AutoCreator<ApplicationMetadata>(ApplicationMetadata.class);
}
