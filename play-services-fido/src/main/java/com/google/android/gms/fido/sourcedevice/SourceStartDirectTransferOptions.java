/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.sourcedevice;

import android.accounts.Account;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

/**
 * Customized options to start direct transfer.
 */
@SafeParcelable.Class
public class SourceStartDirectTransferOptions extends AbstractSafeParcelable {
    /**
     * Value of the callerType if the caller is unknown.
     */
    public static final int CALLER_TYPE_UNKNOWN = 0;
    /**
     * Value of the callerType if the caller is a source device.
     */
    public static final int CALLER_TYPE_SOURCE_DEVICE = 1;
    /**
     * Value of the callerType if the caller is browser.
     */
    public static final int CALLER_TYPE_BROWSER = 2;

    @Field(value = 1, getterName = "getCallerType")
    private int callerType;
    @Field(value = 2, getterName = "isUserPresenceSupported")
    private boolean userPresenceSupported;
    @Field(value = 3, getterName = "getAccounts", subClass = Account.class)
    private List<Account> accounts;
    @Field(value = 4, getterName = "isUserVerified")
    private boolean userVerified;
    @Field(value = 5, getterName = "getUserVerificationOrigin")
    private String userVerificationOrigin;

    private SourceStartDirectTransferOptions() {
    }

    /**
     * Constructor for the {@link SourceStartDirectTransferOptions}.
     */
    public SourceStartDirectTransferOptions(List<Account> accounts) {
        this(CALLER_TYPE_SOURCE_DEVICE, true, accounts, false, null);
    }

    @Constructor
    public SourceStartDirectTransferOptions(
            @Param(1) int callerType,
            @Param(2) boolean userPresenceSupported,
            @Param(3) List<Account> accounts,
            @Param(4) boolean userVerified,
            @Param(5) String userVerificationOrigin
    ) {
        this.callerType = callerType;
        this.userPresenceSupported = userPresenceSupported;
        this.accounts = accounts;
        this.userVerified = userVerified;
        this.userVerificationOrigin = userVerificationOrigin;
    }

    public int getCallerType() {
        return callerType;
    }

    public boolean isUserPresenceSupported() {
        return userPresenceSupported;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public boolean isUserVerified() {
        return userVerified;
    }

    public String getUserVerificationOrigin() {
        return userVerificationOrigin;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SourceStartDirectTransferOptions> CREATOR = findCreator(SourceStartDirectTransferOptions.class);
}
