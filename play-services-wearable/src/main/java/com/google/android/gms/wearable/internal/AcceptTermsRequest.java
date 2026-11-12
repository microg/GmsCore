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

public class AcceptTermsRequest extends AutoSafeParcelable {
    @Field(1)
    public int termsContext;
    @Field(value = 2, useDirectList = true)
    public List<Integer> acceptedTermTypes;
    @Field(3)
    public String parentGaiaId;
    @Field(4)
    public String childGaiaId;
    @Field(5)
    public String nodeId;
    @Field(6)
    public String accountName;
    @Field(value = 7, useDirectList = true)
    public List<Integer> skippedTermTypes;
    @Field(8)
    public boolean perWatchConsents;
    
    private AcceptTermsRequest() {}

    public AcceptTermsRequest(int termsContext, List<Integer> acceptedTermTypes, String parentGaiaId, String childGaiaId, String nodeId, String accountName, List<Integer> skippedTermTypes, boolean perWatchConsents) {
        this.termsContext = termsContext;
        this.acceptedTermTypes = acceptedTermTypes;
        this.parentGaiaId = parentGaiaId;
        this.childGaiaId = childGaiaId;
        this.nodeId = nodeId;
        this.accountName = accountName;
        this.skippedTermTypes = skippedTermTypes;
        this.perWatchConsents = perWatchConsents;
    }

    public static final Creator<AcceptTermsRequest> CREATOR = new AutoCreator<AcceptTermsRequest>(AcceptTermsRequest.class);
}
