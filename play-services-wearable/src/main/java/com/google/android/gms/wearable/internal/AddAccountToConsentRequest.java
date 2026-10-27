/* SPDX-License-Identifier: Apache-2.0 */
package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class AddAccountToConsentRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public String accountName;
    @SafeParceled(2)
    public boolean hasConsent;

    private AddAccountToConsentRequest() {}

    public AddAccountToConsentRequest(String accountName, boolean hasConsent) {
        this.accountName = accountName;
        this.hasConsent = hasConsent;
    }

    public static final Creator<AddAccountToConsentRequest> CREATOR =
            new AutoCreator<>(AddAccountToConsentRequest.class);
}
