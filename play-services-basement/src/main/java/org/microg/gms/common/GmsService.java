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

package org.microg.gms.common;

public enum GmsService {
    UNKNOWN(-2),
    ANY(-1),
    PEOPLE(5, "com.google.android.gms.people.service.START"),
    LOCATION(6),
    ACCOUNT(9, "com.google.android.gms.accounts.ACCOUNT_SERVICE"),
    CAST(10, "com.google.android.gms.cast.service.BIND_CAST_DEVICE_CONTROLLER_SERVICE"),
    ADDRESS(12, "com.google.android.gms.identity.service.BIND"),
    AUTH_PROXY(16, "com.google.android.gms.auth.service.START"),
    LIGHTWEIGHT_INDEX(19, "com.google.android.gms.icing.LIGHTWEIGHT_INDEX_SERVICE"),
    INDEX(21, "com.google.android.gms.icing.INDEX_SERVICE"),
    LOCATION_REPORTING(22, "com.google.android.gms.location.reporting.service.START", "com.google.android.location.reporting.service.START"),
    LOCATION_MANAGER(23, "com.google.android.location.internal.GoogleLocationManagerService.START"),
    PLAY_LOG(24, "com.google.android.gms.playlog.service.START"),
    CAST_MIRRORING(27, "com.google.android.gms.cast_mirroring.service.START"),
    SEARCH_ADMINISTRATION(30),
    SEARCH_QUERIES(32),
    SEARCH_GLOBAL(33),
    SEARCH_CORPORA(36),
    COMMON(39, "com.google.android.gms.common.service.START"),
    CLEARCUT_LOGGER(40, "com.google.android.gms.clearcut.service.START"),
    USAGE_REPORTING(41, "com.google.android.gms.usagereporting.service.START"),
    SIGN_IN(44, "com.google.android.gms.signin.service.START"),
    CONTEXT_MANAGER(47, "com.google.android.contextmanager.service.ContextManagerService.START"),
    LIGHTWEIGHT_NETWORK_QUALITY(50, "com.google.android.gms.herrevad.services.LightweightNetworkQualityAndroidService.START"),
    PHENOTYPE(51, "com.mgoogle.android.gms.phenotype.service.START"),
    GEODATA(65, "com.google.android.gms.location.places.GeoDataApi"),
    SEARCH_IME(66),
    PLACE_DETECTION(67, "com.google.android.gms.location.places.PlaceDetectionApi"),
    CREDENTIALS(68, "com.google.android.gms.auth.api.credentials.service.START"),
    MEASUREMENT(93, "com.google.android.gms.measurement.START"),
    GASS(116, "com.google.android.gms.gass.START"),
    FACS_CACHE(202, "com.google.android.gms.facs.cache.service.START"),
    IDENTITY_SIGN_IN(212, "com.google.android.gms.auth.api.identity.service.signin.START"),
    FACS_SYNC(220, "com.google.android.gms.facs.internal.service.START"),
    ;

    public int SERVICE_ID;
    public String ACTION;
    public String[] SECONDARY_ACTIONS;

    GmsService(int serviceId, String... actions) {
        this.SERVICE_ID = serviceId;
        this.ACTION = actions.length > 0 ? actions[0] : null;
        this.SECONDARY_ACTIONS = actions;
    }

    public interface ADVERTISING_ID {
        // Has no service id
        String ACTION = "com.google.android.gms.ads.identifier.service.START";
    }

    public static GmsService byServiceId(int serviceId) {
        for (GmsService service : values()) {
            if (service.SERVICE_ID == serviceId) return service;
        }
        return UNKNOWN;
    }

    public static GmsService byAction(String action) {
        for (GmsService service : values()) {
            for (String serviceAction : service.SECONDARY_ACTIONS) {
                if (serviceAction.equals(action)) return service;
            }
        }
        return UNKNOWN;
    }

    public static String nameFromServiceId(int serviceId) {
        return byServiceId(serviceId).toString(serviceId);
    }

    public String toString(int serviceId) {
        if (this != UNKNOWN) return toString();
        return "UNKNOWN(" + serviceId + ")";
    }
}