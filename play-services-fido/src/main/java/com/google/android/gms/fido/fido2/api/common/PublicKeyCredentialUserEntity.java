/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

/**
 * This class is used to supply additional parameters about the user account when creating a new Credential.
 */
@PublicApi
public class PublicKeyCredentialUserEntity extends AutoSafeParcelable {
    @Field(2)
    private byte[] id;
    @Field(3)
    private String name;
    @Field(4)
    private String icon;
    @Field(5)
    private String displayName;

    private PublicKeyCredentialUserEntity() {
    }

    public PublicKeyCredentialUserEntity(byte[] id, String name, String icon, String displayName) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public byte[] getId() {
        return id;
    }

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
    public String toString() {
        return ToStringHelper.name("PublicKeyCredentialUserEntity")
                .value(id)
                .field("name", name)
                .field("icon", icon)
                .field("displayName", displayName)
                .end();
    }

    @PublicApi(exclude = true)
    public static final Creator<PublicKeyCredentialUserEntity> CREATOR = new AutoCreator<>(PublicKeyCredentialUserEntity.class);
}
