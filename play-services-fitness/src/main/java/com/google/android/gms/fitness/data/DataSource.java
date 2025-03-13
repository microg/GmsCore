/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fitness.data;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Parcel;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.gms.common.Constants;
import org.microg.gms.utils.ToStringHelper;

/**
 * Definition of a unique source of sensor data. Data sources can expose raw data coming from hardware sensors on local or companion
 * devices. They can also expose derived data, created by transforming or merging other data sources. Multiple data sources can exist for the
 * same data type. Every data point inserted into or read from Google Fit has an associated data source.
 * <p>
 * The data source contains enough information to uniquely identify its data, including the hardware device and the application that
 * collected and/or transformed the data. It also holds useful metadata, such as a stream name and the device type.
 * <p>
 * The data source's data stream can be accessed in a live fashion by registering a data source listener, or via queries over fixed time intervals.
 * <p>
 * An end-user-visible name for the data stream can be set by calling {@link DataSource.Builder.setStreamName(String)} or otherwise computed
 * from the device model and application name.
 */
@SafeParcelable.Class
public class DataSource extends AbstractSafeParcelable {

    /**
     * Name for the parcelable intent extra containing a data source. It can be extracted using {@link #extract(Intent)}.
     */
    public static final String EXTRA_DATA_SOURCE = "vnd.google.fitness.data_source";

    /**
     * Type constant for a data source which exposes original, raw data from an external source such as a hardware sensor, a wearable device, or
     * user input.
     */
    public static final int TYPE_RAW = 0;

    /**
     * Type constant for a data source which exposes data which is derived from one or more existing data sources by performing
     * transformations on the original data.
     */
    public static final int TYPE_DERIVED = 1;

    @Field(value = 1, getterName = "getDataType")
    @NonNull
    private final DataType dataType;
    @Field(value = 3, getterName = "getType")
    private final int type;
    @Field(value = 4, getterName = "getDevice")
    @Nullable
    private final Device device;
    @Field(value = 5, getterName = "getApplication")
    @Nullable
    final Application application;
    @Field(value = 6, getterName = "getStreamName")
    private final String streamName;

    @Constructor
    DataSource(@Param(1) @NonNull DataType dataType, @Param(3) int type, @Param(4) @Nullable Device device, @Param(5) @Nullable Application application, @Param(6) String streamName) {
        this.dataType = dataType;
        this.type = type;
        this.device = device;
        this.application = application;
        this.streamName = streamName;
    }

    @Nullable
    public Application getApplication() {
        return application;
    }

    /**
     * Returns the package name for the application responsible for setting the data, or {@code null} if unset/unknown. {@link PackageManager} can be used to
     * query relevant information about the application, such as the name, icon, and logo.
     * <p>
     * Data coming from local sensors or BLE devices will not have a corresponding application.
     */
    @Nullable
    public String getAppPackageName() {
        if (application == null) return null;
        return application.getPackageName();
    }

    /**
     * Returns the data type for data coming from this data source. Knowing the type of a data source can be useful to perform transformations on
     * top of raw data without using sources that are themselves computed by transforming raw data.
     */
    @NonNull
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Returns the device where data is being collected, or {@code null} if unset.
     */
    @Nullable
    public Device getDevice() {
        return device;
    }

    /**
     * Returns a unique identifier for the data stream produced by this data source. The identifier includes, in order:
     * <ol>
     * <li>the data source's type (raw or derived)</li>
     * <li>the data source's data type</li>
     * <li>the application's package name (unique for a given application)</li>
     * <li>the physical device's manufacturer, model, and serial number (UID)</li>
     * <li>the data source's stream name.</li>
     * </ol>
     */
    @NonNull
    public String getStreamIdentifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(type == TYPE_RAW ? "raw" : "derived");
        sb.append(":").append(dataType.getName());
        if (application != null) sb.append(":").append(application.getPackageName());
        if (device != null) sb.append(":").append(device.getDeviceId());
        if (streamName != null) sb.append(":").append(streamName);
        return sb.toString();
    }

    /**
     * Returns the specific stream name for the stream coming from this data source, or an empty string if unset.
     */
    @NonNull
    public String getStreamName() {
        return streamName;
    }

    /**
     * Returns the constant describing the type of this data source.
     *
     * @return One of the constant values ({@link #TYPE_DERIVED} or {@link #TYPE_RAW}), zero if unset. Values outside of this range should be treated as
     * unset/unknown.
     */
    public int getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return getStreamIdentifier().hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("DataSource").value(getStreamIdentifier()).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    /**
     * Extracts the data source extra from the given intent, such as an intent to view user's data.
     *
     * @return The data source, or {@code null} if not found.
     */
    @Nullable
    public static DataSource extract(@NonNull Intent intent) {
        return SafeParcelableSerializer.deserializeFromBytes(intent.getByteArrayExtra(EXTRA_DATA_SOURCE), CREATOR);
    }

    /**
     * A builder that can be used to construct new data source objects. In general, a built data source should be saved in memory to avoid the cost
     * of re-constructing it for every request.
     */
    public static class Builder {
        private DataType dataType;
        private Device device;
        private Application application;
        private int type = -1;
        private String streamName = "";

        /**
         * Finishes building the data source and returns a DataSource object.
         *
         * @throws IllegalStateException If the builder didn't have enough data to build a valid data source.
         */
        @NonNull
        public DataSource build() {
            if (dataType == null) throw new IllegalStateException("dataType must be set");
            if (type < 0) throw new IllegalStateException("type must be set");
            return new DataSource(dataType, type, device, application, streamName);
        }

        /**
         * Sets the package name for the application that is recording or computing the data. Used for data sources that aren't built into the platform
         * (local sensors and BLE sensors are built-in). It can be used to identify the data source, to disambiguate between data from different
         * applications, and also to link back to the original application for a detailed view.
         */
        @NonNull
        public Builder setAppPackageName(@NonNull String packageName) {
            Application application = Application.GMS_APP;
            this.application = Constants.GMS_PACKAGE_NAME.equals(packageName) ? Application.GMS_APP : new Application(packageName);
            return this;
        }

        /**
         * Sets the package name for the application that is recording or computing the data based on the app's context. This method should be
         * preferred when an application is creating a data source that represents its own data. When creating a data source to query data from other
         * apps, {@link #setAppPackageName(String)} should be used.
         */
        @NonNull
        public Builder setAppPackageName(@NonNull Context appContext) {
            setAppPackageName(appContext.getPackageName());
            return this;
        }

        /**
         * Sets the data type for the data source. Every data source is required to have a data type.
         *
         * @param dataType One of the data types defined in {@link DataType}, or a custom data type.
         */
        @NonNull
        public Builder setDataType(@NonNull DataType dataType) {
            this.dataType = dataType;
            return this;
        }

        /**
         * Sets the integrated device where data is being recorded (for instance, a phone that has sensors, or a wearable). Can be useful to identify the
         * data source, and to disambiguate between data from different devices. If the data is coming from the local device, use
         * {@link Device#getLocalDevice(Context)}.
         * <p>
         * Note that it may be useful to set the device even if the data is not coming from a hardware sensor on the device. For instance, if the user
         * installs an application which generates sensor data in two separate devices, the only way to differentiate the two data sources is using the
         * device. This can be specially important if both devices are used at the same time.
         */
        @NonNull
        public Builder setDevice(@NonNull Device device) {
            this.device = device;
            return this;
        }

        /**
         * The stream name uniquely identifies this particular data source among other data sources of the same type from the same underlying
         * producer. Setting the stream name is optional, but should be done whenever an application exposes two streams for the same data type, or
         * when a device has two equivalent sensors.
         * <p>
         * The stream name is used by {@link DataSource#getStreamIdentifier()} to make sure the different streams are properly separated when
         * querying or persisting data.
         *
         * @throws IllegalArgumentException If the specified stream name is null.
         */
        @NonNull
        public Builder setStreamName(@NonNull String streamName) {
            //noinspection ConstantValue
            if (streamName == null) throw new IllegalArgumentException("streamName must be set");
            this.streamName = streamName;
            return this;
        }

        /**
         * Sets the type of the data source. {@link DataSource#TYPE_DERIVED} should be used if any other data source is used in generating the data.
         * {@link DataSource#TYPE_RAW} should be used if the data comes completely from outside of Google Fit.
         */
        @NonNull
        public Builder setType(int type) {
            this.type = type;
            return this;
        }
    }

    public static final SafeParcelableCreatorAndWriter<DataSource> CREATOR = findCreator(DataSource.class);

}
