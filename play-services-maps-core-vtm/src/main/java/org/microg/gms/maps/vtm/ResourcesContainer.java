/*
 * Copyright (C) 2019 microG Project Team
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

package org.microg.gms.maps.vtm;

import android.content.res.Resources;

public class ResourcesContainer {
    private static Resources resources;

    public static void set(Resources resources) {
        ResourcesContainer.resources = resources;
    }

    public static Resources get() {
        if (resources == null) {
            throw new IllegalStateException("Resources have not been initialized");
        } else {
            return resources;
        }
    }
}
