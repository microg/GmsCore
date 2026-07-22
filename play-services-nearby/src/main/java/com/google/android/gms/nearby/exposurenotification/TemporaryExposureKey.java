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

/**
 * A key generated for advertising over a window of time.
 */
public class TemporaryExposureKey extends AutoSafeParcelable {
    @Field(1)
    private byte[] keyData;
    @Field(2)
    private int rollingStartIntervalNumber;
    @Field(3)
    @RiskLevel
    private int transmissionRiskLevel;
    @Field(4)
    private int rollingPeriod;
    @Field(5)
    @ReportType
    private int reportType;
    @Field(6)
    int daysSinceOnsetOfSymptoms;

    /**
     * The default value for {@link #getDaysSinceOnsetOfSymptoms()}.
     *
     * See {@link DiagnosisKeysDataMapping#getDaysSinceOnsetToInfectiousness()} for more information.
     */
    public static final int DAYS_SINCE_ONSET_OF_SYMPTOMS_UNKNOWN = Integer.MAX_VALUE;

    private TemporaryExposureKey() {
    }

    TemporaryExposureKey(byte[] keyData, int rollingStartIntervalNumber, @RiskLevel int transmissionRiskLevel, int rollingPeriod, @ReportType int reportType, int daysSinceOnsetOfSymptoms) {
        this.keyData = (keyData == null ? new byte[0] : keyData);
        this.rollingStartIntervalNumber = rollingStartIntervalNumber;
        this.transmissionRiskLevel = transmissionRiskLevel;
        this.rollingPeriod = rollingPeriod;
        this.reportType = reportType;
        this.daysSinceOnsetOfSymptoms = daysSinceOnsetOfSymptoms;
    }

    /**
     * The randomly generated Temporary Exposure Key information.
     */
    public byte[] getKeyData() {
        return Arrays.copyOf(keyData, keyData.length);
    }

    /**
     * A number describing when a key starts. It is equal to startTimeOfKeySinceEpochInSecs / (60 * 10).
     */
    public int getRollingStartIntervalNumber() {
        return rollingStartIntervalNumber;
    }

    /**
     * Risk of transmission associated with the person this key came from.
     */
    @RiskLevel
    public int getTransmissionRiskLevel() {
        return transmissionRiskLevel;
    }

    /**
     * A number describing how long a key is valid. It is expressed in increments of 10 minutes (e.g. 144 for 24 hours).
     */
    public int getRollingPeriod() {
        return rollingPeriod;
    }

    /**
     * Type of diagnosis associated with a key.
     */
    @ReportType
    public int getReportType() {
        return reportType;
    }

    /**
     * Number of days elapsed between symptom onset and the key being used.
     * <p>
     * E.g. 2 means the key is 2 days after onset of symptoms.
     */
    public int getDaysSinceOnsetOfSymptoms() {
        return daysSinceOnsetOfSymptoms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TemporaryExposureKey that = (TemporaryExposureKey) o;

        if (rollingStartIntervalNumber != that.rollingStartIntervalNumber) return false;
        if (transmissionRiskLevel != that.transmissionRiskLevel) return false;
        if (rollingPeriod != that.rollingPeriod) return false;
        if (reportType != that.reportType) return false;
        if (daysSinceOnsetOfSymptoms != that.daysSinceOnsetOfSymptoms) return false;
        return Arrays.equals(keyData, that.keyData);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(keyData);
        result = 31 * result + rollingStartIntervalNumber;
        result = 31 * result + transmissionRiskLevel;
        result = 31 * result + rollingPeriod;
        result = 31 * result + reportType;
        result = 31 * result + daysSinceOnsetOfSymptoms;
        return result;
    }

    @Override
    public String toString() {
        return "TemporaryExposureKey{" +
                "keyData=" + Arrays.toString(keyData) +
                ", rollingStartIntervalNumber=" + rollingStartIntervalNumber +
                ", transmissionRiskLevel=" + transmissionRiskLevel +
                ", rollingPeriod=" + rollingPeriod +
                ", reportType=" + reportType +
                ", daysSinceOnsetOfSymptoms=" + daysSinceOnsetOfSymptoms +
                '}';
    }

    /**
     * A builder for {@link TemporaryExposureKey}.
     */
    public static class TemporaryExposureKeyBuilder {
        private byte[] keyData;
        private int rollingStartIntervalNumber;
        @RiskLevel
        private int transmissionRiskLevel;
        private int rollingPeriod;
        @ReportType
        private int reportType;
        private int daysSinceOnsetOfSymptoms = DAYS_SINCE_ONSET_OF_SYMPTOMS_UNKNOWN;

        public TemporaryExposureKeyBuilder setKeyData(byte[] keyData) {
            this.keyData = Arrays.copyOf(keyData, keyData.length);
            return this;
        }

        public TemporaryExposureKeyBuilder setRollingStartIntervalNumber(int rollingStartIntervalNumber) {
            this.rollingStartIntervalNumber = rollingStartIntervalNumber;
            return this;
        }

        public TemporaryExposureKeyBuilder setTransmissionRiskLevel(@RiskLevel int transmissionRiskLevel) {
            this.transmissionRiskLevel = transmissionRiskLevel;
            return this;
        }

        public TemporaryExposureKeyBuilder setRollingPeriod(int rollingPeriod) {
            this.rollingPeriod = rollingPeriod;
            return this;
        }

        public TemporaryExposureKeyBuilder setReportType(@ReportType int reportType) {
            this.reportType = reportType;
            return this;
        }

        public TemporaryExposureKeyBuilder setDaysSinceOnsetOfSymptoms(int daysSinceOnsetOfSymptoms) {
            this.daysSinceOnsetOfSymptoms = daysSinceOnsetOfSymptoms;
            return this;
        }

        public TemporaryExposureKey build() {
            return new TemporaryExposureKey(keyData, rollingStartIntervalNumber, transmissionRiskLevel, rollingPeriod, reportType, daysSinceOnsetOfSymptoms);
        }
    }

    public static final Creator<TemporaryExposureKey> CREATOR = new AutoCreator<>(TemporaryExposureKey.class);
}
