/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
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
 * Represents the information about a relying party with which a credential is associated.
 */
@PublicApi
@SafeParcelable.Class
public class PublicKeyCredentialRpEntity extends AbstractSafeParcelable {
    @Field(value = 2, getterName = "getId")
    @NonNull
    private String id;
    @Field(value = 3, getterName = "getName")
    @NonNull
    private String name;
    @Field(value = 4, getterName = "getIcon")
    @Nullable
    private String icon;

    private PublicKeyCredentialRpEntity() {
    }

    @Constructor
    public PublicKeyCredentialRpEntity(@Param(2)@NonNull String id, @Param(3)@NonNull String name, @Param(4)@Nullable String icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    @Nullable
    public String getIcon() {
        return icon;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublicKeyCredentialRpEntity)) return false;

        PublicKeyCredentialRpEntity that = (PublicKeyCredentialRpEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return icon != null ? icon.equals(that.icon) : that.icon == null;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{id, name, icon});
    }

    @Override
    @NonNull
    public String toString() {
        return ToStringHelper.name("PublicKeyCredentialRpEntity")
                .value(id)
                .field("name", name)
                .field("icon", icon)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Hide
    public static final SafeParcelableCreatorAndWriter<PublicKeyCredentialRpEntity> CREATOR = findCreator(PublicKeyCredentialRpEntity.class);
}
