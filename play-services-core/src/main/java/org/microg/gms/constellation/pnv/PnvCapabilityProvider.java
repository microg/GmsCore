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

package org.microg.gms.constellation.pnv;

import com.google.android.gms.constellation.GetPnvCapabilitiesRequest;
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse;

/**
 * Abstraction for Phone Number Verification capability reporting.
 *
 * Implementations inspect the device's SIM/telephony state and map it to the
 * verification methods supported by each SIM slot.
 */
public interface PnvCapabilityProvider {
    GetPnvCapabilitiesResponse getCapabilities(GetPnvCapabilitiesRequest request);
}
