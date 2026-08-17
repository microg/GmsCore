/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.issuer;

import android.content.Intent;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

import static com.google.android.gms.common.internal.safeparcel.SafeParcelable.*;

@SafeParcelable.Class
public class UserAddress extends AbstractSafeParcelable {
    private static final String EXTRA_ADDRESS = "com.google.android.gms.identity.intents.EXTRA_ADDRESS";

    @Field(value = 2, getterName = "getName")
    @NonNull
    private final String name;
    @Field(value = 3, getterName = "getAddress1")
    @NonNull
    private final String address1;
    @Field(value = 4, getterName = "getAddress2")
    @NonNull
    private final String address2;
    @Field(value = 5, getterName = "getAddress3")
    @NonNull
    private final String address3;
    @Field(value = 6, getterName = "getAddress4")
    @NonNull
    private final String address4;
    @Field(value = 7, getterName = "getAddress5")
    @NonNull
    private final String address5;
    @Field(value = 8, getterName = "getAdministrativeArea")
    @NonNull
    private final String administrativeArea;
    @Field(value = 9, getterName = "getLocality")
    @NonNull
    private final String locality;
    @Field(value = 10, getterName = "getCountryCode")
    @NonNull
    private final String countryCode;
    @Field(value = 11, getterName = "getPostalCode")
    @NonNull
    private final String postalCode;
    @Field(value = 12, getterName = "getSortingCode")
    @NonNull
    private final String sortingCode;
    @Field(value = 13, getterName = "getPhoneNumber")
    @NonNull
    private final String phoneNumber;
    @Field(value = 14, getterName = "isPostBox")
    private final boolean postBox;
    @Field(value = 15, getterName = "getCompanyName")
    @NonNull
    private final String companyName;
    @Field(value = 16, getterName = "getEmailAddress")
    @NonNull
    private final String emailAddress;

    @Constructor
    public UserAddress(@NonNull @Param(2) String name, @NonNull @Param(3) String address1, @NonNull @Param(4) String address2, @NonNull @Param(5) String address3, @NonNull @Param(6) String address4, @NonNull @Param(7) String address5, @NonNull @Param(8) String administrativeArea, @NonNull @Param(9) String locality, @NonNull @Param(10) String countryCode, @NonNull @Param(11) String postalCode, @NonNull @Param(12) String sortingCode, @NonNull @Param(13) String phoneNumber, @Param(14) boolean postBox, @NonNull @Param(15) String companyName, @NonNull @Param(16) String emailAddress) {
        this.name = name;
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.address5 = address5;
        this.administrativeArea = administrativeArea;
        this.locality = locality;
        this.countryCode = countryCode;
        this.postalCode = postalCode;
        this.sortingCode = sortingCode;
        this.phoneNumber = phoneNumber;
        this.postBox = postBox;
        this.companyName = companyName;
        this.emailAddress = emailAddress;
    }


    @Nullable
    public static UserAddress fromIntent(@NonNull Intent data) {
        if (!data.hasExtra(EXTRA_ADDRESS)) return null;
        return (UserAddress) data.getParcelableExtra(EXTRA_ADDRESS);
    }

    @NonNull
    public String getAddress1() {
        return address1;
    }

    @NonNull
    public String getAddress2() {
        return address2;
    }

    @NonNull
    public String getAddress3() {
        return address3;
    }

    @NonNull
    public String getAddress4() {
        return address4;
    }

    @NonNull
    public String getAddress5() {
        return address5;
    }

    @NonNull
    public String getAdministrativeArea() {
        return administrativeArea;
    }

    @NonNull
    public String getCompanyName() {
        return companyName;
    }

    @NonNull
    public String getCountryCode() {
        return countryCode;
    }

    @NonNull
    public String getEmailAddress() {
        return emailAddress;
    }

    @NonNull
    public String getLocality() {
        return locality;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @NonNull
    public String getPostalCode() {
        return postalCode;
    }

    @NonNull
    public String getSortingCode() {
        return sortingCode;
    }

    public boolean isPostBox() {
        return postBox;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("UserAddress").value(name)
                .value(address1).value(address2).value(address3).value(address4).value(address5)
                .field("administrativeArea", administrativeArea)
                .field("locality", locality)
                .field("countryCode", countryCode)
                .field("postalCode", postalCode)
                .field("sortingCode", sortingCode)
                .field("phoneNumber", phoneNumber)
                .field("postBox", postBox)
                .field("companyName", companyName)
                .field("emailAddress", emailAddress)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<UserAddress> CREATOR = findCreator(UserAddress.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
