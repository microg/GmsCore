/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.server.response;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelReader;
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public abstract class FastJsonResponse {
    public static final int TYPE_INTEGER = 0;
    public static final int TYPE_BIG_INTEGER = 1;
    public static final int TYPE_LONG = 2;
    public static final int TYPE_FLOAT = 3;
    public static final int TYPE_DOUBLE = 4;
    public static final int TYPE_BIG_DECIMAL = 5;
    public static final int TYPE_BOOLEAN = 6;
    public static final int TYPE_STRING = 7;
    public static final int TYPE_BASE64 = 8;
    public static final int TYPE_BASE64_URL_SAFE = 9;
    public static final int TYPE_STRING_MAP = 10;
    public static final int TYPE_CONCRETE = 11;

    public abstract Map<String, Field<?, ?>> getFieldMappings();

    protected abstract Object getFieldValue(Field<?, ?> field);

    protected abstract boolean isFieldSet(Field<?, ?> field);

    public final Object getValue(Field<?, ?> field) {
        return getFieldValue(field);
    }

    public final boolean hasValue(Field<?, ?> field) {
        return isFieldSet(field);
    }

    protected void setInteger(Field<?, ?> field, int value) { throw unsupported(field); }
    protected void setIntegerArray(Field<?, ?> field, List<Integer> value) { throw unsupported(field); }
    protected void setBigInteger(Field<?, ?> field, BigInteger value) { throw unsupported(field); }
    protected void setBigIntegerArray(Field<?, ?> field, List<BigInteger> value) { throw unsupported(field); }
    protected void setLong(Field<?, ?> field, long value) { throw unsupported(field); }
    protected void setLongArray(Field<?, ?> field, List<Long> value) { throw unsupported(field); }
    protected void setFloat(Field<?, ?> field, float value) { throw unsupported(field); }
    protected void setFloatArray(Field<?, ?> field, List<Float> value) { throw unsupported(field); }
    protected void setDouble(Field<?, ?> field, double value) { throw unsupported(field); }
    protected void setDoubleArray(Field<?, ?> field, List<Double> value) { throw unsupported(field); }
    protected void setBigDecimal(Field<?, ?> field, BigDecimal value) { throw unsupported(field); }
    protected void setBigDecimalArray(Field<?, ?> field, List<BigDecimal> value) { throw unsupported(field); }
    protected void setBoolean(Field<?, ?> field, boolean value) { throw unsupported(field); }
    protected void setBooleanArray(Field<?, ?> field, List<Boolean> value) { throw unsupported(field); }
    protected void setString(Field<?, ?> field, String value) { throw unsupported(field); }
    protected void setStringArray(Field<?, ?> field, List<String> value) { throw unsupported(field); }
    protected void setDecodedBytes(Field<?, ?> field, byte[] value) { throw unsupported(field); }
    protected void setStringMap(Field<?, ?> field, Map<String, String> value) { throw unsupported(field); }
    protected void addConcreteType(Field<?, ?> field, FastJsonResponse value) { throw unsupported(field); }
    protected void addConcreteTypeArray(Field<?, ?> field, List<? extends FastJsonResponse> value) { throw unsupported(field); }

    private UnsupportedOperationException unsupported(Field<?, ?> field) {
        return new UnsupportedOperationException("Field not supported: " + field.outputFieldName);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(100);
        result.append('{');
        boolean first = true;
        for (Map.Entry<String, Field<?, ?>> entry : getFieldMappings().entrySet()) {
            Field<?, ?> field = entry.getValue();
            if (!isFieldSet(field)) continue;
            if (!first) result.append(',');
            first = false;
            result.append(JSONObject.quote(entry.getKey())).append(':');
            appendJsonValue(result, field, getFieldValue(field));
        }
        return result.append('}').toString();
    }

    private static void appendJsonValue(StringBuilder out, Field<?, ?> field, Object value) {
        if (value == null) {
            out.append("null");
            return;
        }
        if (field.typeOutArray) {
            out.append('[');
            boolean first = true;
            for (Object item : (List<?>) value) {
                if (!first) out.append(',');
                first = false;
                appendScalarJsonValue(out, field.typeOut, item);
            }
            out.append(']');
            return;
        }
        appendScalarJsonValue(out, field.typeOut, value);
    }

    private static void appendScalarJsonValue(StringBuilder out, int type, Object value) {
        if (value == null) {
            out.append("null");
            return;
        }
        switch (type) {
            case TYPE_STRING:
                out.append(JSONObject.quote((String) value));
                break;
            case TYPE_BASE64:
                out.append(JSONObject.quote(Base64.encodeToString((byte[]) value, Base64.NO_WRAP)));
                break;
            case TYPE_BASE64_URL_SAFE:
                out.append(JSONObject.quote(Base64.encodeToString((byte[]) value,
                        Base64.NO_WRAP | Base64.URL_SAFE)));
                break;
            case TYPE_STRING_MAP:
                out.append(new JSONObject((Map<?, ?>) value));
                break;
            case TYPE_CONCRETE:
                out.append(((FastJsonResponse) value).toString());
                break;
            default:
                out.append(value);
        }
    }

    public static class Field<I, O> extends AbstractSafeParcelable {
        public final int versionCode;
        public final int typeIn;
        public final boolean typeInArray;
        public final int typeOut;
        public final boolean typeOutArray;
        public final String outputFieldName;
        public final int safeParcelFieldId;
        public final String concreteTypeName;
        public final java.lang.Class<? extends FastJsonResponse> concreteType;

        public Field(int versionCode, int typeIn, boolean typeInArray, int typeOut,
                     boolean typeOutArray, String outputFieldName, int safeParcelFieldId,
                     String concreteTypeName) {
            this.versionCode = versionCode;
            this.typeIn = typeIn;
            this.typeInArray = typeInArray;
            this.typeOut = typeOut;
            this.typeOutArray = typeOutArray;
            this.outputFieldName = outputFieldName;
            this.safeParcelFieldId = safeParcelFieldId;
            this.concreteTypeName = concreteTypeName;
            this.concreteType = null;
        }

        @SuppressWarnings("unchecked")
        public Field(int typeIn, boolean typeInArray, int typeOut, boolean typeOutArray,
                     String outputFieldName, int safeParcelFieldId, java.lang.Class<?> concreteType) {
            this.versionCode = 1;
            this.typeIn = typeIn;
            this.typeInArray = typeInArray;
            this.typeOut = typeOut;
            this.typeOutArray = typeOutArray;
            this.outputFieldName = outputFieldName;
            this.safeParcelFieldId = safeParcelFieldId;
            this.concreteType = (java.lang.Class<? extends FastJsonResponse>) concreteType;
            this.concreteTypeName = concreteType == null ? null : concreteType.getCanonicalName();
        }

        FastJsonResponse newConcreteTypeInstance() throws ReflectiveOperationException {
            if (concreteType == null) {
                throw new IllegalStateException("Concrete type unavailable for " + outputFieldName);
            }
            return concreteType.getDeclaredConstructor().newInstance();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            int header = SafeParcelWriter.writeObjectHeader(dest);
            SafeParcelWriter.write(dest, 1, versionCode);
            SafeParcelWriter.write(dest, 2, typeIn);
            SafeParcelWriter.write(dest, 3, typeInArray);
            SafeParcelWriter.write(dest, 4, typeOut);
            SafeParcelWriter.write(dest, 5, typeOutArray);
            SafeParcelWriter.write(dest, 6, outputFieldName, false);
            SafeParcelWriter.write(dest, 7, safeParcelFieldId);
            SafeParcelWriter.write(dest, 8, concreteTypeName, false);
            SafeParcelWriter.finishObjectHeader(dest, header);
        }

        public static final Parcelable.Creator<FastJsonResponse.Field> CREATOR = new Parcelable.Creator<FastJsonResponse.Field>() {
            @Override
            public FastJsonResponse.Field createFromParcel(Parcel source) {
                int end = SafeParcelReader.readObjectHeader(source);
                int versionCode = 1;
                int typeIn = 0;
                boolean typeInArray = false;
                int typeOut = 0;
                boolean typeOutArray = false;
                String outputFieldName = null;
                int safeParcelFieldId = 0;
                String concreteTypeName = null;
                while (source.dataPosition() < end) {
                    int header = SafeParcelReader.readHeader(source);
                    switch (SafeParcelReader.getFieldId(header)) {
                        case 1: versionCode = SafeParcelReader.readInt(source, header); break;
                        case 2: typeIn = SafeParcelReader.readInt(source, header); break;
                        case 3: typeInArray = SafeParcelReader.readBool(source, header); break;
                        case 4: typeOut = SafeParcelReader.readInt(source, header); break;
                        case 5: typeOutArray = SafeParcelReader.readBool(source, header); break;
                        case 6: outputFieldName = SafeParcelReader.readString(source, header); break;
                        case 7: safeParcelFieldId = SafeParcelReader.readInt(source, header); break;
                        case 8: concreteTypeName = SafeParcelReader.readString(source, header); break;
                        default: SafeParcelReader.skip(source, header);
                    }
                }
                source.setDataPosition(end);
                return new FastJsonResponse.Field(versionCode, typeIn, typeInArray, typeOut,
                        typeOutArray, outputFieldName, safeParcelFieldId, concreteTypeName);
            }

            @Override
            public FastJsonResponse.Field[] newArray(int size) {
                return new FastJsonResponse.Field[size];
            }
        };
    }
}
