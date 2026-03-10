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
import java.util.Date;

@Deprecated
public class ExposureInformation extends AutoSafeParcelable {
    @Field(1)
    private long dateMillisSinceEpoch;
    @Field(2)
    private int durationMinutes;
    @Field(3)
    private int attenuationValue;
    @Field(4)
    @RiskLevel
    private int transmissionRiskLevel;
    @Field(5)
    private int totalRiskScore;
    @Field(6)
    private int[] attenuationDurationsInMinutes;

    private ExposureInformation() {
    }

    ExposureInformation(long dateMillisSinceEpoch, int durationMinutes, int attenuationValue, @RiskLevel int transmissionRiskLevel, int totalRiskScore, int[] attenuationDurationsInMinutes) {
        this.dateMillisSinceEpoch = dateMillisSinceEpoch;
        this.durationMinutes = durationMinutes;
        this.attenuationValue = attenuationValue;
        this.transmissionRiskLevel = transmissionRiskLevel;
        this.totalRiskScore = totalRiskScore;
        this.attenuationDurationsInMinutes = attenuationDurationsInMinutes;
    }

    public long getDateMillisSinceEpoch() {
        return dateMillisSinceEpoch;
    }

    public Date getDate() {
        return new Date(dateMillisSinceEpoch);
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public int getAttenuationValue() {
        return attenuationValue;
    }

    @RiskLevel
    public int getTransmissionRiskLevel() {
        return transmissionRiskLevel;
    }

    public int getTotalRiskScore() {
        return totalRiskScore;
    }

    public int[] getAttenuationDurationsInMinutes() {
        return Arrays.copyOf(attenuationDurationsInMinutes, attenuationDurationsInMinutes.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExposureInformation that = (ExposureInformation) o;

        if (dateMillisSinceEpoch != that.dateMillisSinceEpoch) return false;
        if (durationMinutes != that.durationMinutes) return false;
        if (attenuationValue != that.attenuationValue) return false;
        if (transmissionRiskLevel != that.transmissionRiskLevel) return false;
        if (totalRiskScore != that.totalRiskScore) return false;
        return Arrays.equals(attenuationDurationsInMinutes, that.attenuationDurationsInMinutes);
    }

    @Override
    public int hashCode() {
        int result = (int) (dateMillisSinceEpoch ^ (dateMillisSinceEpoch >>> 32));
        result = 31 * result + durationMinutes;
        result = 31 * result + attenuationValue;
        result = 31 * result + transmissionRiskLevel;
        result = 31 * result + totalRiskScore;
        result = 31 * result + Arrays.hashCode(attenuationDurationsInMinutes);
        return result;
    }

    @Override
    public String toString() {
        return "ExposureInformation{" +
                "date=" + getDate() +
                ", durationMinutes=" + durationMinutes +
                ", attenuationValue=" + attenuationValue +
                ", transmissionRiskLevel=" + transmissionRiskLevel +
                ", totalRiskScore=" + totalRiskScore +
                ", attenuationDurationsInMinutes=" + Arrays.toString(attenuationDurationsInMinutes) +
                '}';
    }

    public static class ExposureInformationBuilder {
        private long dateMillisSinceEpoch;
        private int durationMinutes;
        private int attenuationValue;
        @RiskLevel
        private int transmissionRiskLevel;
        private int totalRiskScore;
        private int[] attenuationDurations = new int[]{0, 0};

        public ExposureInformationBuilder setDateMillisSinceEpoch(long dateMillisSinceEpoch) {
            this.dateMillisSinceEpoch = dateMillisSinceEpoch;
            return this;
        }

        public ExposureInformationBuilder setDurationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public ExposureInformationBuilder setAttenuationValue(int attenuationValue) {
            this.attenuationValue = attenuationValue;
            return this;
        }

        public ExposureInformationBuilder setTransmissionRiskLevel(@RiskLevel int transmissionRiskLevel) {
            this.transmissionRiskLevel = transmissionRiskLevel;
            return this;
        }

        public ExposureInformationBuilder setTotalRiskScore(int totalRiskScore) {
            this.totalRiskScore = totalRiskScore;
            return this;
        }

        public ExposureInformationBuilder setAttenuationDurations(int[] attenuationDurations) {
            this.attenuationDurations = Arrays.copyOf(attenuationDurations, attenuationDurations.length);
            return this;
        }

        public ExposureInformation build() {
            return new ExposureInformation(dateMillisSinceEpoch, durationMinutes, attenuationValue, transmissionRiskLevel, totalRiskScore, attenuationDurations);
        }
    }

    public static final Creator<ExposureInformation> CREATOR = new AutoCreator<>(ExposureInformation.class);
}
