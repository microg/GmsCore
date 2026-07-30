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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link AsterismConsentRepository}.
 *
 * This is intended for the first implementation pass. A durable implementation
 * (SharedPreferences or server-backed) can be substituted without changing the
 * service contract.
 */
public class InMemoryAsterismConsentRepository implements AsterismConsentRepository {
    private final Map<Integer, AsterismConsent> store = new ConcurrentHashMap<>();

    @Override
    public AsterismConsent get(int requestCode) {
        return store.get(requestCode);
    }

    @Override
    public void set(int requestCode, AsterismConsent consent) {
        store.put(requestCode, consent);
    }
}
