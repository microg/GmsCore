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

import java.util.List;

public class GetTermsResponse extends AutoSafeParcelable {
    @SafeParceled(1)
    public final int statusCode;
    @SafeParceled(2)
    public final List consents; // correct name is unknown, but assuming this is a consent list

    public GetTermsResponse(int statusCode, List consents) {
        this.statusCode = statusCode;
        this.consents = consents;
    }

    public static final Creator<GetTermsResponse> CREATOR = new AutoCreator<GetTermsResponse>(GetTermsResponse.class);
}
