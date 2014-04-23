package com.google.android.gms.common.safeparcel;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class SafeWriter {

	private static void writeStart(Parcel parcel, int position, int length) {
		if (length >= 65535) {
			parcel.writeInt(0xFFFF0000 | position);
			parcel.writeInt(length);
		} else {
			parcel.writeInt(length << 16 | position);
		}
	}

	public static int writeStart(Parcel parcel) {
		return writeStart(parcel, SafeParcelable.SAFE_PARCEL_MAGIC);
	}

	private static int writeStart(Parcel parcel, int position) {
		parcel.writeInt(0xFFFF0000 | position);
		parcel.writeInt(0);
		return parcel.dataPosition();
	}

	public static void writeEnd(Parcel parcel, int start) {
		int end = parcel.dataPosition();
		int length = end - start;
		parcel.setDataPosition(start - 4);
		parcel.writeInt(length);
		parcel.setDataPosition(end);
	}

	public static void write(Parcel parcel, int position, boolean val) {
		writeStart(parcel, position, 4);
		parcel.writeInt(val ? 1 : 0);
	}

	public static void write(Parcel parcel, int position, byte val) {
		writeStart(parcel, position, 4);
		parcel.writeInt(val);
	}

	public static void write(Parcel parcel, int position, short val) {
		writeStart(parcel, position, 4);
		parcel.writeInt(val);
	}

	public static void write(Parcel parcel, int position, int val) {
		writeStart(parcel, position, 4);
		parcel.writeInt(val);
	}

	public static void write(Parcel parcel, int position, long val) {
		writeStart(parcel, position, 8);
		parcel.writeLong(val);
	}

	public static void write(Parcel parcel, int position, float val) {
		writeStart(parcel, position, 4);
		parcel.writeFloat(val);
	}

	public static void write(Parcel parcel, int position, double val) {
		writeStart(parcel, position, 8);
		parcel.writeDouble(val);
	}

	public static void write(Parcel parcel, int position, String val, boolean mayNull) {
		if (val == null) {
			if (mayNull) {
				writeStart(parcel, position, 0);
			}
		} else {
			int start = writeStart(parcel, position);
			parcel.writeString(val);
			writeEnd(parcel, start);
		}
	}

	public static void write(Parcel parcel, int position, Parcelable val, int flags, boolean mayNull) {
		if (val == null) {
			if (mayNull) {
				writeStart(parcel, position, 0);
			}
		} else {
			int start = writeStart(parcel, position);
			val.writeToParcel(parcel, flags);
			writeEnd(parcel, start);
		}
	}

	public static void write(Parcel parcel, int position, Bundle val, boolean mayNull) {
		if (val == null) {
			if (mayNull) {
				writeStart(parcel, position, 0);
			}
		} else {
			int start = writeStart(parcel, position);
			parcel.writeBundle(val);
			writeEnd(parcel, start);
		}
	}

	public static void write(Parcel parcel, int position, byte[] val, boolean mayNull) {
		if (val == null) {
			if (mayNull) {
				writeStart(parcel, position, 0);
			}
		} else {
			int start = writeStart(parcel, position);
			parcel.writeByteArray(val);
			writeEnd(parcel, start);
		}
	}

	public static void write(Parcel parcel, int position, String[] val, boolean mayNull) {
		if (val == null) {
			if (mayNull) {
				writeStart(parcel, position, 0);
			}
		} else {
			int start = writeStart(parcel, position);
			parcel.writeStringArray(val);
			writeEnd(parcel, start);
		}
	}

	public static void writeStringList(Parcel parcel, int position, List<String> val, boolean mayNull) {
		if (val == null) {
			if (mayNull) {
				writeStart(parcel, position, 0);
			}
		} else {
			int start = writeStart(parcel, position);
			parcel.writeStringList(val);
			writeEnd(parcel, start);
		}
	}

	private static <T extends Parcelable> void writeArrayPart(Parcel parcel, T val, int flags) {
		int before = parcel.dataPosition();
		parcel.writeInt(1);
		int start = parcel.dataPosition();
		val.writeToParcel(parcel, flags);
		int end = parcel.dataPosition();
		parcel.setDataPosition(before);
		parcel.writeInt(end - start);
		parcel.setDataPosition(end);
	}

	public static <T extends Parcelable> void write(Parcel parcel, int position, T[] val, int flags, boolean mayNull) {
		if (val == null) {
			if (mayNull) {
				writeStart(parcel, position, 0);
			}
		} else {
			int start = writeStart(parcel, position);
			parcel.writeInt(val.length);
			for (T t : val) {
				if (t == null) {
					parcel.writeInt(0);
				} else {
					writeArrayPart(parcel, t, flags);
				}
			}
			writeEnd(parcel, start);
		}
	}

	public static void write(Parcel parcel, int position, Parcel val, boolean mayNull) {
		if (val == null) {
			if (mayNull) {
				writeStart(parcel, position, 0);
			}
		} else {
			int start = writeStart(parcel, position);
			parcel.appendFrom(val, 0, val.dataSize());
			writeEnd(parcel, start);
		}
	}

	public static void write(Parcel parcel, int position, List val, boolean mayNull) {
		if (val == null) {
			if (mayNull) {
				writeStart(parcel, position, 0);
			}
		} else {
			int start = writeStart(parcel, position);
			parcel.writeList(val);
			writeEnd(parcel, start);
		}
	}

	public static void write(Parcel parcel, int position, IBinder val, boolean mayNull) {
		if (val == null) {
			if (mayNull) {
				writeStart(parcel, position, 0);
			}
		} else {
			int start = writeStart(parcel, position);
			parcel.writeStrongBinder(val);
			writeEnd(parcel, start);
		}
	}

}
