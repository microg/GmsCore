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
import com.google.android.gms.common.internal.safeparcel.SafeParcelReader;
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import org.microg.gms.common.Hide;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Hide
public final class SafeParcelReflectionUtil {
    private static final String TAG = "SafeParcel";

    private SafeParcelReflectionUtil() {
    }

    @Deprecated
    public static <T extends AutoSafeParcelable> T createObject(Class<T> tClass, Parcel in) {
        ClassDescriptor<T> descriptor = new ClassDescriptor<>(tClass);
        return createObject(in, descriptor);
    }

    public static <T extends AutoSafeParcelable> T createObject(Parcel in, ClassDescriptor<T> descriptor) {
        try {
            Constructor<T> constructor = descriptor.constructor;
            T t = constructor.newInstance();
            readObject(t, in, descriptor);
            return t;
        } catch (Exception e) {
            throw new RuntimeException("Can't construct object", e);
        }
    }

    @Deprecated
    public static void writeObject(AutoSafeParcelable object, Parcel parcel, int flags) {
        if (object == null)
            throw new NullPointerException();
        Class<?> clazz = object.getClass();
        ClassDescriptor<?> descriptor = new ClassDescriptor<>(clazz);
        writeObject(object, parcel, flags, descriptor);
    }

    public static <T extends AutoSafeParcelable> void writeObject(T object, Parcel parcel, int flags, ClassDescriptor<?> descriptor) {
        int start = SafeParcelWriter.writeObjectHeader(parcel);
        for (ClassDescriptor.FieldDescriptor fieldDescriptor : descriptor.fields.values()) {
            try {
                writeField(object, parcel, flags, fieldDescriptor);
            } catch (Exception e) {
                Log.w(TAG, "Error writing field: " + e);
            }
        }
        SafeParcelWriter.finishObjectHeader(parcel, start);
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public static <T extends AutoSafeParcelable> void readObject(T object, Parcel parcel) {
        if (object == null)
            throw new NullPointerException();
        Class<T> clazz = (Class<T>) object.getClass();
        ClassDescriptor<T> descriptor = new ClassDescriptor<>(clazz);
        readObject(object, parcel, descriptor);
    }

    public static <T extends AutoSafeParcelable> void readObject(T object, Parcel parcel, ClassDescriptor<T> descriptor) {
        if (object == null)
            throw new NullPointerException();
        int end = SafeParcelReader.readObjectHeader(parcel);
        while (parcel.dataPosition() < end) {
            int header = SafeParcelReader.readHeader(parcel);
            int fieldId = SafeParcelReader.getFieldId(header);
            ClassDescriptor.FieldDescriptor fieldDescriptor = descriptor.fields.get(fieldId);
            if (fieldDescriptor == null) {
                Log.d(TAG, String.format("Unknown field id %d in %s, skipping.", fieldId, descriptor.tClass.getName()));
                SafeParcelReader.skip(parcel, header);
            } else {
                try {
                    readField(object, parcel, header, fieldDescriptor);
                } catch (Exception e) {
                    Log.w(TAG, String.format("Error reading field: %d of type %s in %s, skipping.", fieldId, fieldDescriptor.type, descriptor.tClass.getName()), e);
                    SafeParcelReader.skip(parcel, header);
                }
            }
        }
        if (parcel.dataPosition() > end) {
            throw new RuntimeException("Overread allowed size end=" + end);
        }
    }

    @SuppressWarnings("unchecked")
    private static Parcelable.Creator<Parcelable> getCreator(Field field) {
        Class<?> clazz = field.getType();
        if (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }
        if (clazz != null && Parcelable.class.isAssignableFrom(clazz)) {
            return getCreator((Class<? extends Parcelable>) clazz);
        }
        throw new RuntimeException(clazz + " is not an Parcelable");
    }

    @SuppressWarnings("unchecked")
    public static Parcelable.Creator<Parcelable> getCreator(Class<? extends Parcelable> clazz) {
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
    private static Class<?> getSubClass(Field field) {
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
    private static Class<?> getListItemClass(Field field) {
        Class<?> subClass = getSubClass(field);
        if (subClass != null || field.isAnnotationPresent(SafeParceled.class)) return subClass;
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getActualTypeArguments().length >= 1) {
                Type t = pt.getActualTypeArguments()[0];
                if (t instanceof Class) return (Class<?>) t;
            }
        }
        return null;
    }

    private static ClassLoader getClassLoader(Class<?> clazz) {
        return clazz == null || clazz.getClassLoader() == null ? ClassLoader.getSystemClassLoader() : clazz.getClassLoader();
    }

    @SuppressWarnings("deprecation")
    private static boolean isSafeParceledField(Field field) {
        return field.isAnnotationPresent(SafeParceled.class) || field.isAnnotationPresent(SafeParcelable.Field.class);
    }

    @SuppressWarnings("unchecked")
    private static void writeField(AutoSafeParcelable object, Parcel parcel, int flags, ClassDescriptor.FieldDescriptor descriptor)
            throws IllegalAccessException {
        switch (descriptor.type) {
            case Parcelable:
                SafeParcelWriter.write(parcel, descriptor.id, (Parcelable) descriptor.field.get(object), flags, descriptor.mayNull);
                break;
            case Binder:
                SafeParcelWriter.write(parcel, descriptor.id, (IBinder) descriptor.field.get(object), descriptor.mayNull);
                break;
            case Interface:
                IInterface iInterface = ((IInterface) descriptor.field.get(object));
                IBinder iBinder = iInterface != null ? iInterface.asBinder() : null;
                SafeParcelWriter.write(parcel, descriptor.id, iBinder, descriptor.mayNull);
                break;
            case StringList:
                SafeParcelWriter.writeStringList(parcel, descriptor.id, ((List<String>) descriptor.field.get(object)), descriptor.mayNull);
                break;
            case IntegerList:
                SafeParcelWriter.writeIntegerList(parcel, descriptor.id, ((List<Integer>) descriptor.field.get(object)), descriptor.mayNull);
                break;
            case BooleanList:
                SafeParcelWriter.writeBooleanList(parcel, descriptor.id, ((List<Boolean>) descriptor.field.get(object)), descriptor.mayNull);
                break;
            case LongList:
                SafeParcelWriter.writeLongList(parcel, descriptor.id, ((List<Long>) descriptor.field.get(object)), descriptor.mayNull);
                break;
            case FloatList:
                SafeParcelWriter.writeFloatList(parcel, descriptor.id, ((List<Float>) descriptor.field.get(object)), descriptor.mayNull);
                break;
            case DoubleList:
                SafeParcelWriter.writeDoubleList(parcel, descriptor.id, ((List<Double>) descriptor.field.get(object)), descriptor.mayNull);
                break;
            case List: {
                Class<?> clazz = descriptor.listItemClass;
                if (clazz == null || !Parcelable.class.isAssignableFrom(clazz) || descriptor.useValueParcel) {
                    SafeParcelWriter.write(parcel, descriptor.id, (List<?>) descriptor.field.get(object), descriptor.mayNull);
                } else {
                    SafeParcelWriter.write(parcel, descriptor.id, (List<Parcelable>) descriptor.field.get(object), flags, descriptor.mayNull);
                }
                break;
            }
            case Map:
                SafeParcelWriter.write(parcel, descriptor.id, (Map) descriptor.field.get(object), descriptor.mayNull);
                break;
            case Bundle:
                SafeParcelWriter.write(parcel, descriptor.id, (Bundle) descriptor.field.get(object), descriptor.mayNull);
                break;
            case ParcelableArray:
                SafeParcelWriter.write(parcel, descriptor.id, (Parcelable[]) descriptor.field.get(object), flags, descriptor.mayNull);
                break;
            case StringArray:
                SafeParcelWriter.write(parcel, descriptor.id, (String[]) descriptor.field.get(object), descriptor.mayNull);
                break;
            case ByteArray:
                SafeParcelWriter.write(parcel, descriptor.id, (byte[]) descriptor.field.get(object), descriptor.mayNull);
                break;
            case ByteArrayArray:
                SafeParcelWriter.write(parcel, descriptor.id, (byte[][]) descriptor.field.get(object), descriptor.mayNull);
                break;
            case FloatArray:
                SafeParcelWriter.write(parcel, descriptor.id, (float[]) descriptor.field.get(object), descriptor.mayNull);
                break;
            case IntArray:
                SafeParcelWriter.write(parcel, descriptor.id, (int[]) descriptor.field.get(object), descriptor.mayNull);
                break;
            case Integer:
                SafeParcelWriter.write(parcel, descriptor.id, (Integer) descriptor.field.get(object));
                break;
            case Long:
                SafeParcelWriter.write(parcel, descriptor.id, (Long) descriptor.field.get(object));
                break;
            case Short:
                SafeParcelWriter.write(parcel, descriptor.id, (Short) descriptor.field.get(object));
                break;
            case Boolean:
                SafeParcelWriter.write(parcel, descriptor.id, (Boolean) descriptor.field.get(object));
                break;
            case Float:
                SafeParcelWriter.write(parcel, descriptor.id, (Float) descriptor.field.get(object));
                break;
            case Double:
                SafeParcelWriter.write(parcel, descriptor.id, (Double) descriptor.field.get(object));
                break;
            case String:
                SafeParcelWriter.write(parcel, descriptor.id, (String) descriptor.field.get(object), descriptor.mayNull);
                break;
            case Byte:
                SafeParcelWriter.write(parcel, descriptor.id, (Byte) descriptor.field.get(object));
                break;
        }
    }

    private static void readField(AutoSafeParcelable object, Parcel parcel, int header, ClassDescriptor.FieldDescriptor descriptor)
            throws IllegalAccessException {
        switch (descriptor.type) {
            case Parcelable:
                descriptor.field.set(object, SafeParcelReader.readParcelable(parcel, header, descriptor.creator));
                break;
            case Binder:
                descriptor.field.set(object, SafeParcelReader.readBinder(parcel, header));
                break;
            case Interface: {
                boolean hasStub = false;
                for (Class<?> aClass : descriptor.field.getType().getDeclaredClasses()) {
                    try {
                        descriptor.field.set(object, aClass.getDeclaredMethod("asInterface", IBinder.class)
                                .invoke(null, SafeParcelReader.readBinder(parcel, header)));
                        hasStub = true;
                        break;
                    } catch (Exception ignored) {
                    }
                }
                if (!hasStub) throw new RuntimeException("Field has broken interface: " + descriptor.field);
                break;
            }
            case StringList:
                descriptor.field.set(object, SafeParcelReader.readStringList(parcel, header));
                break;
            case IntegerList:
                descriptor.field.set(object, SafeParcelReader.readIntegerList(parcel, header));
                break;
            case BooleanList:
                descriptor.field.set(object, SafeParcelReader.readBooleanList(parcel, header));
                break;
            case LongList:
                descriptor.field.set(object, SafeParcelReader.readLongList(parcel, header));
                break;
            case FloatList:
                descriptor.field.set(object, SafeParcelReader.readFloatList(parcel, header));
                break;
            case DoubleList:
                descriptor.field.set(object, SafeParcelReader.readDoubleList(parcel, header));
                break;
            case List: {
                Class<?> clazz = descriptor.listItemClass;
                Object val;
                if (clazz == null || !Parcelable.class.isAssignableFrom(clazz) || descriptor.useValueParcel) {
                    val = SafeParcelReader.readList(parcel, header, getClassLoader(clazz));
                } else {
                    val = SafeParcelReader.readParcelableList(parcel, header, descriptor.creator);
                }
                descriptor.field.set(object, val);
                break;
            }
            case Map: {
                Class<?> clazz = descriptor.subClass;
                Object val = SafeParcelReader.readMap(parcel, header, getClassLoader(clazz));
                descriptor.field.set(object, val);
                break;
            }
            case Bundle: {
                Class<?> clazz = descriptor.subClass;
                Object val;
                if (clazz == null || !Parcelable.class.isAssignableFrom(clazz) || descriptor.useValueParcel /* should not happen on Bundles */) {
                    val = SafeParcelReader.readBundle(parcel, header, getClassLoader(descriptor.field.getDeclaringClass()));
                } else {
                    val = SafeParcelReader.readBundle(parcel, header, getClassLoader(clazz));
                }
                descriptor.field.set(object, val);
                break;
            }
            case ParcelableArray:
                descriptor.field.set(object, SafeParcelReader.readParcelableArray(parcel, header, descriptor.creator));
                break;
            case StringArray:
                descriptor.field.set(object, SafeParcelReader.readStringArray(parcel, header));
                break;
            case ByteArray:
                descriptor.field.set(object, SafeParcelReader.readByteArray(parcel, header));
                break;
            case ByteArrayArray:
                descriptor.field.set(object, SafeParcelReader.readByteArrayArray(parcel, header));
                break;
            case FloatArray:
                descriptor.field.set(object, SafeParcelReader.readFloatArray(parcel, header));
                break;
            case IntArray:
                descriptor.field.set(object, SafeParcelReader.readIntArray(parcel, header));
                break;
            case Integer: {
                int i = SafeParcelReader.readInt(parcel, header);
                if (descriptor.versionCode != -1 && i > descriptor.versionCode) {
                    Log.d(TAG, String.format("Version code of %s (%d) is older than object read (%d).", descriptor.field.getDeclaringClass().getName(), descriptor.versionCode, i));
                }
                descriptor.field.set(object, i);
                break;
            }
            case Long: {
                long l = SafeParcelReader.readLong(parcel, header);
                if (descriptor.versionCode != -1 && l > descriptor.versionCode) {
                    Log.d(TAG, String.format("Version code of %s (%d) is older than object read (%d).", descriptor.field.getDeclaringClass().getName(), descriptor.versionCode, l));
                }
                descriptor.field.set(object, l);
                break;
            }
            case Short: {
                short i = SafeParcelReader.readShort(parcel, header);
                if (descriptor.versionCode != -1 && i > descriptor.versionCode) {
                    Log.d(TAG, String.format("Version code of %s (%d) is older than object read (%d).", descriptor.field.getDeclaringClass().getName(), descriptor.versionCode, i));
                }
                descriptor.field.set(object, i);
                break;
            }
            case Boolean:
                descriptor.field.set(object, SafeParcelReader.readBool(parcel, header));
                break;
            case Float:
                descriptor.field.set(object, SafeParcelReader.readFloat(parcel, header));
                break;
            case Double:
                descriptor.field.set(object, SafeParcelReader.readDouble(parcel, header));
                break;
            case String:
                descriptor.field.set(object, SafeParcelReader.readString(parcel, header));
                break;
            case Byte:
                descriptor.field.set(object, SafeParcelReader.readByte(parcel, header));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + descriptor.type);
        }
    }

    private enum SafeParcelType {
        Parcelable, Binder, Interface, Bundle,
        StringList, IntegerList, BooleanList, LongList, FloatList, DoubleList, List, Map,
        ParcelableArray, StringArray, ByteArray, ByteArrayArray, FloatArray, IntArray,
        Integer, Long, Short, Boolean, Float, Double, String, Byte;
    }

    public static class ClassDescriptor<T> {
        Class<T> tClass;
        Constructor<T> constructor;
        Map<Integer, FieldDescriptor> fields = new HashMap<>();

        public ClassDescriptor(Class<T> tClass) {
            this.tClass = tClass;
            try {
                constructor = tClass.getDeclaredConstructor();
                constructor.setAccessible(true);
            } catch (Exception e) {
                Log.w(TAG, tClass + " has no default constructor");
            }
            Class<?> clazz = tClass;
            while (clazz != null) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (isSafeParceledField(field)) {
                        FieldDescriptor fieldDescriptor = new FieldDescriptor(field);
                        fields.put(fieldDescriptor.id, fieldDescriptor);
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }

        public static class FieldDescriptor {
            Field field;
            int id;
            boolean mayNull;
            SafeParcelable.Field annotation;
            SafeParceled legacyAnnotation;
            SafeParcelType type;
            Parcelable.Creator<? extends Parcelable> creator;
            long versionCode = -1;
            Class<?> listItemClass;
            boolean useValueParcel;
            Class<?> subClass;

            public FieldDescriptor(Field field) {
                this.field = field;
                field.setAccessible(true);
                try {
                    Field accessFlagsField = Field.class.getDeclaredField("accessFlags");
                    accessFlagsField.setAccessible(true);
                    accessFlagsField.setInt(field, accessFlagsField.getInt(field) & ~Modifier.FINAL);
                } catch (Exception e) {
                    // Ignored
                }
                this.annotation = field.getAnnotation(SafeParcelable.Field.class);
                this.legacyAnnotation = field.getAnnotation(SafeParceled.class);
                if (annotation != null) {
                    this.id = annotation.value();
                    this.mayNull = annotation.mayNull();
                    this.useValueParcel = annotation.useValueParcel();
                    this.versionCode = annotation.versionCode();
                } else if (legacyAnnotation != null) {
                    this.id = legacyAnnotation.value();
                    this.mayNull = legacyAnnotation.mayNull();
                    this.useValueParcel = legacyAnnotation.useClassLoader();
                } else {
                    throw new IllegalArgumentException();
                }
                this.type = getType();
                switch (type) {
                    case Parcelable:
                    case ParcelableArray:
                        creator = getCreator(field);
                        break;
                    case List:
                        if (listItemClass != null && Parcelable.class.isAssignableFrom(listItemClass)) {
                            if (!this.useValueParcel) {
                                creator = getCreator((Class<? extends Parcelable>) listItemClass);
                            }
                        }
                        break;
                    case Map:
                    case Bundle:
                        subClass = getSubClass(field);
                        break;
                }
            }

            private SafeParcelType getType() {
                Class<?> clazz = field.getType();
                Class<?> component = clazz.getComponentType();
                if (clazz.isArray() && component != null) {
                    if (Parcelable.class.isAssignableFrom(component)) return SafeParcelType.ParcelableArray;
                    if (String.class.isAssignableFrom(component)) return SafeParcelType.StringArray;
                    if (byte.class.isAssignableFrom(component)) return SafeParcelType.ByteArray;
                    if (byte[].class.isAssignableFrom(component)) return SafeParcelType.ByteArrayArray;
                    if (float.class.isAssignableFrom(component)) return SafeParcelType.FloatArray;
                    if (int.class.isAssignableFrom(component)) return SafeParcelType.IntArray;
                }
                if (Bundle.class.isAssignableFrom(clazz))
                    return SafeParcelType.Bundle;
                if (Parcelable.class.isAssignableFrom(clazz))
                    return SafeParcelType.Parcelable;
                if (IBinder.class.isAssignableFrom(clazz))
                    return SafeParcelType.Binder;
                if (IInterface.class.isAssignableFrom(clazz))
                    return SafeParcelType.Interface;
                if (clazz == List.class || clazz == ArrayList.class) {
                    listItemClass = getListItemClass(field);
                    if (listItemClass == String.class && !useValueParcel) return SafeParcelType.StringList;
                    if (listItemClass == Integer.class && annotation.useDirectList()) return SafeParcelType.IntegerList;
                    if (listItemClass == Boolean.class && annotation.useDirectList()) return SafeParcelType.BooleanList;
                    if (listItemClass == Long.class && annotation.useDirectList()) return SafeParcelType.LongList;
                    if (listItemClass == Float.class && annotation.useDirectList()) return SafeParcelType.FloatList;
                    if (listItemClass == Double.class && annotation.useDirectList()) return SafeParcelType.DoubleList;
                    return SafeParcelType.List;
                }
                if (clazz == Map.class || clazz == HashMap.class)
                    return SafeParcelType.Map;
                if (clazz == int.class || clazz == Integer.class)
                    return SafeParcelType.Integer;
                if (clazz == short.class || clazz == Short.class)
                    return SafeParcelType.Short;
                if (clazz == boolean.class || clazz == Boolean.class)
                    return SafeParcelType.Boolean;
                if (clazz == long.class || clazz == Long.class)
                    return SafeParcelType.Long;
                if (clazz == float.class || clazz == Float.class)
                    return SafeParcelType.Float;
                if (clazz == double.class || clazz == Double.class)
                    return SafeParcelType.Double;
                if (clazz == byte.class || clazz == Byte.class)
                    return SafeParcelType.Byte;
                if (clazz == java.lang.String.class)
                    return SafeParcelType.String;
                throw new RuntimeException("Type is not yet usable with SafeParcelReflectionUtil: " + clazz);
            }
        }
    }
}
