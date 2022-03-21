/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.phenotype;

import org.microg.safeparcel.AutoSafeParcelable;

public class Flag extends AutoSafeParcelable {
    @Field(2)
    public String name;
    @Field(3)
    private long longValue;
    @Field(4)
    private boolean boolValue;
    @Field(5)
    private double doubleValue;
    @Field(6)
    private String stringValue;
    @Field(7)
    private byte[] bytesValue;
    @Field(8)
    public int dataType;
    @Field(9)
    public int flagType;

    private Flag() {
    }

    public Flag(String name, long longValue, int flagType) {
        this.name = name;
        this.longValue = longValue;
        this.dataType = DATA_TYPE_LONG;
        this.flagType = flagType;
    }

    public Flag(String name, boolean boolValue, int flagType) {
        this.name = name;
        this.boolValue = boolValue;
        this.dataType = DATA_TYPE_BOOL;
        this.flagType = flagType;
    }

    public Flag(String name, double doubleValue, int flagType) {
        this.name = name;
        this.doubleValue = doubleValue;
        this.dataType = DATA_TYPE_DOUBLE;
        this.flagType = flagType;
    }

    public Flag(String name, String stringValue, int flagType) {
        this.name = name;
        this.stringValue = stringValue;
        this.dataType = DATA_TYPE_STRING;
        this.flagType = flagType;
    }

    public Flag(String name, byte[] bytesValue, int flagType) {
        this.name = name;
        this.bytesValue = bytesValue;
        this.dataType = DATA_TYPE_BYTES;
        this.flagType = flagType;
    }

    public long getLong() {
        if (dataType == DATA_TYPE_LONG)
            return longValue;
        throw new IllegalArgumentException("Not a long type");
    }

    public boolean getBool() {
        if (dataType == DATA_TYPE_BOOL)
            return boolValue;
        throw new IllegalArgumentException("Not a boolean type");
    }

    public double getDouble() {
        if (dataType == DATA_TYPE_DOUBLE)
            return doubleValue;
        throw new IllegalArgumentException("Not a double type");
    }

    public String getString() {
        if (dataType == DATA_TYPE_STRING)
            return stringValue;
        throw new IllegalArgumentException("Not a String type");
    }

    public byte[] getBytes() {
        if (dataType == DATA_TYPE_BYTES)
            return bytesValue;
        throw new IllegalArgumentException("Not a bytes type");
    }

    public static final int DATA_TYPE_LONG = 1;
    public static final int DATA_TYPE_BOOL = 2;
    public static final int DATA_TYPE_DOUBLE = 3;
    public static final int DATA_TYPE_STRING = 4;
    public static final int DATA_TYPE_BYTES = 5;
    public static final Creator<Flag> CREATOR = new AutoCreator<>(Flag.class);
}
