/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import android.os.Parcelable;

import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

@PublicApi
public class COSEAlgorithmIdentifier implements Parcelable {
    private Algorithm algorithm;

    private COSEAlgorithmIdentifier() {
    }

    private COSEAlgorithmIdentifier(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    public static COSEAlgorithmIdentifier fromCoseValue(int value) throws UnsupportedAlgorithmIdentifierException {
        if (value == RSAAlgorithm.LEGACY_RS1.getAlgoValue()) return new COSEAlgorithmIdentifier(RSAAlgorithm.RS1);
        for (RSAAlgorithm algorithm : RSAAlgorithm.values()) {
            if (algorithm.getAlgoValue() == value) return new COSEAlgorithmIdentifier(algorithm);
        }
        for (EC2Algorithm algorithm : EC2Algorithm.values()) {
            if (algorithm.getAlgoValue() == value) return new COSEAlgorithmIdentifier(algorithm);
        }
        throw new UnsupportedAlgorithmIdentifierException(value);
    }

    public int toCoseValue() {
        return algorithm.getAlgoValue();
    }

    @Override
    public String toString() {
        return ToStringHelper.name("COSEAlgorithmIdentifier")
                .value(algorithm)
                .end();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(algorithm.getAlgoValue());
    }

    public static final Creator<COSEAlgorithmIdentifier> CREATOR = new Creator<COSEAlgorithmIdentifier>() {
        @Override
        public COSEAlgorithmIdentifier createFromParcel(Parcel in) {
            try {
                return fromCoseValue(in.readInt());
            } catch (UnsupportedAlgorithmIdentifierException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public COSEAlgorithmIdentifier[] newArray(int size) {
            return new COSEAlgorithmIdentifier[size];
        }
    };

    public static class UnsupportedAlgorithmIdentifierException extends Exception {
        public UnsupportedAlgorithmIdentifierException(int algId) {
            super("Algorithm with COSE value " + algId + " not supported");
        }
    }
}
