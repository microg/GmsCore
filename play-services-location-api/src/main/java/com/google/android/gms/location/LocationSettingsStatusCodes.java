/*
 * Copyright (C) 2017 microG Project Team
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

package com.google.android.gms.location;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import org.microg.gms.common.PublicApi;

/**
 * Location settings specific status codes, for use in {@link Status#getStatusCode()}
 */
@PublicApi
public class LocationSettingsStatusCodes extends CommonStatusCodes {
    /**
     * Location settings can't be changed to meet the requirements, no dialog pops up
     */
    public static final int SETTINGS_CHANGE_UNAVAILABLE = 8502;
}
