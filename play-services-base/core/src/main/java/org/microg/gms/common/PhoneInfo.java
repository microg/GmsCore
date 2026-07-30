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

import org.microg.gms.common.telephony.TelephonyInfoProvider;

/**
 * Immutable snapshot of telephony information used for check-in and
 * phone-number verification flows.
 *
 * All values are sourced from a {@link TelephonyInfoProvider}. When data is
 * unavailable (missing permission, no SIM, no telephony stack), the fields are
 * empty strings. No carrier identifiers are hardcoded.
 */
public class PhoneInfo {
    public final String cellOperator;
    public final String roaming;
    public final String simOperator;
    public final String imsi;

    /**
     * Construct with empty values for callers that cannot provide a provider.
     */
    public PhoneInfo() {
        this.cellOperator = "";
        this.roaming = "";
        this.simOperator = "";
        this.imsi = "";
    }

    public PhoneInfo(TelephonyInfoProvider provider) {
        this.cellOperator = nonNull(provider.getNetworkOperatorNumeric());
        this.roaming = nonNull(provider.getRoamingState());
        this.simOperator = nonNull(provider.getSimOperatorNumeric());
        this.imsi = nonNull(provider.getSubscriberId());
    }

    private static String nonNull(String value) {
        return value != null ? value : "";
    }
}
