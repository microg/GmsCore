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

package org.microg.gms.common;

import com.google.android.gms.common.BuildConfig;

public class Constants {
    public static final int GMS_VERSION_CODE = (BuildConfig.VERSION_CODE / 1000) * 1000;
    public static final String GMS_PACKAGE_NAME = "com.google.android.gms";
    public static final String GSF_PACKAGE_NAME = "com.google.android.gsf";
    public static final String GMS_PACKAGE_SIGNATURE_SHA1 = "38918a453d07199354f8b19af05ec6562ced5788";
    public static final String GMS_SECONDARY_PACKAGE_SIGNATURE_SHA1 = "bd32424203e0fb25f36b57e5aa356f9bdd1da998";
    @Deprecated
    public static final int MAX_REFERENCE_VERSION = GMS_VERSION_CODE;
}
