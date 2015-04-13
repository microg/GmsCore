package com.google.android.gms.common.api;

import org.microg.safeparcel.AutoSafeParcelable;

public class AccountInfo extends AutoSafeParcelable {
    public static final Creator<AccountInfo> CREATOR = new AutoCreator<AccountInfo>(AccountInfo.class);
}
