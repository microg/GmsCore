/*
 * Copyright 2013-2025 microG Project Team
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

package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class RecordTermConsentRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public final int termsContext;
    @SafeParceled(2)
    public final int termType;
    @SafeParceled(3)
    public final boolean consentGranted;
    @SafeParceled(4)
    public final String parentGaiaId;
    @SafeParceled(5)
    public final String childGaiaId;
    @SafeParceled(6)
    public final String accountId;

    public RecordTermConsentRequest(int termsContext, int termType, boolean consentGranted, String parentGaiaId, String childGaiaId, String accountId) {
        this.termsContext = termsContext;
        this.termType = termType;
        this.consentGranted = consentGranted;
        this.parentGaiaId = parentGaiaId;
        this.childGaiaId = childGaiaId;
        this.accountId = accountId;
    }

    public static final Creator<RecordTermConsentRequest> CREATOR = new AutoCreator<RecordTermConsentRequest>(RecordTermConsentRequest.class);

}
