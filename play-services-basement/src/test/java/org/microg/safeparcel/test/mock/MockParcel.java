/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.safeparcel.test.mock;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockParcel {

    public static Parcel obtain() {
        return new MockParcel().parcel;
    }

    private int dataPosition = 0;
    private byte buf[] = new byte[32];
    private int count = 0;
    private Parcel parcel = mock(Parcel.class);
    private DataOutputStream dos = new DataOutputStream(new BytesOutputStream());
    private DataInputStream dis = new DataInputStream(new BytesInputStream());

    private MockParcel() {
        setup();
    }

    private void setup() {
        doAnswer(i -> {
            dos.writeByte(i.<Byte>getArgument(0));
            return null;
        }).when(parcel).writeByte(anyByte());
        doAnswer(i -> {
            dos.writeInt(i.<Integer>getArgument(0));
            return null;
        }).when(parcel).writeInt(anyInt());
        doAnswer(i -> {
            dos.writeLong(i.<Long>getArgument(0));
            return null;
        }).when(parcel).writeLong(anyLong());
        doAnswer(i -> {
            dos.writeFloat(i.<Float>getArgument(0));
            return null;
        }).when(parcel).writeFloat(anyFloat());
        doAnswer(i -> {
            dos.writeDouble(i.<Double>getArgument(0));
            return null;
        }).when(parcel).writeDouble(anyDouble());
        doAnswer(i -> {
            dos.writeUTF(i.getArgument(0));
            return null;
        }).when(parcel).writeString(anyString());
        doAnswer(i -> {
            dos.writeUTF(i.<Parcelable>getArgument(0).getClass().getName());
            i.<Parcelable>getArgument(0).writeToParcel(parcel, i.<Integer>getArgument(1));
            return null;
        }).when(parcel).writeParcelable(any(Parcelable.class), anyInt());
        doAnswer(i -> {
            dos.write(i.getArgument(0), i.<Integer>getArgument(1), i.<Integer>getArgument(2));
            return null;
        }).when(parcel).unmarshall(any(), anyInt(), anyInt());
        doAnswer(i -> {
            byte[] val = i.getArgument(0);
            if (val == null) {
                parcel.writeInt(-1);
                return null;
            }
            int N = val.length;
            int j = 0;
            parcel.writeInt(N);
            while (j < N) {
                parcel.writeByte(val[j]);
                j++;
            }
            return null;
        }).when(parcel).writeByteArray(any());
        doAnswer(i -> {
            float[] val = i.getArgument(0);
            if (val == null) {
                parcel.writeInt(-1);
                return null;
            }
            int N = val.length;
            int j = 0;
            parcel.writeInt(N);
            while (j < N) {
                parcel.writeFloat(val[j]);
                j++;
            }
            return null;
        }).when(parcel).writeFloatArray(any());
        doAnswer(i -> {
            int[] val = i.getArgument(0);
            if (val == null) {
                parcel.writeInt(-1);
                return null;
            }
            int N = val.length;
            int j = 0;
            parcel.writeInt(N);
            while (j < N) {
                parcel.writeInt(val[j]);
                j++;
            }
            return null;
        }).when(parcel).writeIntArray(any());
        doAnswer(i -> {
            List<String> val = i.getArgument(0);
            if (val == null) {
                parcel.writeInt(-1);
                return null;
            }
            int N = val.size();
            int j = 0;
            parcel.writeInt(N);
            while (j < N) {
                parcel.writeString(val.get(j));
                j++;
            }
            return null;
        }).when(parcel).writeStringList(any());
        doAnswer(i -> {
            List val = i.getArgument(0);
            if (val == null) {
                parcel.writeInt(-1);
                return null;
            }
            int N = val.size();
            int j = 0;
            parcel.writeInt(N);
            while (j < N) {
                parcel.writeValue(val.get(j));
                j++;
            }
            return null;
        }).when(parcel).writeList(any(List.class));
        doAnswer(i -> {
            List<Parcelable> val = i.getArgument(0);
            if (val == null) {
                parcel.writeInt(-1);
                return null;
            }
            int N = val.size();
            int j = 0;
            parcel.writeInt(N);
            while (j < N) {
                parcel.writeParcelable(val.get(j), i.getArgument(1));
                j++;
            }
            return null;
        }).when(parcel).writeParcelableList(any(List.class), anyInt());
        doAnswer(i -> {
            Map val = i.getArgument(0);
            if (val == null) {
                parcel.writeInt(-1);
                return null;
            }
            int N = val.size();
            parcel.writeInt(N);
            for (Map.Entry o : (Set<Map.Entry>)val.entrySet()) {
                parcel.writeValue(o.getKey());
                parcel.writeValue(o.getValue());
            }
            return null;
        }).when(parcel).writeMap(any());
        doAnswer(i -> {
            Object o = i.getArgument(0);
            if (o == null) {
                parcel.writeInt(-1);
            } else if (o instanceof String) {
                parcel.writeInt(0);
                parcel.writeString((String)o);
            } else if (o instanceof Integer) {
                parcel.writeInt(1);
                parcel.writeInt((Integer)o);
            } else if (o instanceof Map) {
                parcel.writeInt(2);
                parcel.writeMap((Map)o);
            } else if (o instanceof Parcelable) {
                parcel.writeInt(4);
                parcel.writeParcelable((Parcelable)o, 0);
            } else if (o instanceof Long) {
                parcel.writeInt(6);
                parcel.writeLong((Long)o);
            } else if (o instanceof Float) {
                parcel.writeInt(7);
                parcel.writeFloat((Float)o);
            } else if (o instanceof Double) {
                parcel.writeInt(8);
                parcel.writeDouble((Double)o);
            } else {
                throw new NoSuchMethodException();
            }
            return null;
        }).when(parcel).writeValue(any());


        when(parcel.readByte()).thenAnswer(i -> dis.readByte());
        when(parcel.readInt()).thenAnswer(i -> dis.readInt());
        when(parcel.readLong()).thenAnswer(i -> dis.readLong());
        when(parcel.readFloat()).thenAnswer(i -> dis.readFloat());
        when(parcel.readDouble()).thenAnswer(i -> dis.readDouble());
        when(parcel.readString()).thenAnswer(i -> dis.readUTF());
        when(parcel.readParcelable(any(ClassLoader.class))).then(i -> {
            String className = dis.readUTF();
            Parcelable.Creator creator = (Parcelable.Creator) i.<ClassLoader>getArgument(0).loadClass(className).getDeclaredField("CREATOR").get(null);
            return creator.createFromParcel(parcel);
        });
        when(parcel.createStringArrayList()).thenAnswer(i -> {
            int N = parcel.readInt();
            if (N == -1) return null;
            ArrayList<String> res = new ArrayList<>(N);
            int j = 0;
            while (j < N) {
                res.add(parcel.readString());
                j++;
            }
            return res;
        });
        when(parcel.createTypedArrayList(any(Parcelable.Creator.class))).thenAnswer(i -> {
            int N = parcel.readInt();
            if (N == -1) return null;
            ArrayList res = new ArrayList(N);
            int j = 0;
            while (j < N) {
                res.add(parcel.readTypedObject(i.getArgument(0)));
                j++;
            }
            return res;
        });
        when(parcel.createTypedArray(any(Parcelable.Creator.class))).thenAnswer(i -> {
            int N = parcel.readInt();
            if (N == -1) return null;
            Object[] res = i.<Parcelable.Creator>getArgument(0).newArray(N);
            int j = 0;
            while (j < N) {
                res[j] = parcel.readTypedObject(i.getArgument(0));
                j++;
            }
            return res;
        });
        when(parcel.createByteArray()).thenAnswer(i -> {
            int N = parcel.readInt();
            if (N == -1) return null;
            byte[] res = new byte[N];
            int j = 0;
            while (j < N) {
                res[j] = parcel.readByte();
                j++;
            }
            return res;
        });
        when(parcel.createFloatArray()).thenAnswer(i -> {
            int N = parcel.readInt();
            if (N == -1) return null;
            float[] res = new float[N];
            int j = 0;
            while (j < N) {
                res[j] = parcel.readFloat();
                j++;
            }
            return res;
        });
        when(parcel.createIntArray()).thenAnswer(i -> {
            int N = parcel.readInt();
            if (N == -1) return null;
            int[] res = new int[N];
            int j = 0;
            while (j < N) {
                res[j] = parcel.readInt();
                j++;
            }
            return res;
        });
        when(parcel.readArrayList(any(ClassLoader.class))).thenAnswer(i -> {
            int N = parcel.readInt();
            if (N == -1) return null;
            ArrayList res = new ArrayList(N);
            int j = 0;
            while (j < N) {
                res.add(parcel.readValue(i.getArgument(0)));
                j++;
            }
            return res;
        });
        when(parcel.readHashMap(any(ClassLoader.class))).thenAnswer(i -> {
            int N = parcel.readInt();
            HashMap res = new HashMap(N);
            int j = 0;
            while (j < N) {
                Object key = parcel.readValue(i.getArgument(0));
                Object value = parcel.readValue(i.getArgument(0));
                res.put(key, value);
                j++;
            }
            return res;
        });
        when(parcel.readValue(any(ClassLoader.class))).thenAnswer(i -> {
            int type = parcel.readInt();
            switch (type) {
                case -1:
                    return null;
                case 0:
                    return parcel.readString();
                case 1:
                    return parcel.readInt();
                case 2:
                    return parcel.readHashMap(i.getArgument(0));
                case 3:
                    return parcel.readBundle(i.getArgument(0));
                case 4:
                    return parcel.readParcelable(i.getArgument(0));
                case 5:
                    return (short) parcel.readInt();
                case 6:
                    return parcel.readLong();
                case 7:
                    return parcel.readFloat();
                case 8:
                    return parcel.readDouble();
                default:
                    throw new NoSuchMethodException();
            }
        });
        when(parcel.readTypedObject(any(Parcelable.Creator.class))).thenAnswer(i -> {
            if (parcel.readInt() != 0) {
                return i.<Parcelable.Creator>getArgument(0).createFromParcel(parcel);
            } else {
                return null;
            }
        });
        when(parcel.marshall()).thenAnswer(i -> {
            byte[] bytes = new byte[count];
            System.arraycopy(buf, 0, bytes, 0, count);
            return bytes;
        });
        when(parcel.dataPosition()).thenAnswer(i -> dataPosition);
        when(parcel.dataSize()).thenAnswer(i -> count);
        when(parcel.dataAvail()).thenAnswer(i -> count - dataPosition);


        doAnswer(i -> {
            if (i.<Integer>getArgument(0) < 0) throw new IllegalArgumentException();
            dataPosition = i.getArgument(0);
            return null;
        }).when(parcel).setDataPosition(anyInt());
        doAnswer(i -> {
            count = 0;
            return null;
        }).when(parcel).recycle();
    }

    private class BytesInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            return (dataPosition < count) ? (buf[dataPosition++] & 0xff) : -1;
        }

        public synchronized int read(byte b[], int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }

            if (dataPosition >= count) {
                return -1;
            }

            int avail = count - dataPosition;
            if (len > avail) {
                len = avail;
            }
            if (len <= 0) {
                return 0;
            }
            System.arraycopy(buf, dataPosition, b, off, len);
            dataPosition += len;
            return len;
        }
    }

    private class BytesOutputStream extends OutputStream {
        private void grow(int minCapacity) {
            int oldCapacity = buf.length;
            int newCapacity = oldCapacity << 1;
            if (newCapacity - minCapacity < 0)
                newCapacity = minCapacity;
            buf = Arrays.copyOf(buf, newCapacity);
        }

        private void ensureCapacity(int minCapacity) {
            if (minCapacity - buf.length > 0) grow(minCapacity);
        }

        public synchronized void write(int b) {
            ensureCapacity(dataPosition + 1);
            buf[dataPosition] = (byte) b;
            dataPosition += 1;
            if (dataPosition > count) count = dataPosition;
        }

        public synchronized void write(byte b[], int off, int len) {
            if ((off < 0) || (off > b.length) || (len < 0) ||
                    ((off + len) - b.length > 0)) {
                throw new IndexOutOfBoundsException();
            }
            ensureCapacity(dataPosition + len);
            System.arraycopy(b, off, buf, dataPosition, len);
            dataPosition += len;
            if (dataPosition > count) count = dataPosition;
        }
    }
}
