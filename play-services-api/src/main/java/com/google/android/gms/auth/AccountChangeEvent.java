package com.google.android.gms.auth;

import org.microg.safeparcel.AutoSafeParcelable;

public class AccountChangeEvent extends AutoSafeParcelable {
    public static Creator<AccountChangeEvent> CREATOR = new AutoCreator<>(AccountChangeEvent.class);
}
