package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class AccountConsentRecordParcelable extends AutoSafeParcelable {
    @SafeParceled(1)
    public String accountName;
    @SafeParceled(2)
    public boolean consentGranted;

    private AccountConsentRecordParcelable() {}

    public AccountConsentRecordParcelable(String accountName, boolean consentGranted) {
        this.accountName = accountName;
        this.consentGranted = consentGranted;
    }

    public static final Creator<AccountConsentRecordParcelable> CREATOR =
            new AutoCreator<AccountConsentRecordParcelable>(AccountConsentRecordParcelable.class);
}