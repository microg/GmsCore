/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SafeParcelable.Class
public class DataType extends AbstractSafeParcelable {

    public static final DataType TYPE_STEP_COUNT_DELTA = new DataType("com.google.step_count.delta", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_STEPS);
    public static final DataType TYPE_STEP_COUNT_CUMULATIVE = new DataType("com.google.step_count.cumulative", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_STEPS);
    public static final DataType TYPE_STEP_COUNT_CADENCE = new DataType("com.google.step_count.cadence", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_RPM);
    public static final DataType TYPE_INTERNAL_GOAL = new DataType("com.google.internal.goal", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_FITNESS_GOAL_V2);
    public static final DataType TYPE_ACTIVITY_SEGMENT = new DataType("com.google.activity.segment", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_ACTIVITY);
    public static final DataType TYPE_SLEEP_SEGMENT = new DataType("com.google.sleep.segment", "https://www.googleapis.com/auth/fitness.sleep.read", "https://www.googleapis.com/auth/fitness.sleep.write", com.google.android.gms.fitness.data.Field.FIELD_SLEEP_SEGMENT_TYPE);
    public static final DataType TYPE_CALORIES_EXPENDED = new DataType("com.google.calories.expended", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_CALORIES);
    public static final DataType TYPE_BASAL_METABOLIC_RATE = new DataType("com.google.calories.bmr", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_CALORIES);
    public static final DataType TYPE_POWER_SAMPLE = new DataType("com.google.power.sample", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_WATTS);
    public static final DataType TYPE_SENSOR_EVENTS = new DataType("com.google.sensor.events", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_SENSOR_TYPE, com.google.android.gms.fitness.data.Field.FIELD_TIMESTAMPS, com.google.android.gms.fitness.data.Field.FIELD_SENSOR_VALUES);
    public static final DataType TYPE_HEART_RATE_BPM = new DataType("com.google.heart_rate.bpm", "https://www.googleapis.com/auth/fitness.heart_rate.read", "https://www.googleapis.com/auth/fitness.heart_rate.write", com.google.android.gms.fitness.data.Field.FIELD_BPM);
    public static final DataType TYPE_RESPIRATORY_RATE = new DataType("com.google.respiratory_rate", "https://www.googleapis.com/auth/fitness.respiratory_rate.read", "https://www.googleapis.com/auth/fitness.respiratory_rate.write", com.google.android.gms.fitness.data.Field.FIELD_RESPIRATORY_RATE);
    public static final DataType TYPE_LOCATION_SAMPLE = new DataType("com.google.location.sample", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", com.google.android.gms.fitness.data.Field.FIELD_LATITUDE, com.google.android.gms.fitness.data.Field.FIELD_LONGITUDE, com.google.android.gms.fitness.data.Field.FIELD_ACCURACY, com.google.android.gms.fitness.data.Field.FIELD_ALTITUDE);
    @Deprecated
    public static final DataType TYPE_LOCATION_TRACK = new DataType("com.google.location.track", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", com.google.android.gms.fitness.data.Field.FIELD_LATITUDE, com.google.android.gms.fitness.data.Field.FIELD_LONGITUDE, com.google.android.gms.fitness.data.Field.FIELD_ACCURACY, com.google.android.gms.fitness.data.Field.FIELD_ALTITUDE);
    public static final DataType TYPE_DISTANCE_DELTA = new DataType("com.google.distance.delta", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", com.google.android.gms.fitness.data.Field.FIELD_DISTANCE);
    public static final DataType TYPE_SPEED = new DataType("com.google.speed", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", com.google.android.gms.fitness.data.Field.FIELD_SPEED);
    public static final DataType TYPE_CYCLING_WHEEL_REVOLUTION = new DataType("com.google.cycling.wheel_revolution.cumulative", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", com.google.android.gms.fitness.data.Field.FIELD_REVOLUTIONS);
    public static final DataType TYPE_CYCLING_WHEEL_RPM = new DataType("com.google.cycling.wheel_revolution.rpm", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", com.google.android.gms.fitness.data.Field.FIELD_RPM);
    public static final DataType TYPE_CYCLING_PEDALING_CUMULATIVE = new DataType("com.google.cycling.pedaling.cumulative", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_REVOLUTIONS);
    public static final DataType TYPE_CYCLING_PEDALING_CADENCE = new DataType("com.google.cycling.pedaling.cadence", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_RPM);
    public static final DataType TYPE_HEIGHT = new DataType("com.google.height", "https://www.googleapis.com/auth/fitness.body.read", "https://www.googleapis.com/auth/fitness.body.write", com.google.android.gms.fitness.data.Field.FIELD_HEIGHT);
    public static final DataType TYPE_WEIGHT = new DataType("com.google.weight", "https://www.googleapis.com/auth/fitness.body.read", "https://www.googleapis.com/auth/fitness.body.write", com.google.android.gms.fitness.data.Field.FIELD_WEIGHT);
    public static final DataType TYPE_BODY_FAT_PERCENTAGE = new DataType("com.google.body.fat.percentage", "https://www.googleapis.com/auth/fitness.body.read", "https://www.googleapis.com/auth/fitness.body.write", com.google.android.gms.fitness.data.Field.FIELD_PERCENTAGE);
    public static final DataType TYPE_NUTRITION = new DataType("com.google.nutrition", "https://www.googleapis.com/auth/fitness.nutrition.read", "https://www.googleapis.com/auth/fitness.nutrition.write", com.google.android.gms.fitness.data.Field.FIELD_NUTRIENTS, com.google.android.gms.fitness.data.Field.FIELD_MEAL_TYPE, com.google.android.gms.fitness.data.Field.FIELD_FOOD_ITEM);
    public static final DataType AGGREGATE_HYDRATION = new DataType("com.google.hydration", "https://www.googleapis.com/auth/fitness.nutrition.read", "https://www.googleapis.com/auth/fitness.nutrition.write", com.google.android.gms.fitness.data.Field.FIELD_VOLUME);
    public static final DataType TYPE_WORKOUT_EXERCISE = new DataType("com.google.activity.exercise", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_EXERCISE, com.google.android.gms.fitness.data.Field.FIELD_REPETITIONS, com.google.android.gms.fitness.data.Field.FIELD_DURATION_OPTIONAL, com.google.android.gms.fitness.data.Field.FIELD_RESISTANCE_TYPE, com.google.android.gms.fitness.data.Field.FIELD_RESISTANCE);
    public static final DataType TYPE_MOVE_MINUTES = new DataType("com.google.active_minutes", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_DURATION);
    public static final DataType AGGREGATE_MOVE_MINUTES = TYPE_MOVE_MINUTES;
    public static final DataType TYPE_DEVICE_ON_BODY = new DataType("com.google.device_on_body", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_PROBABILITY);
    public static final DataType AGGREGATE_ACTIVITY_SUMMARY = new DataType("com.google.activity.summary", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_ACTIVITY, com.google.android.gms.fitness.data.Field.FIELD_DURATION, com.google.android.gms.fitness.data.Field.FIELD_NUM_SEGMENTS);
    public static final DataType AGGREGATE_BASAL_METABOLIC_RATE_SUMMARY = new DataType("com.google.calories.bmr.summary", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_AVERAGE, com.google.android.gms.fitness.data.Field.FIELD_MAX, com.google.android.gms.fitness.data.Field.FIELD_MIN);
    public static final DataType AGGREGATE_STEP_COUNT_DELTA = TYPE_STEP_COUNT_DELTA;
    public static final DataType AGGREGATE_DISTANCE_DELTA = TYPE_DISTANCE_DELTA;
    public static final DataType AGGREGATE_CALORIES_EXPENDED = TYPE_CALORIES_EXPENDED;
    public static final DataType TYPE_HEART_POINTS = new DataType("com.google.heart_minutes", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_INTENSITY);
    public static final DataType AGGREGATE_HEART_POINTS = new DataType("com.google.heart_minutes.summary", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_INTENSITY, com.google.android.gms.fitness.data.Field.FIELD_DURATION);
    public static final DataType AGGREGATE_HEART_RATE_SUMMARY = new DataType("com.google.heart_rate.summary", "https://www.googleapis.com/auth/fitness.heart_rate.read", "https://www.googleapis.com/auth/fitness.heart_rate.write", com.google.android.gms.fitness.data.Field.FIELD_AVERAGE, com.google.android.gms.fitness.data.Field.FIELD_MAX, com.google.android.gms.fitness.data.Field.FIELD_MIN);
    public static final DataType AGGREGATE_LOCATION_BOUNDING_BOX = new DataType("com.google.location.bounding_box", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", com.google.android.gms.fitness.data.Field.FIELD_LOW_LATITUDE, com.google.android.gms.fitness.data.Field.FIELD_LOW_LONGITUDE, com.google.android.gms.fitness.data.Field.FIELD_HIGH_LATITUDE, com.google.android.gms.fitness.data.Field.FIELD_HIGH_LONGITUDE);
    public static final DataType AGGREGATE_POWER_SUMMARY = new DataType("com.google.power.summary", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_AVERAGE, com.google.android.gms.fitness.data.Field.FIELD_MAX, com.google.android.gms.fitness.data.Field.FIELD_MIN);
    public static final DataType AGGREGATE_SPEED_SUMMARY = new DataType("com.google.speed.summary", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", com.google.android.gms.fitness.data.Field.FIELD_AVERAGE, com.google.android.gms.fitness.data.Field.FIELD_MAX, com.google.android.gms.fitness.data.Field.FIELD_MIN);
    public static final DataType AGGREGATE_BODY_FAT_PERCENTAGE_SUMMARY = new DataType("com.google.body.fat.percentage.summary", "https://www.googleapis.com/auth/fitness.body.read", "https://www.googleapis.com/auth/fitness.body.write", com.google.android.gms.fitness.data.Field.FIELD_AVERAGE, com.google.android.gms.fitness.data.Field.FIELD_MAX, com.google.android.gms.fitness.data.Field.FIELD_MIN);
    public static final DataType AGGREGATE_WEIGHT_SUMMARY = new DataType("com.google.weight.summary", "https://www.googleapis.com/auth/fitness.body.read", "https://www.googleapis.com/auth/fitness.body.write", com.google.android.gms.fitness.data.Field.FIELD_AVERAGE, com.google.android.gms.fitness.data.Field.FIELD_MAX, com.google.android.gms.fitness.data.Field.FIELD_MIN);
    public static final DataType AGGREGATE_HEIGHT_SUMMARY = new DataType("com.google.height.summary", "https://www.googleapis.com/auth/fitness.body.read", "https://www.googleapis.com/auth/fitness.body.write", com.google.android.gms.fitness.data.Field.FIELD_AVERAGE, com.google.android.gms.fitness.data.Field.FIELD_MAX, com.google.android.gms.fitness.data.Field.FIELD_MIN);
    public static final DataType AGGREGATE_NUTRITION_SUMMARY = new DataType("com.google.nutrition.summary", "https://www.googleapis.com/auth/fitness.nutrition.read", "https://www.googleapis.com/auth/fitness.nutrition.write", com.google.android.gms.fitness.data.Field.FIELD_NUTRIENTS, com.google.android.gms.fitness.data.Field.FIELD_MEAL_TYPE);
    public static final DataType TYPE_HYDRATION = AGGREGATE_HYDRATION;
    public static final DataType TYPE_WORKOUT_SAMPLES = new DataType("com.google.activity.samples", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_ACTIVITY_CONFIDENCE);
    public static final DataType TYPE_SLEEP_ATTRIBUTES = new DataType("com.google.internal.sleep_attributes", "https://www.googleapis.com/auth/fitness.sleep.read", "https://www.googleapis.com/auth/fitness.sleep.write", com.google.android.gms.fitness.data.Field.FIELD_FITNESS_SLEEP_ATTRIBUTES);
    public static final DataType TYPE_SLEEP_SCHEDULE = new DataType("com.google.internal.sleep_schedule", "https://www.googleapis.com/auth/fitness.sleep.read", "https://www.googleapis.com/auth/fitness.sleep.write", com.google.android.gms.fitness.data.Field.FIELD_FITNESS_SLEEP_SCHEDULE);
    public static final DataType TYPE_PACED_WALKING_ATTRIBUTES = new DataType("com.google.internal.paced_walking_attributes", "https://www.googleapis.com/auth/fitness.activity.read", "https://www.googleapis.com/auth/fitness.activity.write", com.google.android.gms.fitness.data.Field.FIELD_FITNESS_PACED_WALKING_ATTRIBUTES);
    public static final DataType TYPE_TIME_ZONE_CHANGE = new DataType("com.google.time_zone_change", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", com.google.android.gms.fitness.data.Field.FIELD_ZONE_ID);
    public static final DataType TYPE_MET = new DataType("com.google.internal.met", "https://www.googleapis.com/auth/fitness.location.read", "https://www.googleapis.com/auth/fitness.location.write", com.google.android.gms.fitness.data.Field.FIELD_MET);

    @Field(1)
    public String packageName;
    @Field(2)
    public List<com.google.android.gms.fitness.data.Field> fields;
    @Field(3)
    public String name;
    @Field(4)
    public String value;

    public DataType(String packageName, String name, String value, com.google.android.gms.fitness.data.Field... fieldArr) {
        this.packageName = packageName;
        this.fields = Collections.unmodifiableList(Arrays.asList(fieldArr));
        this.name = name;
        this.value = value;
    }

    public DataType() {
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataType> CREATOR = findCreator(DataType.class);

}
