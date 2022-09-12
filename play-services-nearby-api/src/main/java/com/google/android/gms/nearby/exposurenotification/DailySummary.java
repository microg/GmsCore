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

import java.util.List;

/**
 * Daily exposure summary to pass to client side.
 */
@PublicApi
public class DailySummary extends AutoSafeParcelable {
    @Field(1)
    private int daysSinceEpoch;
    @Field(2)
    private List<ExposureSummaryData> reportSummaries;
    @Field(3)
    private ExposureSummaryData summaryData;

    private DailySummary() {
    }

    @PublicApi(exclude = true)
    public DailySummary(int daysSinceEpoch, List<ExposureSummaryData> reportSummaries, ExposureSummaryData summaryData) {
        this.daysSinceEpoch = daysSinceEpoch;
        this.reportSummaries = reportSummaries;
        this.summaryData = summaryData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailySummary)) return false;

        DailySummary that = (DailySummary) o;

        if (daysSinceEpoch != that.daysSinceEpoch) return false;
        if (reportSummaries != null ? !reportSummaries.equals(that.reportSummaries) : that.reportSummaries != null)
            return false;
        return summaryData != null ? summaryData.equals(that.summaryData) : that.summaryData == null;
    }

    /**
     * Returns days since epoch of the {@link ExposureWindow}s that went into this summary.
     */
    public int getDaysSinceEpoch() {
        return daysSinceEpoch;
    }

    /**
     * Summary of all exposures on this day.
     */
    public ExposureSummaryData getSummaryData() {
        return summaryData;
    }

    /**
     * Summary of all exposures on this day of a specific diagnosis {@link ReportType}.
     */
    public ExposureSummaryData getSummaryDataForReportType(@ReportType int reportType) {
        return reportSummaries.get(reportType);
    }

    @Override
    public int hashCode() {
        int result = daysSinceEpoch;
        result = 31 * result + (reportSummaries != null ? reportSummaries.hashCode() : 0);
        result = 31 * result + (summaryData != null ? summaryData.hashCode() : 0);
        return result;
    }

    /**
     * Stores different scores for specific {@link ReportType}.
     */
    public static class ExposureSummaryData extends AutoSafeParcelable {
        @Field(1)
        private double maximumScore;
        @Field(2)
        private double scoreSum;
        @Field(3)
        private double weightedDurationSum;

        private ExposureSummaryData() {
        }

        @PublicApi(exclude = true)
        public ExposureSummaryData(double maximumScore, double scoreSum, double weightedDurationSum) {
            this.maximumScore = maximumScore;
            this.scoreSum = scoreSum;
            this.weightedDurationSum = weightedDurationSum;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExposureSummaryData)) return false;

            ExposureSummaryData that = (ExposureSummaryData) o;

            if (Double.compare(that.maximumScore, maximumScore) != 0) return false;
            if (Double.compare(that.scoreSum, scoreSum) != 0) return false;
            return Double.compare(that.weightedDurationSum, weightedDurationSum) == 0;
        }

        /**
         * Highest score of all {@link ExposureWindow}s aggregated into this summary.
         * <p>
         * See {@link DailySummariesConfig} for more information about how the per-{@link ExposureWindow} score is computed.
         */
        public double getMaximumScore() {
            return maximumScore;
        }

        /**
         * Sum of scores for all {@link ExposureWindow}s aggregated into this summary.
         * <p>
         * See {@link DailySummariesConfig} for more information about how the per-{@link ExposureWindow} score is computed.
         */
        public double getScoreSum() {
            return scoreSum;
        }


        /**
         * Sum of weighted durations for all {@link ExposureWindow}s aggregated into this summary.
         * <p>
         * See {@link DailySummariesConfig} for more information about how the per-{@link ExposureWindow} score is computed.
         */
        public double getWeightedDurationSum() {
            return weightedDurationSum;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(maximumScore);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(scoreSum);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(weightedDurationSum);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        public static final Creator<ExposureSummaryData> CREATOR = new AutoCreator<>(ExposureSummaryData.class);
    }

    public static final Creator<DailySummary> CREATOR = new AutoCreator<>(DailySummary.class);
}
