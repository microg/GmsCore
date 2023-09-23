/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

/**
 * This class is used to supply additional parameters about the user account when creating a new Credential.
 */
@PublicApi
@SafeParcelable.Class
public class PublicKeyCredentialUserEntity extends AbstractSafeParcelable {
    @Field(value = 2, getterName = "getId")
    @NonNull
    private byte[] id;
    @Field(value = 3, getterName = "getName")
    @NonNull
    private String name;
    @Field(value = 4, getterName = "getIcon")
    @Nullable
    private String icon;
    @Field(value = 5, getterName = "getDisplayName")
    @NonNull
    private String displayName;

    private PublicKeyCredentialUserEntity() {
    }

    @Constructor
    public PublicKeyCredentialUserEntity(@Param(2) @NonNull byte[] id, @Param(3) @NonNull String name, @Param(4) @Nullable String icon, @Param(5) @NonNull String displayName) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.displayName = displayName;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public String getIcon() {
        return icon;
    }

    @NonNull
    public byte[] getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublicKeyCredentialUserEntity)) return false;

        PublicKeyCredentialUserEntity that = (PublicKeyCredentialUserEntity) o;

        if (!Arrays.equals(id, that.id)) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (icon != null ? !icon.equals(that.icon) : that.icon != null) return false;
        return displayName != null ? displayName.equals(that.displayName) : that.displayName == null;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{id, name, icon, displayName});
    }

    @Override
    @NonNull
    public String toString() {
        return ToStringHelper.name("PublicKeyCredentialUserEntity")
                .value(id)
                .field("name", name)
                .field("icon", icon)
                .field("displayName", displayName)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Hide
    public static final SafeParcelableCreatorAndWriter<PublicKeyCredentialUserEntity> CREATOR = findCreator(PublicKeyCredentialUserEntity.class);
}
