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
package org.microg.gms.maps

object MapsConstants {
    /**
     * No base map tiles.
     */
    const val MAP_TYPE_NONE = 0

    /**
     * Basic maps.
     */
    const val MAP_TYPE_NORMAL = 1

    /**
     * Satellite maps with no labels.
     */
    const val MAP_TYPE_SATELLITE = 2

    /**
     * Terrain maps.
     */
    const val MAP_TYPE_TERRAIN = 3

    /**
     * Satellite maps with a transparent layer of major streets.
     */
    const val MAP_TYPE_HYBRID = 4
}