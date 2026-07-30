/*
 * Copyright (C) 2013-2026 microG Project Team
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

package org.microg.gms.asterism;

import com.google.android.gms.common.Feature;

/**
 * Capability features advertised to Asterism/Constellation clients.
 *
 * Values are taken from the Protocol Mapping Register. The server may use
 * these features to gate consent flows and verification methods.
 */
public final class FeatureRegistry {
    private FeatureRegistry() {}

    public static final Feature[] ASTERISM_FEATURES = new Feature[] {
            new Feature("asterism_consent", 3),
            new Feature("one_time_verification", 1),
            new Feature("carrier_auth", 1),
            new Feature("verify_phone_number", 2),
            new Feature("get_iid_token", 1),
            new Feature("get_pnv_capabilities", 1)
    };
}
