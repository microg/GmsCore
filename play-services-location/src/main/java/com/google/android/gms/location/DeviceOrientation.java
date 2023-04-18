/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location;

import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

@Hide
public class DeviceOrientation extends AutoSafeParcelable {
    @Field(1)
    private float[] attitude = new float[4];
    @Field(2)
    private int attitudeConfidence = -1;
    @Field(3)
    private int magConfidence = -1;
    @Field(4)
    private float headingDegrees = Float.NaN;
    @Field(5)
    private float headingErrorDegrees = Float.NaN;
    @Field(6)
    public long elapsedRealtimeNanos = 0;
    @Field(7)
    private byte fieldsMask = 0;
    @Field(8)
    private float conservativeHeadingErrorVonMisesKappa = Float.NaN;

    public float[] getAttitude() {
        if (hasAttitude()) return attitude;
        return new float[4];
    }

    public void setAttitude(float[] attitude) {
        if (attitude.length != 4) throw new IllegalArgumentException();
        this.attitude = attitude;
        fieldsMask = (byte) (fieldsMask | 0x10);
    }

    public int getAttitudeConfidence() {
        if (hasAttitudeConfidence()) return attitudeConfidence;
        return -1;
    }

    public void setAttitudeConfidence(int attitudeConfidence) {
        this.attitudeConfidence = attitudeConfidence;
        fieldsMask = (byte) (fieldsMask | 0x1);
    }

    public int getMagConfidence() {
        if (hasMagConfidence()) return magConfidence;
        return -1;
    }

    public void setMagConfidence(int magConfidence) {
        this.magConfidence = magConfidence;
        fieldsMask = (byte) (fieldsMask | 0x2);
    }

    public float getHeadingDegrees() {
        if (hasHeadingDegrees()) return headingDegrees;
        return Float.NaN;
    }

    public void setHeadingDegrees(float headingDegrees) {
        this.headingDegrees = headingDegrees;
        fieldsMask = (byte) (fieldsMask | 0x4);
    }

    public float getHeadingErrorDegrees() {
        if (hasHeadingErrorDegrees()) return headingErrorDegrees;
        return Float.NaN;
    }

    public void setHeadingErrorDegrees(float headingErrorDegrees) {
        this.headingErrorDegrees = headingErrorDegrees;
        fieldsMask = (byte) (fieldsMask | 0x8);
    }

    public float getConservativeHeadingErrorVonMisesKappa() {
        if (hasConservativeHeadingErrorVonMisesKappa()) return conservativeHeadingErrorVonMisesKappa;
        return Float.NaN;
    }

    public void setConservativeHeadingErrorVonMisesKappa(float conservativeHeadingErrorVonMisesKappa) {
        this.conservativeHeadingErrorVonMisesKappa = conservativeHeadingErrorVonMisesKappa;
        fieldsMask = (byte) (fieldsMask | 0x20);
    }

    public final boolean hasAttitude() {
        return (fieldsMask & 0x10) != 0;
    }

    public final boolean hasAttitudeConfidence() {
        return (fieldsMask & 0x1) != 0;
    }

    public final boolean hasConservativeHeadingErrorVonMisesKappa() {
        return (fieldsMask & 0x20) != 0;
    }

    public final boolean hasHeadingDegrees() {
        return (fieldsMask & 0x4) != 0;
    }

    public final boolean hasHeadingErrorDegrees() {
        return (fieldsMask & 0x8) != 0;
    }

    public final boolean hasMagConfidence() {
        return (fieldsMask & 0x2) != 0;
    }

    @Override
    public String toString() {
        ToStringHelper helper = ToStringHelper.name("DeviceOrientation");
        if (hasAttitude()) helper.field("attitude", Arrays.toString(attitude));
        if (hasAttitudeConfidence()) helper.field("attitudeConfidence", attitudeConfidence);
        if (hasMagConfidence()) helper.field("magConfidence", magConfidence);
        if (hasHeadingDegrees()) helper.field("headingDegrees", headingDegrees);
        if (hasHeadingErrorDegrees()) helper.field("headingErrorDegrees", headingErrorDegrees);
        if (hasConservativeHeadingErrorVonMisesKappa()) helper.field("conservativeHeadingErrorVonMisesKappa", conservativeHeadingErrorVonMisesKappa);
        helper.field("elapsedRealtimeNanos", elapsedRealtimeNanos);
        return helper.end();
    }

    public static final Creator<DeviceOrientation> CREATOR = new AutoCreator<DeviceOrientation>(DeviceOrientation.class);
}
