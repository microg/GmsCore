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


import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import android.os.Bundle;
import android.net.Uri;


public class DynamicLinkData extends AutoSafeParcelable {
    @SafeParceled(1)
    public final String dynamicLink;

    @SafeParceled(2)
    public final String deepLink;

    @SafeParceled(3)
    public final int minVersion;

    @SafeParceled(4)
    public final long clickTimestamp;

    @SafeParceled(5)
    public final Bundle extensionBundle;

    @SafeParceled(6)
    public final Uri redirectUrl;

    public DynamicLinkData() {
        dynamicLink = new String();
        deepLink = new String();
        minVersion = 0;
        clickTimestamp = 0;
        extensionBundle = new Bundle();
        redirectUrl = Uri.EMPTY;
    }


    public static final Creator<DynamicLinkData> CREATOR = new AutoCreator<DynamicLinkData>(DynamicLinkData.class);
}