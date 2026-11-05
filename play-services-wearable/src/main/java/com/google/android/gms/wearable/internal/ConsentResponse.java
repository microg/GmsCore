/* SPDX-License-Identifier: Apache-2.0 */
package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Collections;
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
    @SafeParceled(value = 6, subClass = AccountConsentRecordParcelable.class)
    public List<AccountConsentRecordParcelable> accountConsentRecords;
    @SafeParceled(7)
    public String nodeId;
    @SafeParceled(8)
    public Long lastUpdateRequestedTime;

    private ConsentResponse() {}

    public ConsentResponse(int statusCode, boolean hasTosConsent) {
        this.statusCode = statusCode;
        this.hasTosConsent = hasTosConsent;
        this.accountConsentRecords = Collections.emptyList();
    }

    public static final Creator<ConsentResponse> CREATOR = new AutoCreator<>(ConsentResponse.class);
}
