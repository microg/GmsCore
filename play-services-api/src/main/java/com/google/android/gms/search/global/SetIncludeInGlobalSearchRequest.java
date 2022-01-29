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

package com.google.android.gms.search.global;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class SetIncludeInGlobalSearchRequest extends AutoSafeParcelable {

    @SafeParceled(1000)
    private final int versionCode = 1;

    @SafeParceled(1)
    public String packageName;

    @SafeParceled(2)
    public String source;

    @SafeParceled(3)
    public boolean enabled;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SetIncludeInGlobalSearchRequest{");
        sb.append("packageName='").append(packageName).append('\'');
        sb.append(", source='").append(source).append('\'');
        sb.append(", enabled=").append(enabled);
        sb.append('}');
        return sb.toString();
    }

    public static final Creator<SetIncludeInGlobalSearchRequest> CREATOR = new AutoCreator<SetIncludeInGlobalSearchRequest>(SetIncludeInGlobalSearchRequest.class);
}
