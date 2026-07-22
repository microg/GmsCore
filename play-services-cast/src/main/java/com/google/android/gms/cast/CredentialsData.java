/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cast;

import android.os.Parcel;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Credentials data used to identify the credentials of the sender.
 */
@SafeParcelable.Class
public class CredentialsData extends AbstractSafeParcelable {
    /**
     * The credentials type indicating it comes from an Android sender.
     */
    public static final String CREDENTIALS_TYPE_ANDROID = "android";
    /**
     * The credentials type indicating it comes from the cloud (i.e. assistant).
     */
    public static final String CREDENTIALS_TYPE_CLOUD = "cloud";
    /**
     * The credentials type indicating it comes from an iOS sender.
     */
    public static final String CREDENTIALS_TYPE_IOS = "ios";
    /**
     * The credentials type indicating it comes from a Web sender.
     */
    public static final String CREDENTIALS_TYPE_WEB = "web";

    @Field(1)
    final String credentials;
    @Field(2)
    final String credentialsType;

    @Constructor
    CredentialsData(@Param(1) String credentials, @Param(2) String credentialsType) {
        this.credentials = credentials;
        this.credentialsType = credentialsType;
    }

    /**
     * Returns the application-specific blob which identifies and possibly authenticates the user that's requesting to launch
     * or join an receiver app. This field may be {@code null}.
     * <p>
     * For requests sent from Assistant, it is an OAuth 2 token.
     * <p>
     * For requests sent from mobile senders, it is set by sender apps.
     */
    public String getCredentials() {
        return credentials;
    }

    /**
     * Returns the type of the credentials. This field may be {@code null}.
     * <p>
     * This could be one of the {@code CREDENTIALS_TYPE_*} constants or custom-defined.
     */
    public String getCredentialsType() {
        return credentialsType;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CredentialsData> CREATOR = findCreator(CredentialsData.class);
}
