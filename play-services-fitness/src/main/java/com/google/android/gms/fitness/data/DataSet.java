/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a fixed set of data points in a data type's stream from a particular data source. A data set usually represents data at
 * fixed time boundaries, and can be used both for batch data insertion and as a result of read requests.
 */
@SafeParcelable.Class
public class DataSet extends AbstractSafeParcelable {

    @Field(1000)
    final int versionCode;
    @Field(value = 1, getterName = "getDataSource")
    @NonNull
    private final DataSource dataSource;
    @Field(value = 3, getterName = "getRawDataPoints")
    @NonNull
    private final List<DataPoint> rawDataPoints;
    @Field(value = 4, getterName = "getUniqueDataSources")
    @NonNull
    private final List<DataSource> uniqueDataSources;

    @Constructor
    DataSet(@Param(1000) int versionCode, @Param(1) @NonNull DataSource dataSource, @Param(3) @NonNull List<DataPoint> rawDataPoints, @Param(4) List<DataSource> uniqueDataSources) {
        this.versionCode = versionCode;
        this.dataSource = dataSource;
        this.rawDataPoints = rawDataPoints;
        this.uniqueDataSources = versionCode < 2 ? Collections.singletonList(dataSource) : uniqueDataSources;
    }

    DataSet(@NonNull DataSource dataSource) {
        this.versionCode = 3;
        this.dataSource = dataSource;
        this.rawDataPoints = new ArrayList<>();
        this.uniqueDataSources = new ArrayList<>();
        uniqueDataSources.add(dataSource);
    }

    /**
     * Adds a data point to this data set. The data points should be for the correct data type and data source, and should have its timestamp
     * already set.
     *
     * @throws IllegalArgumentException If dataPoint has invalid data.
     * @deprecated Build {@link DataSet} instances using the builder.
     */
    @Deprecated
    public void add(@NonNull DataPoint dataPoint) {
        if (!dataPoint.getDataSource().getStreamIdentifier().equals(dataSource.getStreamIdentifier()))
            throw new IllegalArgumentException("Conflicting data sources found");
        // TODO
        rawDataPoints.add(dataPoint);
    }

    /**
     * Adds a list of data points to this data set in bulk. All data points should be for the correct data type and data source, and should have their
     * timestamp already set.
     *
     * @deprecated Build {@link DataSet} instances using the builder.
     */
    @Deprecated
    public void addAll(@NonNull Iterable<DataPoint> iterable) {
        for (DataPoint dataPoint : iterable) {
            add(dataPoint);
        }
    }

    /**
     * Creates an empty data point for this data set's data source. The new data point is not added to the data set by this method. After the data
     * point is initialized, {@link #add(DataPoint)} should be called.
     */
    @NonNull
    public DataPoint createDataPoint() {
        return DataPoint.create(this.dataSource);
    }

    /**
     * Returns the list of data points represented by this data set. The data points will preserve the same order in which they were inserted.
     * <p>
     * Certain APIs that return a DataSet might insert data points in chronological order, but this isn't enforced.
     */
    @NonNull
    public List<DataPoint> getDataPoints() {
        return Collections.unmodifiableList(rawDataPoints);
    }

    /**
     * Returns the data source which this data set represents. All of the data points in the data set are from this data source.
     */
    @NonNull
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Returns the data type this data set represents. All of the data points in the data set are of this data type.
     */
    @NonNull
    public DataType getDataType() {
        return dataSource.getDataType();
    }

    @NonNull
    List<DataPoint> getRawDataPoints() {
        return rawDataPoints;
    }

    @NonNull
    List<DataSource> getUniqueDataSources() {
        return uniqueDataSources;
    }

    /**
     * Creates a new builder for a {@link DataSet} with the given {@code dataSource}.
     *
     * @throws NullPointerException If specified data source is null.
     */
    @NonNull
    public static Builder builder(@NonNull DataSource dataSource) {
        return new Builder(dataSource);
    }

    /**
     * Creates a new data set to hold data points for the given {@code dataSource}.
     * <p>
     * Data points with the matching data source can be created using {@link #createDataPoint()}, and after having the values set added to the data set
     * via {@link #add(DataPoint)}.
     *
     * @throws NullPointerException If specified data source is null.
     */
    @NonNull
    public static DataSet create(@NonNull DataSource dataSource) {
        return new DataSet(dataSource);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    /**
     * Builder used to create new data sets.
     */
    public static class Builder {
        private final DataSet dataSet;
        private boolean built = false;

        Builder(DataSource dataSource) {
            this.dataSet = DataSet.create(dataSource);
        }

        /**
         * Adds a data point to this data set. The data points should be for the correct data type and data source, and should have its timestamp
         * already set.
         *
         * @throws IllegalArgumentException If dataPoint has the wrong {@link DataSource}, or contain invalid data.
         */
        @NonNull
        public Builder add(@NonNull DataPoint dataPoint) {
            if (built) throw new IllegalStateException("DataSet has already been built.");
            this.dataSet.add(dataPoint);
            return this;
        }

        /**
         * Adds a list of data points to this data set in bulk. All data points should be for the correct data type and data source, and should have their
         * timestamp already set.
         *
         * @throws IllegalArgumentException If the {@code dataPoints} have the wrong source, or contain invalid data.
         */
        @NonNull
        public Builder addAll(@NonNull Iterable<DataPoint> iterable) {
            if (built) throw new IllegalStateException("DataSet has already been built.");
            this.dataSet.addAll(iterable);
            return this;
        }

        /**
         * Finishes building and returns the {@link DataSet}.
         *
         * @throws IllegalStateException If called more than once.
         */
        @NonNull
        public DataSet build() {
            if (built) throw new IllegalStateException("DataSet has already been built.");
            this.built = true;
            return this.dataSet;
        }
    }

    public static final SafeParcelableCreatorAndWriter<DataSet> CREATOR = findCreator(DataSet.class);

}
