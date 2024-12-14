/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fitness.data;

import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SafeParcelable.Class
public class DataPoint extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getDataSource")
    @NonNull
    private final DataSource dataSource;
    @Field(value = 3, getterName = "getTimestampNanos")
    private long timestampNanos;
    @Field(value = 4, getterName = "getStartTimeNanos")
    private long startTimeNanos;
    @Field(value = 5, getterName = "getValues")
    private final Value[] values;
    @Field(value = 6, getterName = "getOriginalDataSourceIfSet")
    @Nullable
    private DataSource originalDataSource;
    @Field(value = 7, getterName = "getRawTimestamp")
    private final long rawTimestamp;

    DataPoint(DataSource dataSource) {
        this.dataSource = dataSource;
        List<com.google.android.gms.fitness.data.Field> fields = dataSource.getDataType().getFields();
        this.values = new Value[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            values[i] = new Value(fields.get(i).getFormat());
        }
        this.rawTimestamp = 0;
    }

    @Constructor
    DataPoint(@Param(1) @NonNull DataSource dataSource, @Param(3) long timestampNanos, @Param(4) long startTimeNanos, @Param(5) Value[] values, @Param(6) @Nullable DataSource originalDataSource, @Param(7) long rawTimestamp) {
        this.dataSource = dataSource;
        this.timestampNanos = timestampNanos;
        this.startTimeNanos = startTimeNanos;
        this.values = values;
        this.originalDataSource = originalDataSource;
        this.rawTimestamp = rawTimestamp;
    }

    /**
     * Returns the data source for the data point. If the data point is part of a {@link DataSet}, this will correspond to the data set's data source.
     */
    @NonNull
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Returns the data type defining the format of the values in this data point.
     */
    @NonNull
    public DataType getDataType() {
        return dataSource.getDataType();
    }

    /**
     * Returns the end time of the interval represented by this data point, in the given unit since epoch. This method is equivalent to
     * {@link #getTimestamp(TimeUnit)}
     */
    public long getEndTime(@NonNull TimeUnit timeUnit) {
        return timeUnit.convert(this.timestampNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the original data source for this data point. The original data source helps identify the source of the data point as it gets merged and
     * transformed into different streams.
     * <p>
     * Note that, if this data point is part of a {@link DataSet}, the data source returned here may be different from the data set's data source. In case of
     * transformed or merged data sets, each data point's original data source will retain the original attribution as much as possible, while the
     * data set's data source will represent the merged or transformed stream.
     * <p>
     * WARNING: do not rely on this field for anything other than debugging. The value of this field, if it is set at all, is an implementation detail and
     * is not guaranteed to remain consistent.
     */
    @NonNull
    public DataSource getOriginalDataSource() {
        if (originalDataSource != null) return originalDataSource;
        return dataSource;
    }

    /**
     * Returns the start time of the interval represented by this data point, in the given unit since epoch.
     */
    public long getStartTime(@NonNull TimeUnit timeUnit) {
        return timeUnit.convert(this.startTimeNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the timestamp of the data point, in the given unit since epoch. For data points that represent intervals, this method will return the
     * end time.
     */
    public long getTimestamp(@NonNull TimeUnit timeUnit) {
        return timeUnit.convert(this.timestampNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the value holder for the field with the given name. This method can be used both to query the value and to set it.
     *
     * @param field One of the fields of this data type.
     * @return The Value associated with the given field.
     * @throws IllegalArgumentException If the given field doesn't match any of the fields for this DataPoint's data type.
     */
    @NonNull
    public Value getValue(com.google.android.gms.fitness.data.Field field) {
        return this.values[getDataType().indexOf(field)];
    }

    long getTimestampNanos() {
        return timestampNanos;
    }

    long getStartTimeNanos() {
        return startTimeNanos;
    }

    Value[] getValues() {
        return values;
    }

    DataSource getOriginalDataSourceIfSet() {
        return originalDataSource;
    }

    long getRawTimestamp() {
        return rawTimestamp;
    }

    /**
     * Sets the values of this data point, where the format for all of its values is float.
     *
     * @param values The value for each field of the data point, in order.
     * @deprecated Use {@link DataPoint.Builder} to create {@link DataPoint} instances.
     */
    @Deprecated
    public DataPoint setFloatValues(float... values) {
        if (values.length != this.getDataType().getFields().size())
            throw new IllegalArgumentException("The number of values does not match the number of fields");
        for (int i = 0; i < values.length; i++) {
            this.values[i].setFloat(values[i]);
        }
        return this;
    }

    /**
     * Sets the values of this data point, where the format for all of its values is int.
     *
     * @param values The value for each field of the data point, in order.
     * @deprecated Use {@link DataPoint.Builder} to create {@link DataPoint} instances.
     */
    @Deprecated
    public DataPoint setIntValues(int... values) {
        if (values.length != this.getDataType().getFields().size())
            throw new IllegalArgumentException("The number of values does not match the number of fields");
        for (int i = 0; i < values.length; i++) {
            this.values[i].setInt(values[i]);
        }
        return this;
    }

    /**
     * Sets the time interval of a data point that represents an interval of time. For data points that represent instantaneous readings,
     * {@link #setTimestamp(long, TimeUnit)} should be used.
     *
     * @param startTime The start time in the given unit, representing elapsed time since epoch.
     * @param endTime   The end time in the given unit, representing elapsed time since epoch.
     * @param timeUnit  The time unit of both start and end timestamps.
     * @deprecated Use {@link DataPoint.Builder} to create {@link DataPoint} instances.
     */
    @Deprecated
    public DataPoint setTimeInterval(long startTime, long endTime, TimeUnit timeUnit) {
        this.startTimeNanos = timeUnit.toNanos(startTime);
        this.timestampNanos = timeUnit.toNanos(endTime);
        return this;
    }

    /**
     * Sets the timestamp of a data point that represent an instantaneous reading, measurement, or input. For data points that represent intervals,
     * {@link #setTimeInterval(long, long, TimeUnit)} should be used.
     *
     * @param timestamp The timestamp in the given unit, representing elapsed time since epoch.
     * @param timeUnit  The unit of the given timestamp.
     * @deprecated Use {@link DataPoint.Builder} to create {@link DataPoint} instances.
     */
    @Deprecated
    public DataPoint setTimestamp(long timestamp, TimeUnit timeUnit) {
        this.timestampNanos = timeUnit.toNanos(timestamp);
        return this;
    }

    /**
     * Creates a new builder for a {@link DataPoint} with the given {@code dataSource}.
     *
     * @throws NullPointerException If specified data source is null.
     */
    @NonNull
    public static Builder builder(@NonNull DataSource dataSource) {
        return new Builder(dataSource);
    }

    /**
     * Creates a new data point for the given dataSource. An unset {@link Value} is created for each field of the data source's data type.
     *
     * @return An empty data point instance.
     * @deprecated Use {@link DataPoint.Builder} to create {@link DataPoint} instances.
     */
    @NonNull
    @Deprecated
    public static DataPoint create(@NonNull DataSource dataSource) {
        return new DataPoint(dataSource);
    }

    /**
     * Extracts a data point from a callback intent received after registering to a data source with a PendingIntent.
     *
     * @return The extracted DataPoint, or {@code null} if the given intent does not contain a DataPoint
     */
    @Nullable
    public static DataPoint extract(@NonNull Intent intent) {
        return SafeParcelableSerializer.deserializeFromBytes(intent.getByteArrayExtra("com.google.android.gms.fitness.EXTRA_DATA_POINT"), CREATOR);
    }

    /**
     * Builder for {@link DataPoint} instances.
     */
    public static class Builder {
        private final DataPoint dataPoint;
        private boolean built = false;

        Builder(DataSource dataSource) {
            this.dataPoint = DataPoint.create(dataSource);
        }

        /**
         * Builds and returns the {@link DataPoint}.
         */
        @NonNull
        public DataPoint build() {
            if (built) throw new IllegalStateException("DataPoint already built");
            this.built = true;
            return this.dataPoint;
        }

        /**
         * Sets the value of an activity field to {@code activity}.
         *
         * @throws IllegalArgumentException If the given index is out of the range for this data type.
         * @throws IllegalStateException    If the field isn't of format {@link com.google.android.gms.fitness.data.Field#FORMAT_INT32}.
         */
        @NonNull
        public Builder setActivityField(@NonNull com.google.android.gms.fitness.data.Field field, @NonNull String activity) {
            if (built) throw new IllegalStateException("DataPoint already built");
            this.dataPoint.getValue(field).setActivity(activity);
            return this;
        }

        /**
         * Sets the floating point value of the given {@code field} to {@code value}.
         *
         * @throws IllegalArgumentException If the given index is out of the range for this data type.
         * @throws IllegalStateException    If the field isn't of format {@link com.google.android.gms.fitness.data.Field#FORMAT_FLOAT}.
         */
        @NonNull
        public Builder setField(@NonNull com.google.android.gms.fitness.data.Field field, float value) {
            if (built) throw new IllegalStateException("DataPoint already built");
            this.dataPoint.getValue(field).setFloat(value);
            return this;
        }


        /**
         * Sets the map value of the given {@code field} to {@code value}.
         *
         * @throws IllegalArgumentException If the given index is out of the range for this data type.
         * @throws IllegalStateException    If the field isn't of format {@link com.google.android.gms.fitness.data.Field#FORMAT_MAP}.
         */
        @NonNull
        public Builder setField(@NonNull com.google.android.gms.fitness.data.Field field, @NonNull Map<String, Float> map) {
            if (built) throw new IllegalStateException("DataPoint already built");
            this.dataPoint.getValue(field).setMap(map);
            return this;
        }


        /**
         * Sets the integer value of the given {@code field} to {@code value}.
         *
         * @throws IllegalArgumentException If the given index is out of the range for this data type.
         * @throws IllegalStateException    If the field isn't of format {@link com.google.android.gms.fitness.data.Field#FORMAT_INT32}.
         */
        @NonNull
        public Builder setField(@NonNull com.google.android.gms.fitness.data.Field field, int value) {
            if (built) throw new IllegalStateException("DataPoint already built");
            this.dataPoint.getValue(field).setInt(value);
            return this;
        }

        /**
         * Sets the string value of the given {@code field} to {@code value}.
         *
         * @throws IllegalArgumentException If the given index is out of the range for this data type.
         * @throws IllegalStateException    If the field isn't of format {@link com.google.android.gms.fitness.data.Field#FORMAT_STRING}.
         */
        @NonNull
        public Builder setField(@NonNull com.google.android.gms.fitness.data.Field field, @NonNull String value) {
            if (built) throw new IllegalStateException("DataPoint already built");
            this.dataPoint.getValue(field).setString(value);
            return this;
        }

        /**
         * Sets the values of the data point, where the format for all of its values is float.
         *
         * @param values The value for each field of the data point, in order.
         */
        @NonNull
        public Builder setFloatValues(@NonNull float... values) {
            if (built) throw new IllegalStateException("DataPoint already built");
            this.dataPoint.setFloatValues(values);
            return this;
        }

        /**
         * Sets the values of the data point, where the format for all of its values is int.
         *
         * @param values The value for each field of the data point, in order.
         */
        @NonNull
        public Builder setIntValues(@NonNull int... values) {
            if (built) throw new IllegalStateException("DataPoint already built");
            this.dataPoint.setIntValues(values);
            return this;
        }

        /**
         * Sets the time interval of a data point that represents an interval of time. For data points that represent instantaneous readings,
         * {@link #setTimestamp(long, TimeUnit)} should be used.
         *
         * @param startTime The start time in the given unit, representing elapsed time since epoch.
         * @param endTime   The end time in the given unit, representing elapsed time since epoch.
         * @param timeUnit  The time unit of both start and end timestamps.
         */
        @NonNull
        public Builder setTimeInterval(long startTime, long endTime, @NonNull TimeUnit timeUnit) {
            if (built) throw new IllegalStateException("DataPoint already built");
            this.dataPoint.setTimeInterval(startTime, endTime, timeUnit);
            return this;
        }

        /**
         * Sets the timestamp of a data point that represent an instantaneous reading, measurement, or input. For data points that represent intervals,
         * {@link #setTimeInterval(long, long, TimeUnit)} should be used.
         *
         * @param timestamp The timestamp in the given unit, representing elapsed time since epoch.
         * @param timeUnit  The unit of the given timestamp.
         */
        @NonNull
        public Builder setTimestamp(long timestamp, @NonNull TimeUnit timeUnit) {
            if (built) throw new IllegalStateException("DataPoint already built");
            this.dataPoint.setTimestamp(timestamp, timeUnit);
            return this;
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataPoint> CREATOR = findCreator(DataPoint.class);

}
