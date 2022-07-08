/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

/**
 * Represents the information about a relying party with which a credential is associated.
 */
@PublicApi
public class PublicKeyCredentialRpEntity extends AutoSafeParcelable {
    @Field(2)
    private String id;
    @Field(3)
    private String name;
    @Field(4)
    private String icon;

    private PublicKeyCredentialRpEntity() {
    }

    public PublicKeyCredentialRpEntity(String id, String name, String icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public String getId() {
        return id;
    }

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
    public String toString() {
        return ToStringHelper.name("PublicKeyCredentialRpEntity")
                .value(id)
                .field("name", name)
                .field("icon", icon)
                .end();
    }

    @PublicApi(exclude = true)
    public static final Creator<PublicKeyCredentialRpEntity> CREATOR = new AutoCreator<>(PublicKeyCredentialRpEntity.class);
}
