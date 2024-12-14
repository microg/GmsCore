/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fitness.data;

import android.os.Bundle;
import android.os.Build.VERSION;
import android.os.Parcel;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.Map;

import static com.google.android.gms.fitness.data.Field.*;

/**
 * Holder object for the value of a single field in a data point. Values are not constructed directly; a value for each field of the data type
 * is created for each data point.
 * <p>
 * A field value has a particular format, and should be set and read using the format-specific methods. For instance, a float value should be set
 * via {@link #setFloat(float)} and read via {@link #asFloat()}. Formats are defined as constants in {@link com.google.android.gms.fitness.data.Field}.
 */
@SafeParcelable.Class
public final class Value extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getFormat")
    public int format;
    @Field(value = 2, getterName = "isSet")
    public boolean set;
    @Field(value = 3, getterName = "getValue")
    public float value;
    @Field(value = 4, getterName = "getStringValue")
    public String stringValue;
    @Field(value = 5, getterName = "getMapValue")
    @Nullable
    public Bundle mapValue;
    @Field(value = 6, getterName = "getIntArrayValue")
    public int[] intArrayValue;
    @Field(value = 7, getterName = "getFloatArrayValue")
    public float[] floatArrayValue;
    @Field(value = 8, getterName = "getBlob")
    public byte[] blob;

    @Constructor
    public Value(@Param(1) int format, @Param(2) boolean set, @Param(3) float value, @Param(4) String stringValue, @Param(5) @Nullable Bundle mapValue, @Param(6) int[] intArrayValue, @Param(7) float[] floatArrayValue, @Param(8) byte[] blob) {
        this.format = format;
        this.set = set;
        this.value = value;
        this.stringValue = stringValue;
        this.mapValue = mapValue;
        this.intArrayValue = intArrayValue;
        this.floatArrayValue = floatArrayValue;
        this.blob = blob;
    }

    Value(int format) {
        this(format, false, 0f, null, null, null, null, null);
    }

    /**
     * Returns the value of this object as an activity. The integer representation of the activity is converted to a String prior to returning.
     *
     * @return One of the constants from {@link FitnessActivities}; {@link FitnessActivities#UNKNOWN} if the object does not hold a valid activity
     * representation
     * @throws IllegalStateException If this {@link Value} does not correspond to a {@link com.google.android.gms.fitness.data.Field#FORMAT_INT32}
     */
    public String asActivity() {
        return null; // TODO
    }

    /**
     * Returns the value of this object as a float.
     *
     * @throws IllegalStateException If this {@link Value} does not correspond to a {@link com.google.android.gms.fitness.data.Field#FORMAT_FLOAT}
     */
    public float asFloat() {
        if (format != FORMAT_FLOAT) throw new IllegalStateException("Value is not a float.");
        return value;
    }

    /**
     * Returns the value of this object as a int.
     *
     * @throws IllegalStateException If this {@link Value} does not correspond to a {@link com.google.android.gms.fitness.data.Field#FORMAT_INT32}
     */
    public int asInt() {
        if (format != FORMAT_INT32) throw new IllegalStateException("Value is not a int.");
        return Float.floatToRawIntBits(this.value);
    }

    /**
     * Returns the value of this object as a string.
     *
     * @throws IllegalStateException If this {@link Value} does not correspond to a {@link com.google.android.gms.fitness.data.Field#FORMAT_STRING}
     */
    @NonNull
    public String asString() {
        if (format != FORMAT_STRING) throw new IllegalStateException("Value is not a string.");
        if (stringValue == null) return "";
        return stringValue;
    }

    /**
     * Clears any value currently associated with the given {@code key} in the map. This method can be used only on map values.
     *
     * @param key The key you're modifying.
     * @deprecated Use {@link DataPoint.Builder} to construct new {@link DataPoint} instances.
     */
    @Deprecated
    public void clearKey(String key) {
        if (format != FORMAT_MAP) throw new IllegalStateException("Value is not a map.");
        if (mapValue != null) {
            mapValue.remove(key);
        }
    }

    /**
     * Returns the format of this value, which matches the appropriate field in the data type definition.
     *
     * @return One of the format constants from {@link com.google.android.gms.fitness.data.Field}.
     */
    public int getFormat() {
        return format;
    }

    /**
     * Returns the value of the given key in the map as a {@link Float}.
     *
     * @return {@code null} if the key doesn't have a set value in the map.
     * @throws IllegalStateException If this {@link Value} does not correspond to a {@link com.google.android.gms.fitness.data.Field#FORMAT_MAP}
     */
    @Nullable
    public Float getKeyValue(@NonNull String key) {
        if (format != FORMAT_MAP) throw new IllegalStateException("Value is not a map.");
        if (mapValue == null || !mapValue.containsKey(key)) {
            return null;
        }
        mapValue.setClassLoader(MapValue.class.getClassLoader());
        if (VERSION.SDK_INT >= 33) {
            return mapValue.getParcelable(key, MapValue.class).asFloat();
        } else {
            //noinspection deprecation
            return ((MapValue) mapValue.getParcelable(key)).asFloat();
        }
    }

    /**
     * Returns {@code true} if this object's value has been set by calling one of the setters.
     */
    public boolean isSet() {
        return set;
    }

    float getValue() {
        return value;
    }

    String getStringValue() {
        return stringValue;
    }

    @Nullable
    Bundle getMapValue() {
        return mapValue;
    }

    int[] getIntArrayValue() {
        return intArrayValue;
    }

    float[] getFloatArrayValue() {
        return floatArrayValue;
    }

    byte[] getBlob() {
        return blob;
    }

    /**
     * Updates this value object to represent an activity value. Activities are internally represented as integers for storage.
     *
     * @param activity One of the activities from {@link FitnessActivities}
     * @deprecated Use {@link DataPoint.Builder} to construct new {@link DataPoint} instances.
     */
    public void setActivity(String activity) {
        setInt(0); // TODO
    }

    /**
     * Updates this value object to represent a float value. Any previous values associated with this object are erased.
     *
     * @param value The new value that this objects holds.
     * @deprecated Use {@link DataPoint.Builder} to construct new {@link DataPoint} instances.
     */
    public void setFloat(float value) {
        if (format != FORMAT_FLOAT) throw new IllegalStateException("Value is not a float.");
        this.set = true;
        this.value = value;
    }

    /**
     * Updates this value object to represent an int value. Any previous values are erased.
     *
     * @param value The new value that this object holds.
     * @deprecated Use {@link DataPoint.Builder} to construct new {@link DataPoint} instances.
     */
    public void setInt(int value) {
        if (format != FORMAT_INT32) throw new IllegalStateException("Value is not a int.");
        this.set = true;
        this.value = Float.intBitsToFloat(value);
    }

    /**
     * Updates the value for a given key in the map to the given float value. Any previous values associated with the key are erased. This method
     * can be used only on map values.
     * <p>
     * Key values should be kept small whenever possible. This is specially important for high frequency streams, since large keys may result in
     * down sampling.
     *
     * @param key   The key you're modifying.
     * @param value The new value for the given key.
     * @deprecated Use {@link DataPoint.Builder} to construct new {@link DataPoint} instances.
     */
    public void setKeyValue(String key, float value) {
        if (format != FORMAT_MAP) throw new IllegalStateException("Value is not a map.");
        this.set = true;
        if (mapValue == null) mapValue = new Bundle();
        mapValue.putParcelable(key, MapValue.ofFloat(value));
    }

    void setMap(@NonNull Map<String, Float> value) {
        if (format != FORMAT_MAP) throw new IllegalStateException("Value is not a map.");
        this.set = true;
        if (mapValue == null) mapValue = new Bundle();
        for (String key : value.keySet()) {
            mapValue.putParcelable(key, MapValue.ofFloat(value.get(key)));
        }
    }

    /**
     * Updates this value object to represent a string value. Any previous values associated with this object are erased.
     * <p>
     * String values should be kept small whenever possible. This is specially important for high frequency streams, since large values may result
     * in down sampling.
     *
     * @param value The new value that this objects holds.
     * @deprecated Use {@link DataPoint.Builder} to construct new {@link DataPoint} instances.
     */
    public void setString(String value) {
        if (format != FORMAT_STRING) throw new IllegalStateException("Value is not a string.");
        this.set = true;
        this.stringValue = value;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Value> CREATOR = findCreator(Value.class);
}