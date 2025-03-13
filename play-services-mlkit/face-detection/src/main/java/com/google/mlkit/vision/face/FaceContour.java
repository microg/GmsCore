/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.face;

import android.graphics.PointF;
import android.os.Parcel;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

@SafeParcelable.Class
public class FaceContour extends AbstractSafeParcelable {

    // The outline of the subject's face.
    public static final int FACE = 1;
    // The top outline of the subject's left eyebrow.
    public static final int LEFT_EYEBROW_TOP = 2;
    // The bottom outline of the subject's left eyebrow.
    public static final int LEFT_EYEBROW_BOTTOM = 3;
    // The top outline of the subject's right eyebrow.
    public static final int RIGHT_EYEBROW_TOP = 4;
    // The bottom outline of the subject's right eyebrow.
    public static final int RIGHT_EYEBROW_BOTTOM = 5;
    // The outline of the subject's left eye.
    public static final int LEFT_EYE = 6;
    // The outline of the subject's right eye.
    public static final int RIGHT_EYE = 7;
    // The top outline of the subject's upper lip.
    public static final int UPPER_LIP_TOP = 8;
    // The bottom outline of the subject's upper lip.
    public static final int UPPER_LIP_BOTTOM = 9;
    // The top outline of the subject's lower lip.
    public static final int LOWER_LIP_TOP = 10;
    // The bottom outline of the subject's lower lip.
    public static final int LOWER_LIP_BOTTOM = 11;
    // the outline of the subject's nose bridge.
    public static final int NOSE_BRIDGE = 12;
    // The outline of the subject's nose bridge.
    public static final int NOSE_BOTTOM = 13;
    // The center of the left cheek.
    public static final int LEFT_CHEEK = 14;
    // The center of the right cheek.
    public static final int RIGHT_CHEEK = 15;

    @Retention(RetentionPolicy.CLASS)
    @IntDef(value = {FACE, LEFT_EYEBROW_TOP, LEFT_EYEBROW_BOTTOM, RIGHT_EYEBROW_TOP, RIGHT_EYEBROW_BOTTOM, LEFT_EYE, RIGHT_EYE, UPPER_LIP_TOP, UPPER_LIP_BOTTOM, LOWER_LIP_TOP, LOWER_LIP_BOTTOM, NOSE_BRIDGE, NOSE_BOTTOM, LEFT_CHEEK, RIGHT_CHEEK})
    public @interface ContourType {
    }

    @Field(value = 1, getterName = "getFaceContourType")
    private final @ContourType int type;

    @Field(value = 2, getterName = "getPoints")
    private final List<PointF> points;

    @Constructor
    public FaceContour(@Param(1) int type, @Param(2) List<PointF> points) {
        this.type = type;
        this.points = points;
    }

    @FaceContour.ContourType
    public int getFaceContourType() {
        return type;
    }

    public List<PointF> getPoints() {
        return points;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FaceContour> CREATOR = findCreator(FaceContour.class);

}
