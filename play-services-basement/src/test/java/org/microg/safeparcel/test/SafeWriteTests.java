/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.safeparcel.test;

import android.os.Parcel;

import org.junit.Test;
import org.microg.safeparcel.SafeParcelReader;
import org.microg.safeparcel.SafeParcelWriter;
import org.microg.safeparcel.test.mock.MockParcel;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * These tests only ensure that the written safe parcelable is in fact safe.
 */
public class SafeWriteTests {
    private static int FIELD_ID = 1123;

    private void testSkipField(Parcel parcel1) {
        Parcel parcel2 = MockParcel.obtain();
        parcel2.unmarshall(parcel1.marshall(), 0, parcel1.dataSize());
        parcel2.setDataPosition(0);
        int header = SafeParcelReader.readHeader(parcel2);
        SafeParcelReader.skip(parcel2, header);
        assertEquals(SafeParcelReader.getFieldId(header), FIELD_ID);
        assertEquals(parcel1.dataPosition(), parcel2.dataPosition());
    }

    @Test
    public void testWriteBool() {
        Parcel parcel1 = MockParcel.obtain();
        SafeParcelWriter.write(parcel1, FIELD_ID, true);
        testSkipField(parcel1);
    }

    @Test
    public void testWriteByte() {
        Parcel parcel1 = MockParcel.obtain();
        SafeParcelWriter.write(parcel1, FIELD_ID, (byte) 1);
        testSkipField(parcel1);
    }

    @Test
    public void testWriteShort() {
        Parcel parcel1 = MockParcel.obtain();
        SafeParcelWriter.write(parcel1, FIELD_ID, (short) 1);
        testSkipField(parcel1);
    }

    @Test
    public void testWriteInt() {
        Parcel parcel1 = MockParcel.obtain();
        SafeParcelWriter.write(parcel1, FIELD_ID, 1);
        testSkipField(parcel1);
    }

    @Test
    public void testWriteLong() {
        Parcel parcel1 = MockParcel.obtain();
        SafeParcelWriter.write(parcel1, FIELD_ID, 1L);
        testSkipField(parcel1);
    }

    @Test
    public void testWriteFloat() {
        Parcel parcel1 = MockParcel.obtain();
        SafeParcelWriter.write(parcel1, FIELD_ID, 1.0f);
        testSkipField(parcel1);
    }

    @Test
    public void testWriteDouble() {
        Parcel parcel1 = MockParcel.obtain();
        SafeParcelWriter.write(parcel1, FIELD_ID, 1.0);
        testSkipField(parcel1);
    }

    @Test
    public void testWriteString() {
        Parcel parcel1 = MockParcel.obtain();
        SafeParcelWriter.write(parcel1, FIELD_ID, "Test", false);
        testSkipField(parcel1);
    }

    @Test
    public void testWriteObject() {
        Parcel parcel1 = MockParcel.obtain();
        int start = SafeParcelWriter.writeObjectHeader(parcel1);
        SafeParcelWriter.write(parcel1, 1, 1);
        SafeParcelWriter.write(parcel1, 2, "Test", false);
        SafeParcelWriter.finishObjectHeader(parcel1, start);
        Parcel parcel2 = MockParcel.obtain();
        parcel2.unmarshall(parcel1.marshall(), 0, parcel1.dataSize());
        parcel2.setDataPosition(0);
        int end = SafeParcelReader.readObjectHeader(parcel2);
        assertEquals(parcel1.dataPosition(), end);
    }
}
