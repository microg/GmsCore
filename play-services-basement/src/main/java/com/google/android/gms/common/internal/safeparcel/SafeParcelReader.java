/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal.safeparcel;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("MagicNumber")
public final class SafeParcelReader {

    private SafeParcelReader() {
    }

    @Deprecated
    public static int halfOf(int i) {
        return i & 0xFFFF;
    }

    public static int getFieldId(int header) {
        return header & 0xFFFF;
    }

    @Deprecated
    public static int readSingleInt(Parcel parcel) {
        return parcel.readInt();
    }

    public static int readHeader(Parcel parcel) {
        return parcel.readInt();
    }

    private static int readSize(Parcel parcel, int header) {
        if ((header & 0xFFFF0000) != 0xFFFF0000)
            return header >> 16 & 0xFFFF;
        return parcel.readInt();
    }

    private static void readExpectedSize(Parcel parcel, int header, int expectedSize) {
        int i = readSize(parcel, header);
        if (i != expectedSize)
            throw new ReadException("Expected size " + expectedSize + " got " + i + " (0x" + Integer.toHexString(i) + ")", parcel);
    }

    @Deprecated
    public static int readStart(Parcel parcel) {
        return readObjectHeader(parcel);
    }

    public static int readObjectHeader(Parcel parcel) {
        int header = readHeader(parcel);
        int size = readSize(parcel, header);
        int start = parcel.dataPosition();
        if (getFieldId(header) != SafeParcelable.SAFE_PARCEL_OBJECT_MAGIC)
            throw new ReadException("Expected object header. Got 0x" + Integer.toHexString(header), parcel);
        int end = start + size;
        if ((end < start) || (end > parcel.dataSize()))
            throw new ReadException("Size read is invalid start=" + start + " end=" + end, parcel);
        return end;
    }

    public static int readInt(Parcel parcel, int header) {
        readExpectedSize(parcel, header, 4);
        return parcel.readInt();
    }

    public static byte readByte(Parcel parcel, int header) {
        readExpectedSize(parcel, header, 4);
        return (byte) parcel.readInt();
    }

    public static short readShort(Parcel parcel, int header) {
        readExpectedSize(parcel, header, 4);
        return (short) parcel.readInt();
    }

    public static boolean readBool(Parcel parcel, int header) {
        readExpectedSize(parcel, header, 4);
        return parcel.readInt() != 0;
    }

    public static long readLong(Parcel parcel, int header) {
        readExpectedSize(parcel, header, 8);
        return parcel.readLong();
    }

    public static float readFloat(Parcel parcel, int header) {
        readExpectedSize(parcel, header, 4);
        return parcel.readFloat();
    }

    public static double readDouble(Parcel parcel, int header) {
        readExpectedSize(parcel, header, 8);
        return parcel.readDouble();
    }

    public static String readString(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        String string = parcel.readString();
        parcel.setDataPosition(start + size);
        return string;
    }

    public static IBinder readBinder(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        IBinder binder = parcel.readStrongBinder();
        parcel.setDataPosition(start + size);
        return binder;
    }

    public static <T extends Parcelable> T readParcelable(Parcel parcel, int header, Parcelable.Creator<T> creator) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        T t = creator.createFromParcel(parcel);
        parcel.setDataPosition(start + size);
        return t;
    }

    public static ArrayList readList(Parcel parcel, int header, ClassLoader classLoader) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        ArrayList list = parcel.readArrayList(classLoader);
        parcel.setDataPosition(start + size);
        return list;
    }

    public static HashMap readMap(Parcel parcel, int header, ClassLoader classLoader) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        HashMap map = parcel.readHashMap(classLoader);
        parcel.setDataPosition(start + size);
        return map;
    }

    public static <T extends Parcelable> ArrayList<T> readParcelableList(Parcel parcel, int header, Parcelable.Creator<T> creator) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        ArrayList<T> list = parcel.createTypedArrayList(creator);
        parcel.setDataPosition(start + size);
        return list;
    }

    public static ArrayList<String> readStringList(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        ArrayList<String> list = parcel.createStringArrayList();
        parcel.setDataPosition(start + size);
        return list;
    }

    public static ArrayList<Integer> readIntegerList(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        int length = parcel.readInt();
        ArrayList<Integer> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(parcel.readInt());
        }
        parcel.setDataPosition(start + size);
        return list;
    }

    public static ArrayList<Long> readLongList(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        int length = parcel.readInt();
        ArrayList<Long> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(parcel.readLong());
        }
        parcel.setDataPosition(start + size);
        return list;
    }

    public static ArrayList<Float> readFloatList(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        int length = parcel.readInt();
        ArrayList<Float> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(parcel.readFloat());
        }
        parcel.setDataPosition(start + size);
        return list;
    }

    public static ArrayList<Double> readDoubleList(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        int length = parcel.readInt();
        ArrayList<Double> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(parcel.readDouble());
        }
        parcel.setDataPosition(start + size);
        return list;
    }

    public static ArrayList<Boolean> readBooleanList(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        int length = parcel.readInt();
        ArrayList<Boolean> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(parcel.readInt() != 0);
        }
        parcel.setDataPosition(start + size);
        return list;
    }

    public static <T extends Parcelable> T[] readParcelableArray(Parcel parcel, int header, Parcelable.Creator<T> creator) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        T[] arr = parcel.createTypedArray(creator);
        parcel.setDataPosition(start + size);
        return arr;
    }

    public static String[] readStringArray(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        String[] arr = parcel.createStringArray();
        parcel.setDataPosition(start + size);
        return arr;
    }

    public static byte[] readByteArray(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        byte[] arr = parcel.createByteArray();
        parcel.setDataPosition(start + size);
        return arr;
    }

    public static byte[][] readByteArrayArray(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        int length = parcel.readInt();
        byte[][] arr = new byte[length][];
        for (int i = 0; i < length; i++) {
            arr[i] = parcel.createByteArray();
        }
        parcel.setDataPosition(start + size);
        return arr;
    }

    public static float[] readFloatArray(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        float[] arr = parcel.createFloatArray();
        parcel.setDataPosition(start + size);
        return arr;
    }

    public static int[] readIntArray(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        int[] arr = parcel.createIntArray();
        parcel.setDataPosition(start + size);
        return arr;
    }

    public static Bundle readBundle(Parcel parcel, int header, ClassLoader classLoader) {
        int size = readSize(parcel, header);
        if (size == 0)
            return null;
        int start = parcel.dataPosition();
        Bundle bundle = parcel.readBundle(classLoader);
        parcel.setDataPosition(start + size);
        return bundle;
    }

    public static void skip(Parcel parcel, int header) {
        int size = readSize(parcel, header);
        parcel.setDataPosition(parcel.dataPosition() + size);
    }

    public static class ReadException extends RuntimeException {
        public ReadException(String message, Parcel parcel) {
            super(message);
        }
    }
}