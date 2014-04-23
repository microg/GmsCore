package com.google.android.gms.common.safeparcel;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class SafeReader {
	public static int halfOf(int i) {
		return i & 0xFFFF;
	}

	public static int readSingleInt(Parcel parcel) {
		return parcel.readInt();
	}

	private static int readStart(Parcel parcel, int first) {
		if ((first & 0xFFFF0000) != -65536)
			return first >> 16 & 0xFFFF;
		return parcel.readInt();
	}

	private static void readStart(Parcel parcel, int position, int length) {
		int i = readStart(parcel, position);
		if (i != length)
			throw new ReadException("Expected size " + length + " got " + i + " (0x" + Integer.toHexString(i) + ")", parcel);
	}

	public static int readStart(Parcel parcel) {
		int first = readSingleInt(parcel);
		int length = readStart(parcel, first);
		int start = parcel.dataPosition();
		if (halfOf(first) != SafeParcelable.SAFE_PARCEL_MAGIC)
			throw new ReadException("Expected object header. Got 0x" + Integer.toHexString(first), parcel);
		int end = start + length;
		if ((end < start) || (end > parcel.dataSize()))
			throw new ReadException("Size read is invalid start=" + start + " end=" + end, parcel);
		return end;
	}

	public static int readInt(Parcel parcel, int position) {
		readStart(parcel, position, 4);
		return parcel.readInt();
	}

	public static byte readByte(Parcel parcel, int position) {
		readStart(parcel, position, 4);
		return (byte) parcel.readInt();
	}

	public static short readShort(Parcel parcel, int position) {
		readStart(parcel, position, 4);
		return (short) parcel.readInt();
	}

	public static boolean readBool(Parcel parcel, int position) {
		readStart(parcel, position, 4);
		return parcel.readInt() != 0;
	}

	public static long readLong(Parcel parcel, int position) {
		readStart(parcel, position, 8);
		return parcel.readLong();
	}

	public static float readFloat(Parcel parcel, int position) {
		readStart(parcel, position, 4);
		return parcel.readFloat();
	}

	public static double readDouble(Parcel parcel, int position) {
		readStart(parcel, position, 8);
		return parcel.readDouble();
	}
	public static IBinder readBinder(Parcel parcel, int position) {
		int length = readStart(parcel, position);
		int start = parcel.dataPosition();
		if (length == 0)
			return null;
		IBinder binder = parcel.readStrongBinder();
		parcel.setDataPosition(start + length);
		return binder;
	}

	public static <T extends Parcelable> T readParcelable(Parcel parcel, int position, Parcelable.Creator<T> creator) {
		int length = readStart(parcel, position);
		int start = parcel.dataPosition();
		if (length == 0)
			return null;
		T t = creator.createFromParcel(parcel);
		parcel.setDataPosition(start + length);
		return t;
	}

	public static void skip(Parcel parcel, int position) {
		int i = readStart(parcel, position);
		parcel.setDataPosition(parcel.dataPosition() + i);
	}

	public static class ReadException extends RuntimeException {
		public ReadException(String message, Parcel parcel) {
			super(message);
		}
	}
}
