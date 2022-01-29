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

package com.google.android.gms.people.model;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class AccountMetadata extends AutoSafeParcelable {

    @SafeParceled(1)
    private final int versionCode = 2;

    @SafeParceled(2)
    public boolean hasGooglePlus = true;

    @SafeParceled(3)
    public boolean hasFeature2 = true;

    @SafeParceled(4)
    public boolean hasFeature3 = true;

    @SafeParceled(5)
    public boolean hasFeature4 = true;

    public static Creator<AccountMetadata> CREATOR = new AutoCreator<AccountMetadata>(AccountMetadata.class);
}
