/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

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
    private long elapsedRealtimeNanos = 0;
    @Field(7)
    private byte flags = 0;
    @Field(8)
    private float conservativeHeadingErrorVonMisesKappa = Float.NaN;

    public float[] getAttitude() {
        if ((flags & 0x10) != 0) return attitude;
        return new float[4];
    }

    public void setAttitude(float[] attitude) {
        if (attitude.length != 4) throw new IllegalArgumentException();
        this.attitude = attitude;
        flags = (byte) (flags | 0x10);
    }

    public int getAttitudeConfidence() {
        if ((flags & 0x1) != 0) return attitudeConfidence;
        return -1;
    }

    public void setAttitudeConfidence(int attitudeConfidence) {
        this.attitudeConfidence = attitudeConfidence;
        flags = (byte) (flags | 0x1);
    }

    public int getMagConfidence() {
        if ((flags & 0x2) != 0) return magConfidence;
        return -1;
    }

    public void setMagConfidence(int magConfidence) {
        this.magConfidence = magConfidence;
        flags = (byte) (flags | 0x2);
    }

    public float getHeadingDegrees() {
        if ((flags & 0x4) != 0) return headingDegrees;
        return Float.NaN;
    }

    public void setHeadingDegrees(float headingDegrees) {
        this.headingDegrees = headingDegrees;
        flags = (byte) (flags | 0x4);
    }

    public float getHeadingErrorDegrees() {
        if ((flags & 0x8) != 0) return headingErrorDegrees;
        return Float.NaN;
    }

    public void setHeadingErrorDegrees(float headingErrorDegrees) {
        this.headingErrorDegrees = headingErrorDegrees;
        flags = (byte) (flags | 0x8);
    }

    public float getConservativeHeadingErrorVonMisesKappa() {
        if ((flags & 0x20) != 0) return conservativeHeadingErrorVonMisesKappa;
        return Float.NaN;
    }

    public void setConservativeHeadingErrorVonMisesKappa(float conservativeHeadingErrorVonMisesKappa) {
        this.conservativeHeadingErrorVonMisesKappa = conservativeHeadingErrorVonMisesKappa;
        flags = (byte) (flags | 0x20);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DeviceOrientation{");
        if ((flags & 0x10) != 0)
            sb.append("attitude=").append(Arrays.toString(attitude));
        if ((flags & 0x1) != 0)
            sb.append(", attitudeConfidence=").append(attitudeConfidence);
        if ((flags & 0x2) != 0)
            sb.append(", magConfidence=").append(magConfidence);
        if ((flags & 0x4) != 0)
            sb.append(", headingDegrees=").append(headingDegrees);
        if ((flags & 0x8) != 0)
            sb.append(", headingErrorDegrees=").append(headingErrorDegrees);
        return "DeviceOrientation{" +
                "attitude=" + Arrays.toString(attitude) +
                ", attitudeConfidence=" + attitudeConfidence +
                ", magConfidence=" + magConfidence +
                ", headingDegrees=" + headingDegrees +
                ", headingErrorDegrees=" + headingErrorDegrees +
                ", elapsedRealtimeNanos=" + elapsedRealtimeNanos +
                ", flags=" + flags +
                ", conservativeHeadingErrorVonMisesKappa=" + conservativeHeadingErrorVonMisesKappa +
                '}';
    }

    public static final Creator<DeviceOrientation> CREATOR = new AutoCreator<DeviceOrientation>(DeviceOrientation.class);
}
