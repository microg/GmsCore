/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal.safeparcel;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Map;

@SuppressWarnings("MagicNumber")
public final class SafeParcelWriter {

    private SafeParcelWriter() {
    }

    private static void writeHeader(Parcel parcel, int fieldId, int size) {
        if (size >= 0xFFFF) {
            parcel.writeInt(0xFFFF0000 | fieldId);
            parcel.writeInt(size);
        } else {
            parcel.writeInt(size << 16 | fieldId);
        }
    }

    @Deprecated
    public static int writeStart(Parcel parcel) {
        return writeObjectHeader(parcel);
    }

    public static int writeObjectHeader(Parcel parcel) {
        writeHeader(parcel, SafeParcelable.SAFE_PARCEL_OBJECT_MAGIC, 0xFFFF);
        return parcel.dataPosition();
    }

    private static int writeObjectHeader(Parcel parcel, int fieldId) {
        writeHeader(parcel, fieldId, 0xFFFF);
        return parcel.dataPosition();
    }

    @Deprecated
    public static void writeEnd(Parcel parcel, int start) {
        finishObjectHeader(parcel, start);
    }

    public static void finishObjectHeader(Parcel parcel, int start) {
        int end = parcel.dataPosition();
        int length = end - start;
        parcel.setDataPosition(start - 4);
        parcel.writeInt(length);
        parcel.setDataPosition(end);
    }

    public static void write(Parcel parcel, int fieldId, Boolean val) {
        if (val == null) return;
        writeHeader(parcel, fieldId, 4);
        parcel.writeInt(val ? 1 : 0);
    }

    public static void write(Parcel parcel, int fieldId, Byte val) {
        if (val == null) return;
        writeHeader(parcel, fieldId, 4);
        parcel.writeInt(val);
    }

    public static void write(Parcel parcel, int fieldId, Short val) {
        if (val == null) return;
        writeHeader(parcel, fieldId, 4);
        parcel.writeInt(val);
    }

    public static void write(Parcel parcel, int fieldId, Integer val) {
        if (val == null) return;
        writeHeader(parcel, fieldId, 4);
        parcel.writeInt(val);
    }

    public static void write(Parcel parcel, int fieldId, Long val) {
        if (val == null) return;
        writeHeader(parcel, fieldId, 8);
        parcel.writeLong(val);
    }

    public static void write(Parcel parcel, int fieldId, Float val) {
        if (val == null) return;
        writeHeader(parcel, fieldId, 4);
        parcel.writeFloat(val);
    }

    public static void write(Parcel parcel, int fieldId, Double val) {
        if (val == null) return;
        writeHeader(parcel, fieldId, 8);
        parcel.writeDouble(val);
    }

    public static void write(Parcel parcel, int fieldId, String val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeString(val);
            finishObjectHeader(parcel, start);
        }
    }

    public static void write(Parcel parcel, int fieldId, Parcelable val, int flags, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            val.writeToParcel(parcel, flags);
            finishObjectHeader(parcel, start);
        }
    }

    public static void write(Parcel parcel, int fieldId, Bundle val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeBundle(val);
            finishObjectHeader(parcel, start);
        }
    }

    public static void write(Parcel parcel, int fieldId, byte[] val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeByteArray(val);
            finishObjectHeader(parcel, start);
        }
    }

    public static void write(Parcel parcel, int fieldId, byte[][] val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeInt(val.length);
            for (byte[] arr : val) {
                parcel.writeByteArray(arr);
            }
            finishObjectHeader(parcel, start);
        }
    }

    public static void write(Parcel parcel, int fieldId, float[] val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeFloatArray(val);
            finishObjectHeader(parcel, start);
        }
    }

    public static void write(Parcel parcel, int fieldId, int[] val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeIntArray(val);
            finishObjectHeader(parcel, start);
        }
    }

    public static void write(Parcel parcel, int fieldId, String[] val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeStringArray(val);
            finishObjectHeader(parcel, start);
        }
    }

    public static void writeStringList(Parcel parcel, int fieldId, List<String> val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeStringList(val);
            finishObjectHeader(parcel, start);
        }
    }

    public static void writeIntegerList(Parcel parcel, int fieldId, List<Integer> val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeInt(val.size());
            for (Integer i : val) {
                parcel.writeInt(i);
            }
            finishObjectHeader(parcel, start);
        }
    }

    public static void writeLongList(Parcel parcel, int fieldId, List<Long> val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeInt(val.size());
            for (Long l : val) {
                parcel.writeLong(l);
            }
            finishObjectHeader(parcel, start);
        }
    }

    public static void writeFloatList(Parcel parcel, int fieldId, List<Float> val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeInt(val.size());
            for (Float f : val) {
                parcel.writeFloat(f);
            }
            finishObjectHeader(parcel, start);
        }
    }

    public static void writeDoubleList(Parcel parcel, int fieldId, List<Double> val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeInt(val.size());
            for (Double d : val) {
                parcel.writeDouble(d);
            }
            finishObjectHeader(parcel, start);
        }
    }

    public static void writeBooleanList(Parcel parcel, int fieldId, List<Boolean> val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeInt(val.size());
            for (Boolean b : val) {
                parcel.writeInt(b ? 1 : 0);
            }
            finishObjectHeader(parcel, start);
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

    public static <T extends Parcelable> void write(Parcel parcel, int fieldId, T[] val, int flags, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeInt(val.length);
            for (T t : val) {
                if (t == null) {
                    parcel.writeInt(0);
                } else {
                    writeArrayPart(parcel, t, flags);
                }
            }
            finishObjectHeader(parcel, start);
        }
    }

    public static <T extends Parcelable> void write(Parcel parcel, int fieldId, List<T> val, int flags, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeInt(val.size());
            for (T t : val) {
                if (t == null) {
                    parcel.writeInt(0);
                } else {
                    writeArrayPart(parcel, t, flags);
                }
            }
            finishObjectHeader(parcel, start);
        }
    }

    public static void write(Parcel parcel, int fieldId, Parcel val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.appendFrom(val, 0, val.dataSize());
            finishObjectHeader(parcel, start);
        }
    }

    public static void write(Parcel parcel, int fieldId, List val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeList(val);
            finishObjectHeader(parcel, start);
        }
    }

    public static void write(Parcel parcel, int fieldId, Map val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeMap(val);
            finishObjectHeader(parcel, start);
        }
    }

    public static void write(Parcel parcel, int fieldId, IBinder val, boolean mayNull) {
        if (val == null) {
            if (mayNull) {
                writeHeader(parcel, fieldId, 0);
            }
        } else {
            int start = writeObjectHeader(parcel, fieldId);
            parcel.writeStrongBinder(val);
            finishObjectHeader(parcel, start);
        }
    }

}