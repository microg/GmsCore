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
import org.microg.gms.common.Hide;

/**
 * A field represents one dimension of a data type. It defines the name and format of data. Unlike data type names, field names are not
 * namespaced, and only need to be unique within the data type.
 * <p>
 * This class contains constants representing the field names of common data types, each prefixed with {@code FIELD_}. These can be used to
 * access and set the fields via {@link DataPoint#getValue(com.google.android.gms.fitness.data.Field)}.
 * <p>
 * Fields for custom data types can be created using {@link DataTypeCreateRequest.Builder#addField(String, int)}.
 */
@SafeParcelable.Class
public class Field extends AbstractSafeParcelable {

    /**
     * Format constant indicating the field holds integer values.
     */
    public static final int FORMAT_INT32 = 1;
    /**
     * Format constant indicating the field holds float values.
     */
    public static final int FORMAT_FLOAT = 2;
    /**
     * Format constant indicating the field holds string values. Strings should be kept small whenever possible. Data streams with large string
     * values and high data frequency may be down sampled.
     */
    public static final int FORMAT_STRING = 3;
    /**
     * Format constant indicating the field holds a map of string keys to values. The valid key space and units for the corresponding value should
     * be documented as part of the data type definition.
     * <p>
     * Map values can be set using {@link DataPoint.Builder#setField(com.google.android.gms.fitness.data.Field, java.util.Map)}.
     * <p>
     * Keys should be kept small whenever possible. Data streams with large keys and high data frequency may be down sampled.
     */
    public static final int FORMAT_MAP = 4;

    public static final int FORMAT_LONG = 5;
    public static final int FORMAT_DOUBLE = 6;
    public static final int FORMAT_OBJECT = 7;

    /**
     * Meal type constant representing that the meal type is unknown.
     */
    public static final int MEAL_TYPE_UNKNOWN = 0;
    /**
     * Meal type constant representing a breakfast meal.
     */
    public static final int MEAL_TYPE_BREAKFAST = 1;
    /**
     * Meal type constant representing a lunch meal.
     */
    public static final int MEAL_TYPE_LUNCH = 2;
    /**
     * Meal type constant representing a dinner meal.
     */
    public static final int MEAL_TYPE_DINNER = 3;
    /**
     * Meal type constant representing a snack meal.
     */
    public static final int MEAL_TYPE_SNACK = 4;
    /**
     * Calcium amount in milligrams.
     */
    @NonNull
    public static final String NUTRIENT_CALCIUM = "calcium";
    /**
     * Calories in kcal.
     */
    @NonNull
    public static final String NUTRIENT_CALORIES = "calories";
    /**
     * Cholesterol in milligrams.
     */
    @NonNull
    public static final String NUTRIENT_CHOLESTEROL = "cholesterol";
    /**
     * Dietary fiber in grams.
     */
    @NonNull
    public static final String NUTRIENT_DIETARY_FIBER = "dietary_fiber";
    /**
     * Iron amount in milligrams.
     */
    @NonNull
    public static final String NUTRIENT_IRON = "iron";
    /**
     * Monounsaturated fat in grams.
     */
    @NonNull
    public static final String NUTRIENT_MONOUNSATURATED_FAT = "fat.monounsaturated";
    /**
     * Polyunsaturated fat in grams.
     */
    @NonNull
    public static final String NUTRIENT_POLYUNSATURATED_FAT = "fat.polyunsaturated";
    /**
     * Potassium in milligrams.
     */
    @NonNull
    public static final String NUTRIENT_POTASSIUM = "potassium";
    /**
     * Protein amount in grams.
     */
    @NonNull
    public static final String NUTRIENT_PROTEIN = "protein";
    /**
     * Saturated fat in grams.
     */
    @NonNull
    public static final String NUTRIENT_SATURATED_FAT = "fat.saturated";
    /**
     * Sodium in milligrams.
     */
    @NonNull
    public static final String NUTRIENT_SODIUM = "sodium";
    /**
     * Sugar amount in grams.
     */
    @NonNull
    public static final String NUTRIENT_SUGAR = "sugar";
    /**
     * Total carbohydrates in grams.
     */
    @NonNull
    public static final String NUTRIENT_TOTAL_CARBS = "carbs.total";
    /**
     * Total fat in grams.
     */
    @NonNull
    public static final String NUTRIENT_TOTAL_FAT = "fat.total";
    /**
     * Trans fat in grams.
     */
    @NonNull
    public static final String NUTRIENT_TRANS_FAT = "fat.trans";
    /**
     * Unsaturated fat in grams.
     */
    @NonNull
    public static final String NUTRIENT_UNSATURATED_FAT = "fat.unsaturated";
    /**
     * Vitamin A amount in International Units (IU). For converting from daily percentages, the FDA recommended 5000 IUs Daily Value can be
     * used.
     */
    @NonNull
    public static final String NUTRIENT_VITAMIN_A = "vitamin_a";
    /**
     * Vitamin C amount in milligrams.
     */
    @NonNull
    public static final String NUTRIENT_VITAMIN_C = "vitamin_c";
    /**
     * The resistance type is unknown, unspecified, or not represented by any canonical values.
     */
    public static final int RESISTANCE_TYPE_UNKNOWN = 0;
    /**
     * The user is using a barbell for resistance. The specified resistance should include the weight of the bar, as well as weights added to both
     * sides.
     */
    public static final int RESISTANCE_TYPE_BARBELL = 1;
    /**
     * The user is using a cable for resistance. When two cables are being used (one for each arm), the specified resistance should include the
     * weight being pulled by one cable.
     */
    public static final int RESISTANCE_TYPE_CABLE = 2;
    /**
     * The user is using dumbells for resistance. The specified resistance should include the weight of a single dumbell.
     */
    public static final int RESISTANCE_TYPE_DUMBBELL = 3;
    /**
     * The user is using a kettlebell for resistance.
     */
    public static final int RESISTANCE_TYPE_KETTLEBELL = 4;
    /**
     * The user is performing the exercise in a machine. The specified resistance should match the weight specified by the machine.
     */
    public static final int RESISTANCE_TYPE_MACHINE = 5;
    /**
     * The user is using their own body weight for resistance.
     */
    public static final int RESISTANCE_TYPE_BODY = 6;
    /**
     * The accuracy of an accompanied value (such as location).
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_ACCURACY = createFloatField("accuracy");
    /**
     * An activity type of {@link FitnessActivities}, encoded as an integer for efficiency. The activity value should be stored using
     * {@link DataPoint.Builder#setActivityField(Field, String)}.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_ACTIVITY = createIntField("activity");
    /**
     * An altitude of a location represented as a float, in meters above sea level. Some location samples don't have an altitude value so this field
     * might not be set.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_ALTITUDE = createOptionalFloatField("altitude");
    /**
     * An average value.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_AVERAGE = createFloatField("average");
    /**
     * A heart rate in beats per minute.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_BPM = createFloatField("bpm");
    /**
     * Calories in kcal.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_CALORIES = createFloatField("calories");
    /**
     * Circumference of a body part, in centimeters.
     *
     * @deprecated There is no applicable replacement field.
     */
    @Deprecated
    public static final com.google.android.gms.fitness.data.Field FIELD_CIRCUMFERENCE = createFloatField("circumference");
    /**
     * The confidence of an accompanied value, specified as a value between 0.0 and 100.0.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_CONFIDENCE = createFloatField("confidence");
    /**
     * A distance in meters.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_DISTANCE = createFloatField("distance");
    /**
     * A field containing duration. The units of the field are defined by the outer data type.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_DURATION = createIntField("duration");
    /**
     * A workout exercise, as represented by one of the constants in {@link WorkoutExercises}.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_EXERCISE = createStringField("exercise");
    /**
     * The corresponding food item for a nutrition entry.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_FOOD_ITEM = createOptionalStringField("food_item");
    /**
     * A height in meters.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_HEIGHT = createFloatField("height");
    /**
     * A high latitude of a location bounding box represented as a float, in degrees.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_HIGH_LATITUDE = createFloatField("high_latitude");
    /**
     * A high longitude of a location bounding box represented as a float, in degrees.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_HIGH_LONGITUDE = createFloatField("high_longitude");
    /**
     * Intensity of user activity, represented as a float.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_INTENSITY = createFloatField("intensity");
    /**
     * A latitude of a location represented as a float, in degrees.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_LATITUDE = createFloatField("latitude");
    /**
     * A longitude of a location represented as a float, in degrees.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_LONGITUDE = createFloatField("longitude");
    /**
     * A low latitude of a location bounding box represented as a float, in degrees.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_LOW_LATITUDE = createFloatField("low_latitude");
    /**
     * A low longitude of a location bounding box represented as a float, in degrees.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_LOW_LONGITUDE = createFloatField("low_longitude");
    /**
     * A maximum value.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_MAX = createFloatField("max");
    /**
     * A maximum int value.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_MAX_INT = createIntField("max");
    /**
     * Type of meal, represented as the appropriate int constant.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_MEAL_TYPE = createOptionalIntField("meal_type");
    /**
     * A minimum value.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_MIN = createFloatField("min");
    /**
     * A minimum int value.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_MIN_INT = createIntField("min");
    /**
     * A number of segments.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_NUM_SEGMENTS = createIntField("num_segments");
    /**
     * Nutrients ingested by the user, represented as a float map of nutrient key to quantity. The valid keys of the map are listed in this class using
     * the {@code NUTRIENT_} prefix. The documentation for each key describes the unit of its value.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_NUTRIENTS = createMapField("nutrients");
    /**
     * How many occurrences of an event there were in a time range. For sample data types this should not be set to more than one.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_OCCURRENCES = createIntField("occurrences");
    /**
     * A percentage value, between 0 and 100.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_PERCENTAGE = createFloatField("percentage");
    /**
     * A count of repetitions for a single set of a workout exercise.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_REPETITIONS = createOptionalIntField("repetitions");
    /**
     * The resistance of the exercise (or weight), in kg.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_RESISTANCE = createOptionalFloatField("resistance");
    /**
     * The type of resistance used in this exercise, represented as the appropriate int constant.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_RESISTANCE_TYPE = createOptionalIntField("resistance_type");
    /**
     * A count of revolutions.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_REVOLUTIONS = createIntField("revolutions");
    /**
     * Revolutions per minute or rate per minute.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_RPM = createFloatField("rpm");
    /**
     * Sleep Segment type defined in {@link SleepStages}.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_SLEEP_SEGMENT_TYPE = createIntField("sleep_segment_type");
    /**
     * A speed in meter/sec.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_SPEED = createFloatField("speed");
    /**
     * A count of steps.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_STEPS = createIntField("steps");
    /**
     * Distance between steps in meters.
     *
     * @deprecated There is no applicable replacement field.
     */
    @Deprecated
    public static final com.google.android.gms.fitness.data.Field FIELD_STEP_LENGTH = createFloatField("step_length");
    /**
     * Volume in liters.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_VOLUME = createFloatField("volume");
    /**
     * Power in watts.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_WATTS = createFloatField("watts");
    /**
     * A weight in kilograms.
     */
    public static final com.google.android.gms.fitness.data.Field FIELD_WEIGHT = createFloatField("weight");

    public static final com.google.android.gms.fitness.data.Field FIELD_ACTIVITY_CONFIDENCE = createMapField("activity_confidence");
    public static final com.google.android.gms.fitness.data.Field FIELD_ACTIVITY_DURATION_ASCENDING = createMapField("activity_duration.ascending");
    public static final com.google.android.gms.fitness.data.Field FIELD_ACTIVITY_DURATION_DESCENDING = createMapField("activity_duration.descending");
    public static final com.google.android.gms.fitness.data.Field FIELD_DURATION_OPTIONAL = createOptionalIntField("duration");
    public static final com.google.android.gms.fitness.data.Field FIELD_FITNESS_DEVICE = createObjectField("google.android.fitness.Device");
    public static final com.google.android.gms.fitness.data.Field FIELD_FITNESS_GOAL_V2 = createObjectField("google.android.fitness.GoalV2");
    public static final com.google.android.gms.fitness.data.Field FIELD_FITNESS_SLEEP_ATTRIBUTES = createObjectField("google.android.fitness.SleepAttributes");
    public static final com.google.android.gms.fitness.data.Field FIELD_FITNESS_SLEEP_SCHEDULE = createObjectField("google.android.fitness.SleepSchedule");
    public static final com.google.android.gms.fitness.data.Field FIELD_FITNESS_PACED_WALKING_ATTRIBUTES = createObjectField("google.android.fitness.PacedWalkingAttributes");
    public static final com.google.android.gms.fitness.data.Field FIELD_MET = createFloatField("met");
    public static final com.google.android.gms.fitness.data.Field FIELD_PROBABILITY = createFloatField("probability");
    public static final com.google.android.gms.fitness.data.Field FIELD_RESPIRATORY_RATE = createFloatField("respiratory_rate");
    public static final com.google.android.gms.fitness.data.Field FIELD_SENSOR_TYPE = createIntField("sensor_type");
    public static final com.google.android.gms.fitness.data.Field FIELD_SENSOR_VALUES = createDoubleField("sensor_values");
    public static final com.google.android.gms.fitness.data.Field FIELD_TIMESTAMPS = createLongField("timestamps");
    public static final com.google.android.gms.fitness.data.Field FIELD_ZONE_ID = createStringField("zone_id");

    @Field(value = 1, getterName = "getName")
    @NonNull
    private final String name;
    @Field(value = 2, getterName = "getFormat")
    private final int format;
    @Field(value = 3, getterName = "isOptional")
    @Nullable
    private final Boolean optional;

    @Constructor
    public Field(@Param(1) @NonNull String name, @Param(2) int format, @Param(3) @Nullable Boolean optional) {
        this.name = name;
        this.format = format;
        this.optional = optional;
    }

    public Field(@NonNull String name, int format) {
        this(name, format, null);
    }

    /**
     * Returns the format of the field, as one of the format constant values.
     */
    public int getFormat() {
        return format;
    }

    /**
     * Returns the name of the field.
     */
    public String getName() {
        return name;
    }

    public Boolean isOptional() {
        return optional;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof com.google.android.gms.fitness.data.Field)) return false;

        com.google.android.gms.fitness.data.Field field = (com.google.android.gms.fitness.data.Field) o;
        return format == field.format && name.equals(field.name);
    }

    public static com.google.android.gms.fitness.data.Field createIntField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_INT32);
    }

    public static com.google.android.gms.fitness.data.Field createFloatField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_FLOAT);
    }

    public static com.google.android.gms.fitness.data.Field createStringField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_STRING);
    }

    public static com.google.android.gms.fitness.data.Field createMapField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_MAP);
    }

    public static com.google.android.gms.fitness.data.Field createLongField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_LONG);
    }

    public static com.google.android.gms.fitness.data.Field createDoubleField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_DOUBLE);
    }

    public static com.google.android.gms.fitness.data.Field createObjectField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_OBJECT);
    }

    public static com.google.android.gms.fitness.data.Field createOptionalIntField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_INT32, true);
    }

    public static com.google.android.gms.fitness.data.Field createOptionalFloatField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_FLOAT, true);
    }

    public static com.google.android.gms.fitness.data.Field createOptionalStringField(String name) {
        return new com.google.android.gms.fitness.data.Field(name, FORMAT_STRING, true);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<com.google.android.gms.fitness.data.Field> CREATOR = findCreator(com.google.android.gms.fitness.data.Field.class);

}
