/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.exposurenotification;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

@Deprecated
public class ExposureConfiguration extends AutoSafeParcelable {
    @Field(1)
    private int minimumRiskScore;
    @Field(2)
    private int[] attenuationScores;
    @Field(3)
    private int attenuationWeight;
    @Field(4)
    private int[] daysSinceLastExposureScores;
    @Field(5)
    private int daysSinceLastExposureWeight;
    @Field(6)
    private int[] durationScores;
    @Field(7)
    private int durationWeight;
    @Field(8)
    private int[] transmissionRiskScores;
    @Field(9)
    private int transmissionRiskWeight;
    @Field(10)
    private int[] durationAtAttenuationThresholds;

    private ExposureConfiguration() {
    }

    ExposureConfiguration(int minimumRiskScore, int[] attenuationScores, int attenuationWeight, int[] daysSinceLastExposureScores, int daysSinceLastExposureWeight, int[] durationScores, int durationWeight, int[] transmissionRiskScores, int transmissionRiskWeight, int[] durationAtAttenuationThresholds) {
        this.minimumRiskScore = minimumRiskScore;
        this.attenuationScores = attenuationScores;
        this.attenuationWeight = attenuationWeight;
        this.daysSinceLastExposureScores = daysSinceLastExposureScores;
        this.daysSinceLastExposureWeight = daysSinceLastExposureWeight;
        this.durationScores = durationScores;
        this.durationWeight = durationWeight;
        this.transmissionRiskScores = transmissionRiskScores;
        this.transmissionRiskWeight = transmissionRiskWeight;
        this.durationAtAttenuationThresholds = durationAtAttenuationThresholds;
    }

    public int getMinimumRiskScore() {
        return minimumRiskScore;
    }

    public int[] getAttenuationScores() {
        return Arrays.copyOf(attenuationScores, attenuationScores.length);
    }

    public int getAttenuationWeight() {
        return attenuationWeight;
    }

    public int[] getDaysSinceLastExposureScores() {
        return Arrays.copyOf(daysSinceLastExposureScores, daysSinceLastExposureScores.length);
    }

    public int getDaysSinceLastExposureWeight() {
        return daysSinceLastExposureWeight;
    }

    public int[] getDurationScores() {
        return Arrays.copyOf(durationScores, durationScores.length);
    }

    public int getDurationWeight() {
        return durationWeight;
    }

    public int[] getTransmissionRiskScores() {
        return Arrays.copyOf(transmissionRiskScores, transmissionRiskScores.length);
    }

    public int getTransmissionRiskWeight() {
        return transmissionRiskWeight;
    }

    public int[] getDurationAtAttenuationThresholds() {
        return Arrays.copyOf(durationAtAttenuationThresholds, durationAtAttenuationThresholds.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExposureConfiguration that = (ExposureConfiguration) o;

        if (minimumRiskScore != that.minimumRiskScore) return false;
        if (attenuationWeight != that.attenuationWeight) return false;
        if (daysSinceLastExposureWeight != that.daysSinceLastExposureWeight) return false;
        if (durationWeight != that.durationWeight) return false;
        if (transmissionRiskWeight != that.transmissionRiskWeight) return false;
        if (!Arrays.equals(attenuationScores, that.attenuationScores)) return false;
        if (!Arrays.equals(daysSinceLastExposureScores, that.daysSinceLastExposureScores)) return false;
        if (!Arrays.equals(durationScores, that.durationScores)) return false;
        if (!Arrays.equals(transmissionRiskScores, that.transmissionRiskScores)) return false;
        return Arrays.equals(durationAtAttenuationThresholds, that.durationAtAttenuationThresholds);
    }

    @Override
    public int hashCode() {
        int result = minimumRiskScore;
        result = 31 * result + Arrays.hashCode(attenuationScores);
        result = 31 * result + attenuationWeight;
        result = 31 * result + Arrays.hashCode(daysSinceLastExposureScores);
        result = 31 * result + daysSinceLastExposureWeight;
        result = 31 * result + Arrays.hashCode(durationScores);
        result = 31 * result + durationWeight;
        result = 31 * result + Arrays.hashCode(transmissionRiskScores);
        result = 31 * result + transmissionRiskWeight;
        result = 31 * result + Arrays.hashCode(durationAtAttenuationThresholds);
        return result;
    }

    @Override
    public String toString() {
        return "ExposureConfiguration{" +
                "minimumRiskScore=" + minimumRiskScore +
                ", attenuationScores=" + Arrays.toString(attenuationScores) +
                ", attenuationWeight=" + attenuationWeight +
                ", daysSinceLastExposureScores=" + Arrays.toString(daysSinceLastExposureScores) +
                ", daysSinceLastExposureWeight=" + daysSinceLastExposureWeight +
                ", durationScores=" + Arrays.toString(durationScores) +
                ", durationWeight=" + durationWeight +
                ", transmissionRiskScores=" + Arrays.toString(transmissionRiskScores) +
                ", transmissionRiskWeight=" + transmissionRiskWeight +
                ", durationAtAttenuationThresholds=" + Arrays.toString(durationAtAttenuationThresholds) +
                '}';
    }

    public static class ExposureConfigurationBuilder {
        private int minimumRiskScore = 4;
        private int[] attenuationScores = new int[]{4, 4, 4, 4, 4, 4, 4, 4};
        private int attenuationWeight = 50;
        private int[] daysSinceLastExposureScores = new int[]{4, 4, 4, 4, 4, 4, 4, 4};
        private int daysSinceLastExposureWeight = 50;
        private int[] durationScores = new int[]{4, 4, 4, 4, 4, 4, 4, 4};
        private int durationWeight = 50;
        private int[] transmissionRiskScores = new int[]{4, 4, 4, 4, 4, 4, 4, 4};
        private int transmissionRiskWeight = 50;
        private int[] durationAtAttenuationThresholds = new int[]{50, 74};

        public ExposureConfigurationBuilder setMinimumRiskScore(int minimumRiskScore) {
            this.minimumRiskScore = minimumRiskScore;
            return this;
        }

        public ExposureConfigurationBuilder setAttenuationScores(int... attenuationScores) {
            this.attenuationScores = Arrays.copyOf(attenuationScores, attenuationScores.length);
            return this;
        }

        public ExposureConfigurationBuilder setAttenuationWeight(int attenuationWeight) {
            this.attenuationWeight = attenuationWeight;
            return this;
        }

        public ExposureConfigurationBuilder setDaysSinceLastExposureScores(int... daysSinceLastExposureScores) {
            this.daysSinceLastExposureScores = Arrays.copyOf(daysSinceLastExposureScores, daysSinceLastExposureScores.length);
            return this;
        }

        public ExposureConfigurationBuilder setDaysSinceLastExposureWeight(int daysSinceLastExposureWeight) {
            this.daysSinceLastExposureWeight = daysSinceLastExposureWeight;
            return this;
        }

        public ExposureConfigurationBuilder setDurationScores(int... durationScores) {
            this.durationScores = Arrays.copyOf(durationScores, durationScores.length);
            return this;
        }

        public ExposureConfigurationBuilder setDurationWeight(int durationWeight) {
            this.durationWeight = durationWeight;
            return this;
        }

        public ExposureConfigurationBuilder setTransmissionRiskScores(int... transmissionRiskScores) {
            this.transmissionRiskScores = Arrays.copyOf(transmissionRiskScores, transmissionRiskScores.length);
            return this;
        }

        public ExposureConfigurationBuilder setTransmissionRiskWeight(int transmissionRiskWeight) {
            this.transmissionRiskWeight = transmissionRiskWeight;
            return this;
        }

        public ExposureConfigurationBuilder setDurationAtAttenuationThresholds(int... durationAtAttenuationThresholds) {
            this.durationAtAttenuationThresholds = Arrays.copyOf(durationAtAttenuationThresholds, durationAtAttenuationThresholds.length);
            return this;
        }

        public ExposureConfiguration build() {
            return new ExposureConfiguration(minimumRiskScore, attenuationScores, attenuationWeight, daysSinceLastExposureScores, daysSinceLastExposureWeight, durationScores, durationWeight, transmissionRiskScores, transmissionRiskWeight, durationAtAttenuationThresholds);
        }
    }

    public static final Creator<ExposureConfiguration> CREATOR = new AutoCreator<>(ExposureConfiguration.class);
}
