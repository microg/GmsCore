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

package org.microg.gms.common.telephony;

/**
 * Abstraction over the system telephony stack.
 *
 * Implementations are responsible for permission checks, multi-SIM handling,
 * and graceful fallback when no telephony data is available.
 */
public interface TelephonyInfoProvider {
    /**
     * MCC+MNC of the SIM operator, e.g. "26207".
     * Returns an empty string when unavailable.
     */
    String getSimOperatorNumeric();

    /**
     * MCC+MNC of the current registered network/cell operator.
     * Returns an empty string when unavailable.
     */
    String getNetworkOperatorNumeric();

    /**
     * IMSI of the active SIM, or empty string if permission is missing.
     */
    String getSubscriberId();

    /**
     * Roaming state as a checkin-friendly string.
     * Returns "mobile-notroaming" or "mobile-roaming" when determinable,
     * otherwise an empty string.
     */
    String getRoamingState();
}
