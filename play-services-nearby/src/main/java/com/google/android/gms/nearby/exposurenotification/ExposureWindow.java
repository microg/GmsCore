/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.exposurenotification;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * A duration of up to 30 minutes during which beacons from a TEK were observed.
 * <p>
 * Each {@link ExposureWindow} corresponds to a single TEK, but one TEK can lead to several {@link ExposureWindow} due to random 15-30 minutes cuts. See {@link ExposureNotificationClient#getExposureWindows()} for more info.
 * <p>
 * The TEK itself isn't exposed by the API.
 */
@PublicApi
public class ExposureWindow extends AutoSafeParcelable {
    @Field(1)
    private long dateMillisSinceEpoch;
    @Field(2)
    private List<ScanInstance> scanInstances;
    @Field(3)
    @ReportType
    private int reportType;
    @Field(4)
    @Infectiousness
    private int infectiousness;
    @Field(5)
    @CalibrationConfidence
    private int calibrationConfidence;

    private ExposureWindow() {
    }

    private ExposureWindow(long dateMillisSinceEpoch, List<ScanInstance> scanInstances, int reportType, int infectiousness, int calibrationConfidence) {
        this.dateMillisSinceEpoch = dateMillisSinceEpoch;
        this.scanInstances = scanInstances;
        this.reportType = reportType;
        this.infectiousness = infectiousness;
        this.calibrationConfidence = calibrationConfidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExposureWindow)) return false;

        ExposureWindow that = (ExposureWindow) o;

        if (dateMillisSinceEpoch != that.dateMillisSinceEpoch) return false;
        if (reportType != that.reportType) return false;
        if (infectiousness != that.infectiousness) return false;
        if (calibrationConfidence != that.calibrationConfidence) return false;
        return scanInstances != null ? scanInstances.equals(that.scanInstances) : that.scanInstances == null;
    }

    /**
     * Confidence of the BLE Transmit power calibration of the transmitting device.
     */
    @CalibrationConfidence
    public int getCalibrationConfidence() {
        return calibrationConfidence;
    }

    /**
     * Returns the epoch time in milliseconds the exposure occurred. This will represent the start of a day in UTC.
     */
    public long getDateMillisSinceEpoch() {
        return dateMillisSinceEpoch;
    }

    /**
     * Infectiousness of the TEK that caused this exposure, computed from the days since onset of symptoms using the daysToInfectiousnessMapping.
     */
    @Infectiousness
    public int getInfectiousness() {
        return infectiousness;
    }

    /**
     * Report Type of the TEK that caused this exposure
     * <p>
     * TEKs with no report type set are returned with reportType=CONFIRMED_TEST.
     * <p>
     * TEKs with RECURSIVE report type may be dropped because this report type is reserved for future use.
     * <p>
     * TEKs with REVOKED or invalid report types do not lead to exposures.
     */
    @ReportType
    public int getReportType() {
        return reportType;
    }

    /**
     * Sightings of this ExposureWindow, time-ordered.
     * <p>
     * Each sighting corresponds to a scan (of a few seconds) during which a beacon with the TEK causing this exposure was observed.
     */
    public List<ScanInstance> getScanInstances() {
        return scanInstances;
    }

    @Override
    public int hashCode() {
        int result = (int) (dateMillisSinceEpoch ^ (dateMillisSinceEpoch >>> 32));
        result = 31 * result + (scanInstances != null ? scanInstances.hashCode() : 0);
        result = 31 * result + reportType;
        result = 31 * result + infectiousness;
        result = 31 * result + calibrationConfidence;
        return result;
    }

    /**
     * Builder for ExposureWindow.
     */
    public static class Builder {
        private long dateMillisSinceEpoch;
        private List<ScanInstance> scanInstances;
        @ReportType
        private int reportType;
        @Infectiousness
        private int infectiousness;
        @CalibrationConfidence
        private int calibrationConfidence;

        public ExposureWindow build() {
            return new ExposureWindow(dateMillisSinceEpoch, scanInstances, reportType, infectiousness, calibrationConfidence);
        }

        public Builder setCalibrationConfidence(int calibrationConfidence) {
            this.calibrationConfidence = calibrationConfidence;
            return this;
        }

        public Builder setDateMillisSinceEpoch(long dateMillisSinceEpoch) {
            this.dateMillisSinceEpoch = dateMillisSinceEpoch;
            return this;
        }

        public Builder setInfectiousness(@Infectiousness int infectiousness) {
            this.infectiousness = infectiousness;
            return this;
        }

        public Builder setReportType(@ReportType int reportType) {
            this.reportType = reportType;
            return this;
        }

        public Builder setScanInstances(List<ScanInstance> scanInstances) {
            this.scanInstances = new ArrayList<>(scanInstances);
            return this;
        }
    }

    public static final Creator<ExposureWindow> CREATOR = new AutoCreator<>(ExposureWindow.class);
}
