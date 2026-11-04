/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.server.response;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class FastParser<T extends FastJsonResponse> {
    public void parse(InputStream input, T target) throws ParseException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            StringBuilder json = new StringBuilder();
            char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) != -1) json.append(buffer, 0, count);
            if (json.length() == 0) throw new ParseException("No data to parse");
            populate(new JSONObject(json.toString()), target);
        } catch (IOException | JSONException | ReflectiveOperationException e) {
            throw new ParseException(e);
        }
    }

    private void populate(JSONObject json, FastJsonResponse target)
            throws JSONException, ReflectiveOperationException, ParseException {
        for (Map.Entry<String, FastJsonResponse.Field<?, ?>> entry
                : target.getFieldMappings().entrySet()) {
            if (!json.has(entry.getKey())) continue;
            FastJsonResponse.Field<?, ?> field = entry.getValue();
            Object raw = json.get(entry.getKey());
            if (field.typeInArray) {
                if (raw == JSONObject.NULL) {
                    dispatchArray(target, field, null);
                } else {
                    JSONArray array = (JSONArray) raw;
                    ArrayList<Object> values = new ArrayList<>(array.length());
                    for (int i = 0; i < array.length(); i++) {
                        Object item = array.get(i);
                        values.add(item == JSONObject.NULL ? null : decodeScalar(field, item));
                    }
                    dispatchArray(target, field, values);
                }
            } else {
                dispatchScalar(target, field,
                        raw == JSONObject.NULL ? null : decodeScalar(field, raw));
            }
        }
    }

    private Object decodeScalar(FastJsonResponse.Field<?, ?> field, Object raw)
            throws JSONException, ReflectiveOperationException, ParseException {
        switch (field.typeIn) {
            case FastJsonResponse.TYPE_INTEGER: return ((Number) raw).intValue();
            case FastJsonResponse.TYPE_BIG_INTEGER: return new BigInteger(raw.toString());
            case FastJsonResponse.TYPE_LONG: return ((Number) raw).longValue();
            case FastJsonResponse.TYPE_FLOAT: return ((Number) raw).floatValue();
            case FastJsonResponse.TYPE_DOUBLE: return ((Number) raw).doubleValue();
            case FastJsonResponse.TYPE_BIG_DECIMAL: return new BigDecimal(raw.toString());
            case FastJsonResponse.TYPE_BOOLEAN: return (Boolean) raw;
            case FastJsonResponse.TYPE_STRING: return raw.toString();
            case FastJsonResponse.TYPE_BASE64:
                return Base64.decode(raw.toString(), Base64.DEFAULT);
            case FastJsonResponse.TYPE_BASE64_URL_SAFE:
                return Base64.decode(raw.toString(), Base64.URL_SAFE);
            case FastJsonResponse.TYPE_STRING_MAP:
                return readStringMap((JSONObject) raw);
            case FastJsonResponse.TYPE_CONCRETE:
                FastJsonResponse child = field.newConcreteTypeInstance();
                populate((JSONObject) raw, child);
                return child;
            default: throw new ParseException("Invalid field type " + field.typeIn);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void dispatchScalar(FastJsonResponse target, FastJsonResponse.Field field, Object value)
            throws ParseException {
        switch (field.typeIn) {
            case FastJsonResponse.TYPE_INTEGER: target.setInteger(field, value == null ? 0 : (Integer) value); break;
            case FastJsonResponse.TYPE_BIG_INTEGER: target.setBigInteger(field, (BigInteger) value); break;
            case FastJsonResponse.TYPE_LONG: target.setLong(field, value == null ? 0L : (Long) value); break;
            case FastJsonResponse.TYPE_FLOAT: target.setFloat(field, value == null ? 0f : (Float) value); break;
            case FastJsonResponse.TYPE_DOUBLE: target.setDouble(field, value == null ? 0d : (Double) value); break;
            case FastJsonResponse.TYPE_BIG_DECIMAL: target.setBigDecimal(field, (BigDecimal) value); break;
            case FastJsonResponse.TYPE_BOOLEAN: target.setBoolean(field, value != null && (Boolean) value); break;
            case FastJsonResponse.TYPE_STRING: target.setString(field, (String) value); break;
            case FastJsonResponse.TYPE_BASE64:
            case FastJsonResponse.TYPE_BASE64_URL_SAFE: target.setDecodedBytes(field, (byte[]) value); break;
            case FastJsonResponse.TYPE_STRING_MAP: target.setStringMap(field, (Map<String, String>) value); break;
            case FastJsonResponse.TYPE_CONCRETE: target.addConcreteType(field, (FastJsonResponse) value); break;
            default: throw new ParseException("Invalid field type " + field.typeIn);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void dispatchArray(FastJsonResponse target, FastJsonResponse.Field field, List values)
            throws ParseException {
        switch (field.typeIn) {
            case FastJsonResponse.TYPE_INTEGER: target.setIntegerArray(field, values); break;
            case FastJsonResponse.TYPE_BIG_INTEGER: target.setBigIntegerArray(field, values); break;
            case FastJsonResponse.TYPE_LONG: target.setLongArray(field, values); break;
            case FastJsonResponse.TYPE_FLOAT: target.setFloatArray(field, values); break;
            case FastJsonResponse.TYPE_DOUBLE: target.setDoubleArray(field, values); break;
            case FastJsonResponse.TYPE_BIG_DECIMAL: target.setBigDecimalArray(field, values); break;
            case FastJsonResponse.TYPE_BOOLEAN: target.setBooleanArray(field, values); break;
            case FastJsonResponse.TYPE_STRING: target.setStringArray(field, values); break;
            case FastJsonResponse.TYPE_CONCRETE: target.addConcreteTypeArray(field, values); break;
            default: throw new ParseException("Invalid array field type " + field.typeIn);
        }
    }

    private Map<String, String> readStringMap(JSONObject json) throws JSONException {
        HashMap<String, String> result = new HashMap<>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            result.put(key, json.isNull(key) ? null : json.getString(key));
        }
        return result;
    }

    public static class ParseException extends Exception {
        public ParseException(String message) { super(message); }
        public ParseException(Throwable cause) { super(cause); }
    }
}
