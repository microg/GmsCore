/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils;

import android.util.Base64;

public class ToStringHelper {
    private StringBuilder sb;
    private boolean hasField;
    private boolean hasValue;
    private boolean hasEnd;

    public ToStringHelper(String name) {
        this.sb = new StringBuilder(name).append("[");
    }

    public static ToStringHelper name(String name) {
        return new ToStringHelper(name);
    }

    public ToStringHelper value(String val) {
        if (!hasField) {
            if (hasValue) sb.append(',');
            sb.append(val);
            hasValue = true;
        }
        return this;
    }

    public ToStringHelper value(long val) {
        return value(Long.toString(val));
    }

    public ToStringHelper value(double val) {
        return value(Double.toString(val));
    }

    public ToStringHelper value(Object val) {
        if (val instanceof Long) value((long) val);
        if (val instanceof Double) value((double) val);
        return value(val, false);
    }

    public ToStringHelper value(Object val, boolean forceNull) {
        if (val == null && !forceNull) return this;
        return value(val == null ? "null" : val.toString());
    }

    public ToStringHelper value(byte[] val) {
        return value(val, false);
    }

    public ToStringHelper value(byte[] val, boolean forceNull) {
        if (val == null && !forceNull) return this;
        return value(val == null ? "null" : Base64.encodeToString(val, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE));
    }

    private ToStringHelper fieldUnquoted(String name, String val) {
        if (hasValue || hasField) sb.append(", ");
        sb.append(name).append('=').append(val);
        hasField = true;
        return this;
    }

    public ToStringHelper field(String name, String val) {
        return field(name, val, false);
    }

    public ToStringHelper field(String name, String val, boolean forceNull) {
        if (val == null && !forceNull) return this;
        if (val == null) return fieldUnquoted(name, "null");
        if (hasValue || hasField) sb.append(", ");
        sb.append(name).append("=\"").append(val.replace("\"", "\\\"")).append('"');
        hasField = true;
        return this;
    }

    public ToStringHelper field(String name, long val) {
        return fieldUnquoted(name, Long.toString(val));
    }

    public ToStringHelper field(String name, double val) {
        return fieldUnquoted(name, Double.toString(val));
    }

    public ToStringHelper field(String name, boolean val) {
        return fieldUnquoted(name, Boolean.toString(val));
    }

    public ToStringHelper field(String name, Object val) {
        if (val instanceof Long) return field(name, (long) val);
        if (val instanceof Double) return field(name, (double) val);
        if (val instanceof Boolean) return field(name, (boolean) val);
        return field(name, val, false);
    }

    public ToStringHelper field(String name, Object val, boolean forceNull) {
        if (val == null && !forceNull) return this;
        return fieldUnquoted(name, val == null ? "null" : val.toString());
    }

    public ToStringHelper field(String name, byte[] val) {
        return field(name, val, false);
    }

    public ToStringHelper field(String name, byte[] val, boolean forceNull) {
        if (val == null && !forceNull) return this;
        return fieldUnquoted(name, val == null ? "null" : Base64.encodeToString(val, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE));
    }

    public String end() {
        if (!hasEnd) sb.append(']');
        hasEnd = true;
        return sb.toString();
    }
}
