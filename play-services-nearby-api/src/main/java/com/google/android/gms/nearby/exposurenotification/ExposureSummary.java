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
public class ExposureSummary extends AutoSafeParcelable {
    @Field(1)
    private int daysSinceLastExposure;
    @Field(2)
    private int matchedKeyCount;
    @Field(3)
    private int maximumRiskScore;
    @Field(4)
    private int[] attenuationDurationsInMinutes;
    @Field(5)
    private int summationRiskScore;

    private ExposureSummary() {
    }

    ExposureSummary(int daysSinceLastExposure, int matchedKeyCount, int maximumRiskScore, int[] attenuationDurationsInMinutes, int summationRiskScore) {
        this.daysSinceLastExposure = daysSinceLastExposure;
        this.matchedKeyCount = matchedKeyCount;
        this.maximumRiskScore = maximumRiskScore;
        this.attenuationDurationsInMinutes = attenuationDurationsInMinutes;
        this.summationRiskScore = summationRiskScore;
    }

    public int getDaysSinceLastExposure() {
        return daysSinceLastExposure;
    }

    public int getMatchedKeyCount() {
        return matchedKeyCount;
    }

    public int getMaximumRiskScore() {
        return maximumRiskScore;
    }

    public int[] getAttenuationDurationsInMinutes() {
        return Arrays.copyOf(attenuationDurationsInMinutes, attenuationDurationsInMinutes.length);
    }

    public int getSummationRiskScore() {
        return summationRiskScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExposureSummary that = (ExposureSummary) o;

        if (daysSinceLastExposure != that.daysSinceLastExposure) return false;
        if (matchedKeyCount != that.matchedKeyCount) return false;
        if (maximumRiskScore != that.maximumRiskScore) return false;
        if (summationRiskScore != that.summationRiskScore) return false;
        return Arrays.equals(attenuationDurationsInMinutes, that.attenuationDurationsInMinutes);
    }

    @Override
    public int hashCode() {
        int result = daysSinceLastExposure;
        result = 31 * result + matchedKeyCount;
        result = 31 * result + maximumRiskScore;
        result = 31 * result + Arrays.hashCode(attenuationDurationsInMinutes);
        result = 31 * result + summationRiskScore;
        return result;
    }

    @Override
    public String toString() {
        return "ExposureSummary{" +
                "daysSinceLastExposure=" + daysSinceLastExposure +
                ", matchedKeyCount=" + matchedKeyCount +
                ", maximumRiskScore=" + maximumRiskScore +
                ", attenuationDurationsInMinutes=" + Arrays.toString(attenuationDurationsInMinutes) +
                ", summationRiskScore=" + summationRiskScore +
                '}';
    }

    public static class ExposureSummaryBuilder {
        private int daysSinceLastExposure;
        private int matchedKeyCount;
        private int maximumRiskScore;
        private int[] attenuationDurations = new int[]{0, 0, 0};
        private int summationRiskScore;

        public ExposureSummaryBuilder setDaysSinceLastExposure(int daysSinceLastExposure) {
            this.daysSinceLastExposure = daysSinceLastExposure;
            return this;
        }

        public ExposureSummaryBuilder setMatchedKeyCount(int matchedKeyCount) {
            this.matchedKeyCount = matchedKeyCount;
            return this;
        }

        public ExposureSummaryBuilder setMaximumRiskScore(int maximumRiskScore) {
            this.maximumRiskScore = maximumRiskScore;
            return this;
        }

        public ExposureSummaryBuilder setAttenuationDurations(int[] attenuationDurations) {
            this.attenuationDurations = Arrays.copyOf(attenuationDurations, attenuationDurations.length);
            return this;
        }

        public ExposureSummaryBuilder setSummationRiskScore(int summationRiskScore) {
            this.summationRiskScore = summationRiskScore;
            return this;
        }

        public ExposureSummary build() {
            return new ExposureSummary(daysSinceLastExposure, matchedKeyCount, maximumRiskScore, attenuationDurations, summationRiskScore);
        }
    }

    public static final Creator<ExposureSummary> CREATOR = new AutoCreator<>(ExposureSummary.class);
}
