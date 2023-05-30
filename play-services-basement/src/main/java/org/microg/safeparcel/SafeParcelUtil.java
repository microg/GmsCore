/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.safeparcel;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SafeParcelUtil {
    private static final String TAG = "SafeParcel";

    private SafeParcelUtil() {
    }

    public static <T extends SafeParcelable> T createObject(Class<T> tClass, Parcel in) {
        try {
            Constructor<T> constructor = tClass.getDeclaredConstructor();
            boolean acc = constructor.isAccessible();
            constructor.setAccessible(true);
            T t = constructor.newInstance();
            readObject(t, in);
            constructor.setAccessible(acc);
            return t;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("createObject() requires a default constructor");
        } catch (Exception e) {
            throw new RuntimeException("Can't construct object", e);
        }
    }

    public static void writeObject(SafeParcelable object, Parcel parcel, int flags) {
        if (object == null)
            throw new NullPointerException();
        Class clazz = object.getClass();
        int start = SafeParcelWriter.writeObjectHeader(parcel);
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (isSafeParceledField(field)) {
                    try {
                        writeField(object, parcel, field, flags);
                    } catch (Exception e) {
                        Log.w(TAG, "Error writing field: " + e);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        SafeParcelWriter.finishObjectHeader(parcel, start);
    }

    public static void readObject(SafeParcelable object, Parcel parcel) {
        if (object == null)
            throw new NullPointerException();
        Class clazz = object.getClass();
        SparseArray<Field> fieldMap = new SparseArray<>();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (isSafeParceledField(field)) {
                    int fieldId = getFieldId(field);
                    if (fieldMap.get(fieldId) != null) {
                        throw new RuntimeException(String.format("Field number %d is used twice in %s for fields %s and %s", fieldId, clazz.getName(), field.getName(), fieldMap.get(fieldId).getName()));
                    }
                    fieldMap.put(fieldId, field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        clazz = object.getClass();
        int end = SafeParcelReader.readObjectHeader(parcel);
        while (parcel.dataPosition() < end) {
            int header = SafeParcelReader.readHeader(parcel);
            int fieldId = SafeParcelReader.getFieldId(header);
            Field field = fieldMap.get(fieldId);
            if (field == null) {
                Log.d(TAG, String.format("Unknown field id %d in %s, skipping.", fieldId, clazz.getName()));
                SafeParcelReader.skip(parcel, header);
            } else {
                try {
                    readField(object, parcel, field, header);
                } catch (Exception e) {
                    Log.w(TAG, String.format("Error reading field: %d in %s, skipping.", fieldId, clazz.getName()), e);
                    SafeParcelReader.skip(parcel, header);
                }
            }
        }
        if (parcel.dataPosition() > end) {
            throw new RuntimeException("Overread allowed size end=" + end);
        }
    }

    private static Parcelable.Creator<Parcelable> getCreator(Field field) {
        Class clazz = field.getType();
        if (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }
        if (clazz != null && Parcelable.class.isAssignableFrom(clazz)) {
            return getCreator(clazz);
        }
        throw new RuntimeException(clazz + " is not an Parcelable");
    }

    private static Parcelable.Creator<Parcelable> getCreator(Class clazz) {
        try {
            Field creatorField = clazz.getDeclaredField("CREATOR");
            creatorField.setAccessible(true);
            return (Parcelable.Creator<Parcelable>) creatorField.get(null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(clazz + " is an Parcelable without CREATOR");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("CREATOR in " + clazz + " is not accessible");
        }
    }

    @SuppressWarnings("deprecation")
    private static Class getSubClass(Field field) {
        SafeParceled safeParceled = field.getAnnotation(SafeParceled.class);
        SafeParcelable.Field safeParcelableField = field.getAnnotation(SafeParcelable.Field.class);
        if (safeParceled != null && safeParceled.subClass() != SafeParceled.class) {
            return safeParceled.subClass();
        } else if (safeParceled != null && !"undefined".equals(safeParceled.subType())) {
            try {
                return Class.forName(safeParceled.subType());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        } else if (safeParcelableField != null && safeParcelableField.subClass() != SafeParcelable.class) {
            return safeParcelableField.subClass();
        } else {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static Class getListItemClass(Field field) {
        Class subClass = getSubClass(field);
        if (subClass != null || field.isAnnotationPresent(SafeParceled.class)) return subClass;
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getActualTypeArguments().length >= 1) {
                Type t = pt.getActualTypeArguments()[0];
                if (t instanceof Class) return (Class) t;
            }
        }
        return null;
    }

    private static ClassLoader getClassLoader(Class clazz) {
        return clazz == null || clazz.getClassLoader() == null ? ClassLoader.getSystemClassLoader() : clazz.getClassLoader();
    }

    @SuppressWarnings("deprecation")
    private static boolean useValueParcel(Field field) {
        SafeParceled safeParceled = field.getAnnotation(SafeParceled.class);
        SafeParcelable.Field safeParcelableField = field.getAnnotation(SafeParcelable.Field.class);
        if (safeParceled != null) {
            return safeParceled.useClassLoader();
        } else if (safeParcelableField != null) {
            return safeParcelableField.useValueParcel();
        } else {
            throw new IllegalStateException();
        }
    }

    @SuppressWarnings("deprecation")
    private static int getFieldId(Field field) {
        SafeParceled safeParceled = field.getAnnotation(SafeParceled.class);
        SafeParcelable.Field safeParcelableField = field.getAnnotation(SafeParcelable.Field.class);
        if (safeParceled != null) {
            return safeParceled.value();
        } else if (safeParcelableField != null) {
            return safeParcelableField.value();
        } else {
            throw new IllegalStateException();
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean getMayNull(Field field) {
        SafeParceled safeParceled = field.getAnnotation(SafeParceled.class);
        SafeParcelable.Field safeParcelableField = field.getAnnotation(SafeParcelable.Field.class);
        if (safeParceled != null) {
            return safeParceled.mayNull();
        } else if (safeParcelableField != null) {
            return safeParcelableField.mayNull();
        } else {
            throw new IllegalStateException();
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean isSafeParceledField(Field field) {
        return field.isAnnotationPresent(SafeParceled.class) || field.isAnnotationPresent(SafeParcelable.Field.class);
    }

    private static boolean useDirectList(Field field) {
        SafeParcelable.Field safeParcelableField = field.getAnnotation(SafeParcelable.Field.class);
        if (safeParcelableField != null) return safeParcelableField.useDirectList();
        return false;
    }

    private static void writeField(SafeParcelable object, Parcel parcel, Field field, int flags)
            throws IllegalAccessException {
        int fieldId = getFieldId(field);
        boolean mayNull = getMayNull(field);
        boolean acc = field.isAccessible();
        field.setAccessible(true);
        switch (SafeParcelType.fromField(field)) {
            case Parcelable:
                SafeParcelWriter.write(parcel, fieldId, (Parcelable) field.get(object), flags, mayNull);
                break;
            case Binder:
                SafeParcelWriter.write(parcel, fieldId, (IBinder) field.get(object), mayNull);
                break;
            case Interface:
                SafeParcelWriter.write(parcel, fieldId, ((IInterface) field.get(object)).asBinder(), mayNull);
                break;
            case StringList:
                SafeParcelWriter.writeStringList(parcel, fieldId, ((List<String>) field.get(object)), mayNull);
                break;
            case IntegerList:
                SafeParcelWriter.writeIntegerList(parcel, fieldId, ((List<Integer>) field.get(object)), mayNull);
                break;
            case BooleanList:
                SafeParcelWriter.writeBooleanList(parcel, fieldId, ((List<Boolean>) field.get(object)), mayNull);
                break;
            case LongList:
                SafeParcelWriter.writeLongList(parcel, fieldId, ((List<Long>) field.get(object)), mayNull);
                break;
            case FloatList:
                SafeParcelWriter.writeFloatList(parcel, fieldId, ((List<Float>) field.get(object)), mayNull);
                break;
            case DoubleList:
                SafeParcelWriter.writeDoubleList(parcel, fieldId, ((List<Double>) field.get(object)), mayNull);
                break;
            case List: {
                Class clazz = getListItemClass(field);
                if (clazz == null || !Parcelable.class.isAssignableFrom(clazz) || useValueParcel(field)) {
                    SafeParcelWriter.write(parcel, fieldId, (List) field.get(object), mayNull);
                } else {
                    SafeParcelWriter.write(parcel, fieldId, (List) field.get(object), flags, mayNull);
                }
                break;
            }
            case Map:
                SafeParcelWriter.write(parcel, fieldId, (Map) field.get(object), mayNull);
                break;
            case Bundle:
                SafeParcelWriter.write(parcel, fieldId, (Bundle) field.get(object), mayNull);
                break;
            case ParcelableArray:
                SafeParcelWriter.write(parcel, fieldId, (Parcelable[]) field.get(object), flags, mayNull);
                break;
            case StringArray:
                SafeParcelWriter.write(parcel, fieldId, (String[]) field.get(object), mayNull);
                break;
            case ByteArray:
                SafeParcelWriter.write(parcel, fieldId, (byte[]) field.get(object), mayNull);
                break;
            case ByteArrayArray:
                SafeParcelWriter.write(parcel, fieldId, (byte[][]) field.get(object), mayNull);
                break;
            case FloatArray:
                SafeParcelWriter.write(parcel, fieldId, (float[]) field.get(object), mayNull);
                break;
            case IntArray:
                SafeParcelWriter.write(parcel, fieldId, (int[]) field.get(object), mayNull);
                break;
            case Integer:
                SafeParcelWriter.write(parcel, fieldId, (Integer) field.get(object));
                break;
            case Long:
                SafeParcelWriter.write(parcel, fieldId, (Long) field.get(object));
                break;
            case Boolean:
                SafeParcelWriter.write(parcel, fieldId, (Boolean) field.get(object));
                break;
            case Float:
                SafeParcelWriter.write(parcel, fieldId, (Float) field.get(object));
                break;
            case Double:
                SafeParcelWriter.write(parcel, fieldId, (Double) field.get(object));
                break;
            case String:
                SafeParcelWriter.write(parcel, fieldId, (String) field.get(object), mayNull);
                break;
            case Byte:
                SafeParcelWriter.write(parcel, fieldId, (Byte) field.get(object));
                break;
        }
        field.setAccessible(acc);
    }

    private static void readField(SafeParcelable object, Parcel parcel, Field field, int header)
            throws IllegalAccessException {
        boolean acc = field.isAccessible();
        field.setAccessible(true);
        long versionCode = -1;
        if (field.isAnnotationPresent(SafeParcelable.Field.class)) {
            versionCode = field.getAnnotation(SafeParcelable.Field.class).versionCode();
        }
        switch (SafeParcelType.fromField(field)) {
            case Parcelable:
                field.set(object, SafeParcelReader.readParcelable(parcel, header, getCreator(field)));
                break;
            case Binder:
                field.set(object, SafeParcelReader.readBinder(parcel, header));
                break;
            case Interface: {
                boolean hasStub = false;
                for (Class<?> aClass : field.getType().getDeclaredClasses()) {
                    try {
                        field.set(object, aClass.getDeclaredMethod("asInterface", IBinder.class)
                                .invoke(null, SafeParcelReader.readBinder(parcel, header)));
                        hasStub = true;
                        break;
                    } catch (Exception ignored) {
                    }
                }
                if (!hasStub) throw new RuntimeException("Field has broken interface: " + field);
                break;
            }
            case StringList:
                field.set(object, SafeParcelReader.readStringList(parcel, header));
                break;
            case IntegerList:
                field.set(object, SafeParcelReader.readIntegerList(parcel, header));
                break;
            case BooleanList:
                field.set(object, SafeParcelReader.readBooleanList(parcel, header));
                break;
            case LongList:
                field.set(object, SafeParcelReader.readLongList(parcel, header));
                break;
            case FloatList:
                field.set(object, SafeParcelReader.readFloatList(parcel, header));
                break;
            case DoubleList:
                field.set(object, SafeParcelReader.readDoubleList(parcel, header));
                break;
            case List: {
                Class clazz = getListItemClass(field);
                Object val;
                if (clazz == null || !Parcelable.class.isAssignableFrom(clazz) || useValueParcel(field)) {
                    val = SafeParcelReader.readList(parcel, header, getClassLoader(clazz));
                } else {
                    val = SafeParcelReader.readParcelableList(parcel, header, getCreator(clazz));
                }
                field.set(object, val);
                break;
            }
            case Map: {
                Class clazz = getSubClass(field);
                Object val = SafeParcelReader.readMap(parcel, header, getClassLoader(clazz));
                field.set(object, val);
                break;
            }
            case Bundle: {
                Class clazz = getSubClass(field);
                Object val;
                if (clazz == null || !Parcelable.class.isAssignableFrom(clazz) || useValueParcel(field) /* should not happen on Bundles */) {
                    val = SafeParcelReader.readBundle(parcel, header, getClassLoader(field.getDeclaringClass()));
                } else {
                    val = SafeParcelReader.readBundle(parcel, header, getClassLoader(clazz));
                }
                field.set(object, val);
                break;
            }
            case ParcelableArray:
                field.set(object, SafeParcelReader.readParcelableArray(parcel, header, getCreator(field)));
                break;
            case StringArray:
                field.set(object, SafeParcelReader.readStringArray(parcel, header));
                break;
            case ByteArray:
                field.set(object, SafeParcelReader.readByteArray(parcel, header));
                break;
            case ByteArrayArray:
                field.set(object, SafeParcelReader.readByteArrayArray(parcel, header));
                break;
            case FloatArray:
                field.set(object, SafeParcelReader.readFloatArray(parcel, header));
                break;
            case IntArray:
                field.set(object, SafeParcelReader.readIntArray(parcel, header));
                break;
            case Integer: {
                int i = SafeParcelReader.readInt(parcel, header);
                if (versionCode != -1 && i > versionCode) {
                    Log.d(TAG, String.format("Version code of %s (%d) is older than object read (%d).", field.getDeclaringClass().getName(), versionCode, i));
                }
                field.set(object, i);
                break;
            }
            case Long: {
                long l = SafeParcelReader.readLong(parcel, header);
                if (versionCode != -1 && l > versionCode) {
                    Log.d(TAG, String.format("Version code of %s (%d) is older than object read (%d).", field.getDeclaringClass().getName(), versionCode, l));
                }
                field.set(object, l);
                break;
            }
            case Boolean:
                field.set(object, SafeParcelReader.readBool(parcel, header));
                break;
            case Float:
                field.set(object, SafeParcelReader.readFloat(parcel, header));
                break;
            case Double:
                field.set(object, SafeParcelReader.readDouble(parcel, header));
                break;
            case String:
                field.set(object, SafeParcelReader.readString(parcel, header));
                break;
            case Byte:
                field.set(object, SafeParcelReader.readByte(parcel, header));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + SafeParcelType.fromField(field));
        }
        field.setAccessible(acc);
    }

    private enum SafeParcelType {
        Parcelable, Binder, Interface, Bundle,
        StringList, IntegerList, BooleanList, LongList, FloatList, DoubleList, List, Map,
        ParcelableArray, StringArray, ByteArray, ByteArrayArray, FloatArray, IntArray,
        Integer, Long, Boolean, Float, Double, String, Byte;

        public static SafeParcelType fromField(Field field) {
            Class clazz = field.getType();
            Class component = clazz.getComponentType();
            if (clazz.isArray() && component != null) {
                if (Parcelable.class.isAssignableFrom(component)) return ParcelableArray;
                if (String.class.isAssignableFrom(component)) return StringArray;
                if (byte.class.isAssignableFrom(component)) return ByteArray;
                if (byte[].class.isAssignableFrom(component)) return ByteArrayArray;
                if (float.class.isAssignableFrom(component)) return FloatArray;
                if (int.class.isAssignableFrom(component)) return IntArray;
            }
            if (Bundle.class.isAssignableFrom(clazz))
                return Bundle;
            if (Parcelable.class.isAssignableFrom(clazz))
                return Parcelable;
            if (IBinder.class.isAssignableFrom(clazz))
                return Binder;
            if (IInterface.class.isAssignableFrom(clazz))
                return Interface;
            if (clazz == List.class || clazz == ArrayList.class) {
                if (getListItemClass(field) == String.class && !useValueParcel(field)) return StringList;
                if (getListItemClass(field) == Integer.class && useDirectList(field)) return IntegerList;
                if (getListItemClass(field) == Boolean.class && useDirectList(field)) return BooleanList;
                if (getListItemClass(field) == Long.class && useDirectList(field)) return LongList;
                if (getListItemClass(field) == Float.class && useDirectList(field)) return FloatList;
                if (getListItemClass(field) == Double.class && useDirectList(field)) return DoubleList;
                return List;
            }
            if (clazz == Map.class || clazz == HashMap.class)
                return Map;
            if (clazz == int.class || clazz == Integer.class)
                return Integer;
            if (clazz == boolean.class || clazz == Boolean.class)
                return Boolean;
            if (clazz == long.class || clazz == Long.class)
                return Long;
            if (clazz == float.class || clazz == Float.class)
                return Float;
            if (clazz == double.class || clazz == Double.class)
                return Double;
            if (clazz == byte.class || clazz == Byte.class)
                return Byte;
            if (clazz == java.lang.String.class)
                return String;
            throw new RuntimeException("Type is not yet usable with SafeParcelUtil: " + clazz);
        }
    }

    public static <T extends Parcelable> byte[] asByteArray(T parcelable) {
        if (parcelable == null) return null;
        Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

    public static <T extends Parcelable> T fromByteArray(byte[] bytes, Parcelable.Creator<T> tCreator) {
        if (bytes == null) return null;
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        T parcelable = tCreator.createFromParcel(parcel);
        parcel.recycle();
        return parcelable;
    }
}
