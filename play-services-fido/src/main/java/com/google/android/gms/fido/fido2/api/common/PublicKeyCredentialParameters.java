/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import androidx.annotation.NonNull;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

/**
 * This class supplies additional parameters when creating a new credential.
 */
@PublicApi
public class PublicKeyCredentialParameters extends AutoSafeParcelable {
    @Field(2)
    @NonNull
    private PublicKeyCredentialType type;
    @Field(3)
    @NonNull
    private COSEAlgorithmIdentifier algorithm;

    private PublicKeyCredentialParameters() {
    }

    public PublicKeyCredentialParameters(@NonNull String type, int algorithm) {
        try {
            this.type = PublicKeyCredentialType.fromString(type);
        } catch (PublicKeyCredentialType.UnsupportedPublicKeyCredTypeException e) {
            throw new IllegalArgumentException(e);
        }
        try {
            this.algorithm = COSEAlgorithmIdentifier.fromCoseValue(algorithm);
        } catch (COSEAlgorithmIdentifier.UnsupportedAlgorithmIdentifierException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @NonNull
    public COSEAlgorithmIdentifier getAlgorithm() {
        return algorithm;
    }

    public int getAlgorithmIdAsInteger() {
        return algorithm.toCoseValue();
    }

    @NonNull
    public PublicKeyCredentialType getType() {
        return type;
    }

    @NonNull
    public String getTypeAsString() {
        return type.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublicKeyCredentialParameters)) return false;

        PublicKeyCredentialParameters that = (PublicKeyCredentialParameters) o;

        if (type != that.type) return false;
        return algorithm != null ? algorithm.equals(that.algorithm) : that.algorithm == null;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{type, algorithm});
    }

    @Override
    @NonNull
    public String toString() {
        return ToStringHelper.name("PublicKeyCredentialParameters")
                .field("type", type)
                .field("algorithm", algorithm)
                .end();
    }

    @Hide
    public static final Creator<PublicKeyCredentialParameters> CREATOR = new AutoCreator<>(PublicKeyCredentialParameters.class);
}
