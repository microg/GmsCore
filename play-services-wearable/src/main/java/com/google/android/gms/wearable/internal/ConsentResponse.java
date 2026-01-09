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

import java.util.Arrays;
import java.util.List;

public class ConsentResponse extends AutoSafeParcelable {

    @SafeParceled(1)
    public int statusCode;
    @SafeParceled(2)
    public boolean hasTosConsent;
    @SafeParceled(3)
    public boolean hasLoggingConsent;
    @SafeParceled(4)
    public boolean hasCloudSyncConsent;
    @SafeParceled(5)
    public boolean hasLocationConsent;
    @SafeParceled(6)
    public List accountConsentRecords;
    @SafeParceled(7)
    public String nodeId;
    @SafeParceled(8)
    public Long lastUpdateRequestedTime;

    private ConsentResponse() {}

    public ConsentResponse(int statusCode, boolean hasTosConsent, boolean hasLoggingConsent, boolean hasCloudSyncConsent, boolean hasLocationConsent, List accountConsentRecords, String nodeId, Long lastUpdateRequestedTime) {
        this.statusCode = statusCode;
        this.hasTosConsent = hasTosConsent;
        this.hasLoggingConsent = hasLoggingConsent;
        this.hasCloudSyncConsent = hasCloudSyncConsent;
        this.hasLocationConsent = hasLocationConsent;
        this.accountConsentRecords = accountConsentRecords;
        this.nodeId = nodeId;
        this.lastUpdateRequestedTime = lastUpdateRequestedTime;
    }

    public final int hashCode() {
        return Arrays.hashCode(new Object[]{
                this.statusCode,
                this.hasTosConsent,
                this.hasLoggingConsent,
                this.hasCloudSyncConsent,
                this.hasLocationConsent,
                this.accountConsentRecords,
                this.nodeId,
                this.lastUpdateRequestedTime
        });
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConsentResponse {");
        sb.append("\nstatusCode = ").append(this.statusCode);
        sb.append("\nhasTosConsent = ").append(this.hasTosConsent);
        sb.append("\nhasLoggingConsent = ").append(this.hasLoggingConsent);
        sb.append("\nhasCloudSyncConsent = ").append(this.hasCloudSyncConsent);
        sb.append("\nhasLocationConsent = ").append(this.hasLocationConsent);
        sb.append("\naccountConsentRecords = ").append(this.accountConsentRecords);
        sb.append("\nnodeId = ").append(this.nodeId);
        sb.append("\nlastUpdateRequestedTime = ").append(this.lastUpdateRequestedTime);
        sb.append("\n}\n");
        return sb.toString();

    }

    public static final Creator<ConsentResponse> CREATOR = new AutoCreator<ConsentResponse>(ConsentResponse.class);
}
