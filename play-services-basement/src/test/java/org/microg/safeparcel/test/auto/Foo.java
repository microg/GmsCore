/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.safeparcel.test.auto;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class Foo extends AutoSafeParcelable {
    @Field(value = 1, versionCode = 2)
    private int versionCode = 2;
    @Field(2)
    private int intPrivate;
    @Field(3)
    public String string;
    @Field(value = 4, subClass = String.class)
    public List<String> stringList = new ArrayList<>();
    @Field(5)
    public Map<String, String> stringStringMap = new HashMap<>();
    @Field(6)
    public Bar bar;
    @Field(7)
    public List<Bar> barList = new ArrayList<>();
    @Field(8)
    public Bar[] barArray = new Bar[0];
    @Field(9)
    public List<Integer> intList = new ArrayList<>();
    @Field(value = 10, useDirectList = true)
    public List<Integer> intList2 = new ArrayList<>();
    @Field(11)
    public float[] floatArray = new float[4];
    @Field(12)
    public int[] intArray = new int[1];
    @Field(13)
    public byte[] byteArray = new byte[1];
    @Field(14)
    public byte[][] byteArrayArray = new byte[1][1];

    private Foo() {
    }

    Foo(int intPrivate) {
        this.intPrivate = intPrivate;
    }

    public int getIntPrivate() {
        return intPrivate;
    }

    @Override
    public String toString() {
        return "Foo{" +
                "versionCode=" + versionCode +
                ", intPrivate=" + intPrivate +
                ", string='" + string + '\'' +
                ", stringList=" + stringList +
                ", stringStringMap=" + stringStringMap +
                ", bar=" + bar +
                ", barList=" + barList +
                ", barArray=" + Arrays.toString(barArray) +
                ", intList=" + intList +
                ", intList2=" + intList2 +
                ", floatArray=" + Arrays.toString(floatArray) +
                ", intArray=" + Arrays.toString(intArray) +
                ", byteArray=" + Arrays.toString(byteArray) +
                ", byteArrayArray=" + Arrays.deepToString(byteArrayArray) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Foo foo = (Foo) o;
        return versionCode == foo.versionCode &&
                intPrivate == foo.intPrivate &&
                Objects.equals(string, foo.string) &&
                Objects.equals(stringList, foo.stringList) &&
                Objects.equals(stringStringMap, foo.stringStringMap) &&
                Objects.equals(bar, foo.bar) &&
                Objects.equals(barList, foo.barList) &&
                Arrays.equals(barArray, foo.barArray) &&
                Objects.equals(intList, foo.intList) &&
                Objects.equals(intList2, foo.intList2) &&
                Arrays.equals(floatArray, foo.floatArray) &&
                Arrays.equals(intArray, foo.intArray) &&
                Arrays.equals(byteArray, foo.byteArray) &&
                Arrays.deepEquals(byteArrayArray, foo.byteArrayArray);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(versionCode, intPrivate, string, stringList, stringStringMap, bar, barList, intList, intList2);
        result = 31 * result + Arrays.hashCode(barArray);
        result = 31 * result + Arrays.hashCode(floatArray);
        result = 31 * result + Arrays.hashCode(intArray);
        result = 31 * result + Arrays.hashCode(byteArray);
        result = 31 * result + Arrays.deepHashCode(byteArrayArray);
        return result;
    }

    public static Creator<Foo> CREATOR = new AutoCreator<>(Foo.class);
}
