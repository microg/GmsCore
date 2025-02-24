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

@SafeParcelable.Class
public class FaceLandmark extends AbstractSafeParcelable {

    /**
     * the midpoint between the subject's left mouth corner and the outer corner of the subject's left eye.
     * for full profile faces, this becomes the centroid of the nose base, nose tip, left ear lobe and left ear tip.
     */
    public static final int LEFT_CHEEK = 1;
    /**
     * The midpoint of the subject's left ear tip and left ear lobe.
     */
    public static final int LEFT_EAR = 3;
    /**
     * The center of the subject's left eye cavity.
     */
    public static final int LEFT_EYE = 4;
    /**
     * The center of the subject's bottom lip.
     */
    public static final int MOUTH_BOTTOM = 0;
    /**
     * The subject's left mouth corner where the lips meet.
     */
    public static final int MOUTH_LEFT = 5;
    /**
     * The subject's right mouth corner where the lips meet.
     */
    public static final int MOUTH_RIGHT = 11;
    /**
     * The midpoint between the subject's nostrils where the nose meets the face.
     */
    public static final int NOSE_BASE = 6;
    /**
     * The midpoint between the subject's right mouth corner and the outer corner of the subject's right eye.
     * For full profile faces, this becomes the centroid of the nose base, nose tip, right ear lobe and right ear tip.
     */
    public static final int RIGHT_CHEEK = 7;
    /**
     * The midpoint of the subject's right ear tip and right ear lobe.
     */
    public static final int RIGHT_EAR = 9;
    /**
     * The center of the subject's right eye cavity.
     */
    public static final int RIGHT_EYE = 10;

    @Retention(RetentionPolicy.CLASS)
    @IntDef(value = {LEFT_CHEEK, LEFT_EAR, LEFT_EYE, MOUTH_BOTTOM, MOUTH_LEFT, MOUTH_RIGHT, NOSE_BASE, RIGHT_CHEEK, RIGHT_EAR, RIGHT_EYE})
    public @interface LandmarkType {
    }

    @Field(value = 1, getterName = "getLandmarkType")
    private final @LandmarkType int type;
    @Field(value = 2, getterName = "getPosition")
    private final PointF position;

    @Constructor
    public FaceLandmark(@Param(1) @LandmarkType int type, @Param(2) PointF position) {
        this.type = type;
        this.position = position;
    }

    @FaceLandmark.LandmarkType
    public int getLandmarkType() {
        return type;
    }

    public PointF getPosition() {
        return position;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FaceLandmark> CREATOR = findCreator(FaceLandmark.class);

}
