/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.face;

import android.os.Parcel;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

@SafeParcelable.Class
public class FaceDetectionOptions extends AbstractSafeParcelable {

    public static final int CLASSIFICATION_MODE_ALL = 2;
    public static final int CLASSIFICATION_MODE_NONE = 1;
    public static final int CONTOUR_MODE_ALL = 2;
    public static final int CONTOUR_MODE_NONE = 1;
    public static final int LANDMARK_MODE_ALL = 2;
    public static final int LANDMARK_MODE_NONE = 1;
    public static final int PERFORMANCE_MODE_ACCURATE = 2;
    public static final int PERFORMANCE_MODE_FAST = 1;

    @Retention(RetentionPolicy.CLASS)
    @IntDef(value = {CLASSIFICATION_MODE_NONE, CLASSIFICATION_MODE_ALL})
    public @interface ClassificationMode {
    }

    @Retention(RetentionPolicy.CLASS)
    @IntDef(value = {CONTOUR_MODE_NONE, CONTOUR_MODE_ALL})
    public @interface ContourMode {
    }

    @Retention(RetentionPolicy.CLASS)
    @IntDef(value = {LANDMARK_MODE_NONE, LANDMARK_MODE_ALL})
    public @interface LandmarkMode {
    }

    @Retention(RetentionPolicy.CLASS)
    @IntDef(value = {PERFORMANCE_MODE_FAST, PERFORMANCE_MODE_ACCURATE})
    public @interface PerformanceMode {
    }

    @Field(1)
    private final int landmarkMode;
    @Field(2)
    private final int contourMode;
    @Field(3)
    private final int classificationMode;
    @Field(4)
    private final int performanceMode;
    @Field(5)
    private final boolean trackingEnabled;
    @Field(6)
    private final float minFaceSize;
    private Executor executor;

    @Constructor
    FaceDetectionOptions(@Param(1) int landmarkMode, @Param(2) int contourMode, @Param(3) int classificationMode, @Param(4) int performanceMode, @Param(5) boolean trackingEnabled, @Param(6) float minFaceSize) {
        this.landmarkMode = landmarkMode;
        this.contourMode = contourMode;
        this.classificationMode = classificationMode;
        this.performanceMode = performanceMode;
        this.trackingEnabled = trackingEnabled;
        this.minFaceSize = minFaceSize;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public static class Builder {
        private int landmarkMode;
        private int contourMode;
        private int classificationMode;
        private int performanceMode;
        private boolean trackingEnabled;
        private float minFaceSize;
        private Executor executor;

        public Builder enableTracking(boolean enable) {
            this.trackingEnabled = enable;
            return this;
        }

        public Builder setClassificationMode(@ClassificationMode int mode) {
            this.classificationMode = mode;
            return this;
        }

        public Builder setContourMode(@ContourMode int mode) {
            this.contourMode = mode;
            return this;
        }

        public Builder setExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder setLandmarkMode(@LandmarkMode int mode) {
            this.landmarkMode = mode;
            return this;
        }

        public Builder setMinFaceSize(float minFaceSize) {
            this.minFaceSize = minFaceSize;
            return this;
        }

        public Builder setPerformanceMode(@PerformanceMode int mode) {
            this.performanceMode = mode;
            return this;
        }

        public FaceDetectionOptions build() {
            FaceDetectionOptions faceDetectionOptions = new FaceDetectionOptions(landmarkMode, contourMode, classificationMode, performanceMode, trackingEnabled, minFaceSize);
            if (executor != null) {
                faceDetectionOptions.setExecutor(executor);
            }
            return faceDetectionOptions;
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FaceDetectionOptions> CREATOR = findCreator(FaceDetectionOptions.class);

}
