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

import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.android.gms.fitness.data.Field.*;

/**
 * The data type defines the schema for a stream of data being collected by, inserted into, or queried from Google Fit. The data type defines
 * only the representation and format of the data, and not how it's being collected, the sensor being used, or the parameters of the collection.
 * <p>
 * A data type contains one or more fields. In case of multi-dimensional data (such as location with latitude, longitude, and accuracy) each
 * field represents one dimension. Each data type field has a unique name which identifies it. The field also defines the format of the data
 * (such as int or float).
 * <p>
 * The data types in the {@code com.google} namespace are shared with any app with the user consent. These are fixed and can only be updated in
 * new releases of the platform. This class contains constants representing each of the {@code com.google} data types, each prefixed with {@code TYPE_}.
 * Custom data types can be accessed via the {@link ConfigClient}.
 * <p>
 * Certain data types can represent aggregates, and can be computed as part of read requests by calling
 * {@link DataReadRequest.Builder#aggregate(DataType)}. This class contains constants for all the valid aggregates, each prefixed with
 * {@code AGGREGATE_}. The aggregates for each input type can be queried via {@link #getAggregatesForInput(DataType)}.
 */
@SafeParcelable.Class
public class DataType extends AbstractSafeParcelable {
    /**
     * The common prefix for data type MIME types, for use in intents. The MIME type for a particular data type will be this prefix followed by
     * the data type name.
     * <p>
     * The data type's name is returned by {@link #getName()}. The full MIME type can be computed by {@link #getMimeType(DataType)}.
     */
    public static final String MIME_TYPE_PREFIX = "vnd.google.fitness.data_type/";

    public static final DataType TYPE_ACTIVITY_SEGMENT = new DataType("com.google.activity.segment", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_ACTIVITY);
    public static final DataType TYPE_BASAL_METABOLIC_RATE = new DataType("com.google.calories.bmr", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_CALORIES);
    public static final DataType TYPE_BODY_FAT_PERCENTAGE = new DataType("com.google.body.fat.percentage", "https://www.googleapis.com/auth/fitness.body.read", "https://www.googleapis.com/auth/fitness.body.write", FIELD_PERCENTAGE);
    public static final DataType TYPE_CALORIES_EXPENDED = new DataType("com.google.calories.expended", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_CALORIES);
    public static final DataType TYPE_CYCLING_PEDALING_CADENCE = new DataType("com.google.cycling.pedaling.cadence", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_RPM);
    public static final DataType TYPE_CYCLING_PEDALING_CUMULATIVE = new DataType("com.google.cycling.pedaling.cumulative", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_REVOLUTIONS);
    public static final DataType TYPE_CYCLING_WHEEL_REVOLUTION = new DataType("com.google.cycling.wheel_revolution.cumulative", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", FIELD_REVOLUTIONS);
    public static final DataType TYPE_CYCLING_WHEEL_RPM = new DataType("com.google.cycling.wheel_revolution.rpm", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", FIELD_RPM);
    public static final DataType TYPE_DISTANCE_DELTA = new DataType("com.google.distance.delta", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", FIELD_DISTANCE);
    public static final DataType TYPE_HEART_POINTS = new DataType("com.google.heart_minutes", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_INTENSITY);
    public static final DataType TYPE_HEART_RATE_BPM = new DataType("com.google.heart_rate.bpm", "https://www.googleapis.com/auth/fitness.heart_rate.read", "https://www.googleapis.com/auth/fitness.heart_rate.write", FIELD_BPM);
    public static final DataType TYPE_HEIGHT = new DataType("com.google.height", "https://www.googleapis.com/auth/fitness.body.read", "https://www.googleapis.com/auth/fitness.body.write", FIELD_HEIGHT);
    public static final DataType TYPE_HYDRATION = new DataType("com.google.hydration", "https://www.googleapis.com/auth/fitness.nutrition.read", "https://www.googleapis.com/auth/fitness.nutrition.write", FIELD_VOLUME);
    public static final DataType TYPE_LOCATION_SAMPLE = new DataType("com.google.location.sample", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", FIELD_LATITUDE, FIELD_LONGITUDE, FIELD_ACCURACY, FIELD_ALTITUDE);
    @Deprecated
    public static final DataType TYPE_LOCATION_TRACK = new DataType("com.google.location.track", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", FIELD_LATITUDE, FIELD_LONGITUDE, FIELD_ACCURACY, FIELD_ALTITUDE);
    public static final DataType TYPE_MOVE_MINUTES = new DataType("com.google.active_minutes", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_DURATION);
    public static final DataType TYPE_NUTRITION = new DataType("com.google.nutrition", "https://www.googleapis.com/auth/fitness.nutrition.read", "https://www.googleapis.com/auth/fitness.nutrition.write", FIELD_NUTRIENTS, FIELD_MEAL_TYPE, FIELD_FOOD_ITEM);
    public static final DataType TYPE_POWER_SAMPLE = new DataType("com.google.power.sample", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_WATTS);
    public static final DataType TYPE_SLEEP_SEGMENT = new DataType("com.google.sleep.segment", "https://www.googleapis.com/auth/fitness.sleep.read", "https://www.googleapis.com/auth/fitness.sleep.write", FIELD_SLEEP_SEGMENT_TYPE);
    public static final DataType TYPE_SPEED = new DataType("com.google.speed", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", FIELD_SPEED);
    public static final DataType TYPE_STEP_COUNT_CADENCE = new DataType("com.google.step_count.cadence", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_RPM);
    public static final DataType TYPE_STEP_COUNT_DELTA = new DataType("com.google.step_count.delta", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_STEPS);
    public static final DataType TYPE_WEIGHT = new DataType("com.google.weight", "https://www.googleapis.com/auth/fitness.body.read", "https://www.googleapis.com/auth/fitness.body.write", FIELD_WEIGHT);
    public static final DataType TYPE_WORKOUT_EXERCISE = new DataType("com.google.activity.exercise", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_EXERCISE, FIELD_REPETITIONS, FIELD_DURATION_OPTIONAL, FIELD_RESISTANCE_TYPE, FIELD_RESISTANCE);

    public static final DataType TYPE_DEVICE_ON_BODY = new DataType("com.google.device_on_body", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_PROBABILITY);
    public static final DataType TYPE_INTERNAL_GOAL = new DataType("com.google.internal.goal", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_FITNESS_GOAL_V2);
    public static final DataType TYPE_MET = new DataType("com.google.internal.met", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", FIELD_MET);
    public static final DataType TYPE_PACED_WALKING_ATTRIBUTES = new DataType("com.google.internal.paced_walking_attributes", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_FITNESS_PACED_WALKING_ATTRIBUTES);
    public static final DataType TYPE_RESPIRATORY_RATE = new DataType("com.google.respiratory_rate", "https://www.googleapis.com/auth/fitness.respiratory_rate.read", "https://www.googleapis.com/auth/fitness.respiratory_rate.write", FIELD_RESPIRATORY_RATE);
    public static final DataType TYPE_SENSOR_EVENTS = new DataType("com.google.sensor.events", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_SENSOR_TYPE, FIELD_TIMESTAMPS, FIELD_SENSOR_VALUES);
    public static final DataType TYPE_SLEEP_ATTRIBUTES = new DataType("com.google.internal.sleep_attributes", "https://www.googleapis.com/auth/fitness.sleep.read", "https://www.googleapis.com/auth/fitness.sleep.write", FIELD_FITNESS_SLEEP_ATTRIBUTES);
    public static final DataType TYPE_SLEEP_SCHEDULE = new DataType("com.google.internal.sleep_schedule", "https://www.googleapis.com/auth/fitness.sleep.read", "https://www.googleapis.com/auth/fitness.sleep.write", FIELD_FITNESS_SLEEP_SCHEDULE);
    public static final DataType TYPE_STEP_COUNT_CUMULATIVE = new DataType("com.google.step_count.cumulative", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_STEPS);
    public static final DataType TYPE_TIME_ZONE_CHANGE = new DataType("com.google.time_zone_change", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", FIELD_ZONE_ID);
    public static final DataType TYPE_WORKOUT_SAMPLES = new DataType("com.google.activity.samples", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_ACTIVITY_CONFIDENCE);


    public static final DataType AGGREGATE_ACTIVITY_SUMMARY = new DataType("com.google.activity.summary", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_ACTIVITY, FIELD_DURATION, FIELD_NUM_SEGMENTS);
    public static final DataType AGGREGATE_BASAL_METABOLIC_RATE_SUMMARY = new DataType("com.google.calories.bmr.summary", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_AVERAGE, FIELD_MAX, FIELD_MIN);
    public static final DataType AGGREGATE_BODY_FAT_PERCENTAGE_SUMMARY = new DataType("com.google.body.fat.percentage.summary", "https://www.googleapis.com/auth/fitness.body.read", "https://www.googleapis.com/auth/fitness.body.write", FIELD_AVERAGE, FIELD_MAX, FIELD_MIN);
    public static final DataType AGGREGATE_CALORIES_EXPENDED = TYPE_CALORIES_EXPENDED;
    public static final DataType AGGREGATE_DISTANCE_DELTA = TYPE_DISTANCE_DELTA;
    public static final DataType AGGREGATE_HEART_POINTS = new DataType("com.google.heart_minutes.summary", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_INTENSITY, FIELD_DURATION);
    public static final DataType AGGREGATE_HEART_RATE_SUMMARY = new DataType("com.google.heart_rate.summary", "https://www.googleapis.com/auth/fitness.heart_rate.read", "https://www.googleapis.com/auth/fitness.heart_rate.write", FIELD_AVERAGE, FIELD_MAX, FIELD_MIN);
    public static final DataType AGGREGATE_HEIGHT_SUMMARY = new DataType("com.google.height.summary", "https://www.googleapis.com/auth/fitness.body.read", "https://www.googleapis.com/auth/fitness.body.write", FIELD_AVERAGE, FIELD_MAX, FIELD_MIN);
    public static final DataType AGGREGATE_HYDRATION = TYPE_HYDRATION;
    public static final DataType AGGREGATE_LOCATION_BOUNDING_BOX = new DataType("com.google.location.bounding_box", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", FIELD_LOW_LATITUDE, FIELD_LOW_LONGITUDE, FIELD_HIGH_LATITUDE, FIELD_HIGH_LONGITUDE);
    public static final DataType AGGREGATE_MOVE_MINUTES = TYPE_MOVE_MINUTES;
    public static final DataType AGGREGATE_NUTRITION_SUMMARY = new DataType("com.google.nutrition.summary", "https://www.googleapis.com/auth/fitness.nutrition.read", "https://www.googleapis.com/auth/fitness.nutrition.write", FIELD_NUTRIENTS, FIELD_MEAL_TYPE);
    public static final DataType AGGREGATE_POWER_SUMMARY = new DataType("com.google.power.summary", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", FIELD_AVERAGE, FIELD_MAX, FIELD_MIN);
    public static final DataType AGGREGATE_SPEED_SUMMARY = new DataType("com.google.speed.summary", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", FIELD_AVERAGE, FIELD_MAX, FIELD_MIN);
    public static final DataType AGGREGATE_STEP_COUNT_DELTA = TYPE_STEP_COUNT_DELTA;
    public static final DataType AGGREGATE_WEIGHT_SUMMARY = new DataType("com.google.weight.summary", "https://www.googleapis.com/auth/fitness.body.read", "https://www.googleapis.com/auth/fitness.body.write", FIELD_AVERAGE, FIELD_MAX, FIELD_MIN);

    @Field(value = 1, getterName = "getName")
    @NonNull
    private final String name;
    @Field(value = 2, getterName = "getFields")
    @NonNull
    private final List<com.google.android.gms.fitness.data.Field> fields;
    @Field(3)
    @Nullable
    final String readScope;
    @Field(4)
    @Nullable
    final String writeScope;

    DataType(@NonNull String name, @Nullable String readScope, @Nullable String writeScope, com.google.android.gms.fitness.data.Field... fields) {
        this.name = name;
        this.readScope = readScope;
        this.writeScope = writeScope;
        this.fields = Collections.unmodifiableList(Arrays.asList(fields));
    }

    @Constructor
    DataType(@Param(1) @NonNull String name, @Param(2) @NonNull List<com.google.android.gms.fitness.data.Field> fields, @Param(3) @Nullable String readScope, @Param(4) @Nullable String writeScope) {
        this.name = name;
        this.fields = fields;
        this.readScope = readScope;
        this.writeScope = writeScope;
    }

    /**
     * Returns the aggregate output type for this type, or {@code null} if the type does not support aggregation.
     * <p>
     * To check if a data type is supported for aggregation, check that the returned type is non-null.
     */
    @Nullable
    public DataType getAggregateType() {
        // TODO
        return null;
    }

    /**
     * Returns the ordered list of fields for the data type.
     */
    @NonNull
    public List<com.google.android.gms.fitness.data.Field> getFields() {
        return fields;
    }

    /**
     * Returns the namespaced name which uniquely identifies this data type.
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns the index of a field.
     *
     * @throws IllegalArgumentException If field isn't defined for this data type.
     */
    public int indexOf(@NonNull com.google.android.gms.fitness.data.Field field) {
        int indexOf = this.fields.indexOf(field);
        if (indexOf < 0) throw new IllegalArgumentException("Field not found");
        return indexOf;
    }

    /**
     * Returns a list of output aggregate data types for the specified {@code inputDataType}.
     * <p>
     * To check if a data type is supported for aggregation, check that the returned list is not empty
     * {@code DataType.getAggregatesForInput(dataType).isEmpty()}.
     *
     * @deprecated Use {@link #getAggregateType()} instead.
     */
    @NonNull
    @Deprecated
    public static List<DataType> getAggregatesForInput(@NonNull DataType inputDataType) {
        DataType aggregateType = inputDataType.getAggregateType();
        if (aggregateType == null) return Collections.emptyList();
        return Collections.singletonList(aggregateType);
    }

    /**
     * Returns the MIME type for a particular {@link DataType}. The MIME type is used in intents such as the data view intent.
     */
    @NonNull
    public static String getMimeType(@NonNull DataType dataType) {
        return MIME_TYPE_PREFIX + dataType.getName();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataType> CREATOR = findCreator(DataType.class);

}
