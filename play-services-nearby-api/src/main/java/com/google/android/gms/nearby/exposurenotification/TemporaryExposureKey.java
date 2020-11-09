/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.exposurenotification;

import org.microg.gms.nearby.exposurenotification.Constants;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

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

    public static final int DAYS_SINCE_ONSET_OF_SYMPTOMS_UNKNOWN = Constants.DAYS_SINCE_ONSET_OF_SYMPTOMS_UNKNOWN;

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

    public byte[] getKeyData() {
        return Arrays.copyOf(keyData, keyData.length);
    }

    public int getRollingStartIntervalNumber() {
        return rollingStartIntervalNumber;
    }

    @RiskLevel
    public int getTransmissionRiskLevel() {
        return transmissionRiskLevel;
    }

    public int getRollingPeriod() {
        return rollingPeriod;
    }

    @ReportType
    public int getReportType() {
        return reportType;
    }

    int getDaysSinceOnsetOfSymptoms() {
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
