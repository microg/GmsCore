/*
 * Copyright (C) 2020 e Foundation
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

package com.google.android.gms.maps.model;

import android.os.IBinder;
import com.google.android.gms.dynamic.ObjectWrapper;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

@PublicApi
public class MapStyleOptions extends AutoSafeParcelable {

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    private String json;

    public MapStyleOptions() {
    }

    public MapStyleOptions (String json) {
        this.json = json;
    }

    public String getJson() {
        return this.json;
    }

    public static Creator<MapStyleOptions> CREATOR = new AutoCreator<MapStyleOptions>(MapStyleOptions.class);

}
