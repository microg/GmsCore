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
package org.microg.gms.location

object LocationConstants {
    const val KEY_MOCK_LOCATION = "mockLocation"

    // Place picker client->service
    const val EXTRA_PRIMARY_COLOR = "primary_color"
    const val EXTRA_PRIMARY_COLOR_DARK = "primary_color_dark"
    const val EXTRA_CLIENT_VERSION = "gmscore_client_jar_version"
    const val EXTRA_BOUNDS = "latlng_bounds"

    // Place picker service->client
    const val EXTRA_ATTRIBUTION = "third_party_attributions"
    const val EXTRA_FINAL_BOUNDS = "final_latlng_bounds"
    const val EXTRA_PLACE = "selected_place"
    const val EXTRA_STATUS = "status"
}