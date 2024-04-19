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

@SafeParcelable.Class
public class Field extends AbstractSafeParcelable {

    public static final com.google.android.gms.fitness.data.Field FIELD_ACTIVITY = formatIntField("activity");
    public static final com.google.android.gms.fitness.data.Field FIELD_SLEEP_SEGMENT_TYPE = formatIntField("sleep_segment_type");
    public static final com.google.android.gms.fitness.data.Field FIELD_CONFIDENCE = formatFloatField("confidence");
    public static final com.google.android.gms.fitness.data.Field FIELD_STEPS = formatIntField("steps");
    @Deprecated
    public static final com.google.android.gms.fitness.data.Field FIELD_STEP_LENGTH = formatFloatField("step_length");
    public static final com.google.android.gms.fitness.data.Field FIELD_DURATION = formatIntField("duration");
    public static final com.google.android.gms.fitness.data.Field FIELD_DURATION_OPTIONAL = formatIntOptionalField("duration");
    public static final com.google.android.gms.fitness.data.Field FIELD_ACTIVITY_DURATION_ASCENDING = formatMapField("activity_duration.ascending");
    public static final com.google.android.gms.fitness.data.Field FIELD_ACTIVITY_DURATION_DESCENDING = formatMapField("activity_duration.descending");
    public static final com.google.android.gms.fitness.data.Field FIELD_BPM = formatFloatField("bpm");
    public static final com.google.android.gms.fitness.data.Field FIELD_RESPIRATORY_RATE = formatFloatField("respiratory_rate");
    public static final com.google.android.gms.fitness.data.Field FIELD_LATITUDE = formatFloatField("latitude");
    public static final com.google.android.gms.fitness.data.Field FIELD_LONGITUDE = formatFloatField("longitude");
    public static final com.google.android.gms.fitness.data.Field FIELD_ACCURACY = formatFloatField("accuracy");
    public static final com.google.android.gms.fitness.data.Field FIELD_ALTITUDE = formatFloatOptionalField("altitude");
    public static final com.google.android.gms.fitness.data.Field FIELD_DISTANCE = formatFloatField("distance");
    public static final com.google.android.gms.fitness.data.Field FIELD_HEIGHT = formatFloatField("height");
    public static final com.google.android.gms.fitness.data.Field FIELD_WEIGHT = formatFloatField("weight");
    public static final com.google.android.gms.fitness.data.Field FIELD_PERCENTAGE = formatFloatField("percentage");
    public static final com.google.android.gms.fitness.data.Field FIELD_SPEED = formatFloatField("speed");
    public static final com.google.android.gms.fitness.data.Field FIELD_RPM = formatFloatField("rpm");
    public static final com.google.android.gms.fitness.data.Field FIELD_FITNESS_GOAL_V2 = formatObjectField("google.android.fitness.GoalV2");
    public static final com.google.android.gms.fitness.data.Field FIELD_FITNESS_DEVICE = formatObjectField("google.android.fitness.Device");
    public static final com.google.android.gms.fitness.data.Field FIELD_REVOLUTIONS = formatIntField("revolutions");
    public static final com.google.android.gms.fitness.data.Field FIELD_CALORIES = formatFloatField("calories");
    public static final com.google.android.gms.fitness.data.Field FIELD_WATTS = formatFloatField("watts");
    public static final com.google.android.gms.fitness.data.Field FIELD_VOLUME = formatFloatField("volume");
    public static final com.google.android.gms.fitness.data.Field FIELD_MEAL_TYPE = formatIntOptionalField("meal_type");
    public static final com.google.android.gms.fitness.data.Field FIELD_FOOD_ITEM = formatStringOptionalField("food_item");
    public static final com.google.android.gms.fitness.data.Field FIELD_NUTRIENTS = formatMapField("nutrients");
    public static final com.google.android.gms.fitness.data.Field FIELD_EXERCISE = formatStringField("exercise");
    public static final com.google.android.gms.fitness.data.Field FIELD_REPETITIONS = formatIntOptionalField("repetitions");
    public static final com.google.android.gms.fitness.data.Field FIELD_RESISTANCE = formatFloatOptionalField("resistance");
    public static final com.google.android.gms.fitness.data.Field FIELD_RESISTANCE_TYPE = formatIntOptionalField("resistance_type");
    public static final com.google.android.gms.fitness.data.Field FIELD_NUM_SEGMENTS = formatIntField("num_segments");
    public static final com.google.android.gms.fitness.data.Field FIELD_AVERAGE = formatFloatField("average");
    public static final com.google.android.gms.fitness.data.Field FIELD_MAX = formatFloatField("max");
    public static final com.google.android.gms.fitness.data.Field FIELD_MIN = formatFloatField("min");
    public static final com.google.android.gms.fitness.data.Field FIELD_LOW_LATITUDE = formatFloatField("low_latitude");
    public static final com.google.android.gms.fitness.data.Field FIELD_LOW_LONGITUDE = formatFloatField("low_longitude");
    public static final com.google.android.gms.fitness.data.Field FIELD_HIGH_LATITUDE = formatFloatField("high_latitude");
    public static final com.google.android.gms.fitness.data.Field FIELD_HIGH_LONGITUDE = formatFloatField("high_longitude");
    public static final com.google.android.gms.fitness.data.Field FIELD_OCCURRENCES = formatIntField("occurrences");
    public static final com.google.android.gms.fitness.data.Field FIELD_SENSOR_TYPE = formatIntField("sensor_type");
    public static final com.google.android.gms.fitness.data.Field FIELD_TIMESTAMPS = formatLongField("timestamps");
    public static final com.google.android.gms.fitness.data.Field FIELD_SENSOR_VALUES = formatDoubleField("sensor_values");
    public static final com.google.android.gms.fitness.data.Field FIELD_INTENSITY = formatFloatField("intensity");
    public static final com.google.android.gms.fitness.data.Field FIELD_ACTIVITY_CONFIDENCE = formatMapField("activity_confidence");
    public static final com.google.android.gms.fitness.data.Field FIELD_PROBABILITY = formatFloatField("probability");
    public static final com.google.android.gms.fitness.data.Field FIELD_FITNESS_SLEEP_ATTRIBUTES = formatObjectField("google.android.fitness.SleepAttributes");
    public static final com.google.android.gms.fitness.data.Field FIELD_FITNESS_SLEEP_SCHEDULE = formatObjectField("google.android.fitness.SleepSchedule");
    @Deprecated
    public static final com.google.android.gms.fitness.data.Field FIELD_CIRCUMFERENCE = formatFloatField("circumference");
    public static final com.google.android.gms.fitness.data.Field FIELD_FITNESS_PACED_WALKING_ATTRIBUTES = formatObjectField("google.android.fitness.PacedWalkingAttributes");
    public static final com.google.android.gms.fitness.data.Field FIELD_ZONE_ID = formatStringField("zone_id");
    public static final com.google.android.gms.fitness.data.Field FIELD_MET = formatFloatField("met");

    public static final int FORMAT_INT32 = 1;
    public static final int FORMAT_FLOAT = 2;
    public static final int FORMAT_STRING = 3;
    public static final int FORMAT_MAP = 4;
    public static final int FORMAT_LONG = 5;
    public static final int FORMAT_DOUBLE = 6;
    public static final int FORMAT_OBJECT = 7;

    @Field(1)
    public String name;
    @Field(2)
    public int format;
    @Field(3)
    public Boolean optional;

    public Field() {
    }

    public Field(String name, int format, Boolean optional) {
        this.name = name;
        this.format = format;
        this.optional = optional;
    }

    public Field(String name, int format) {
        this(name, format, null);
    }

    public static com.google.android.gms.fitness.data.Field formatIntField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_INT32);
    }

    public static com.google.android.gms.fitness.data.Field formatFloatField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_FLOAT);
    }

    public static com.google.android.gms.fitness.data.Field formatStringField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_STRING);
    }

    public static com.google.android.gms.fitness.data.Field formatMapField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_MAP);
    }

    public static com.google.android.gms.fitness.data.Field formatLongField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_LONG);
    }

    public static com.google.android.gms.fitness.data.Field formatDoubleField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_DOUBLE);
    }

    public static com.google.android.gms.fitness.data.Field formatObjectField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_OBJECT);
    }

    public static com.google.android.gms.fitness.data.Field formatIntOptionalField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_INT32, true);
    }

    public static com.google.android.gms.fitness.data.Field formatFloatOptionalField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_FLOAT, true);
    }

    public static com.google.android.gms.fitness.data.Field formatStringOptionalField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_STRING, true);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<com.google.android.gms.fitness.data.Field> CREATOR = findCreator(com.google.android.gms.fitness.data.Field.class);

}
