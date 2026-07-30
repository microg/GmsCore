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

package org.microg.gms.asterism.consent;

/**
 * Local snapshot of the consent values supplied by a client.
 *
 * This is intentionally separate from the SafeParcelable request/response types
 * and contains only the fields that represent the user's consent decision.
 */
public final class AsterismConsent {
    public final int consentValue;
    public final int consentVersionValue;
    public final int deviceConsentSourceValue;
    public final int deviceConsentVersionValue;

    public AsterismConsent(int consentValue, int consentVersionValue,
                           int deviceConsentSourceValue, int deviceConsentVersionValue) {
        this.consentValue = consentValue;
        this.consentVersionValue = consentVersionValue;
        this.deviceConsentSourceValue = deviceConsentSourceValue;
        this.deviceConsentVersionValue = deviceConsentVersionValue;
    }
}
