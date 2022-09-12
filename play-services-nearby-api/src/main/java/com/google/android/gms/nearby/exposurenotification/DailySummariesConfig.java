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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration of per-day summary of exposures.
 * <p>
 * During summarization the following are computed for each ExposureWindows:
 * <ul>
 * <li>a weighted duration, computed as
 * {@code ( immediateDurationSeconds * immediateDurationWeight ) + ( nearDurationSeconds * nearDurationWeight ) + ( mediumDurationSeconds * mediumDurationWeight ) + ( otherDurationSeconds * otherDurationWeight )}</li>
 * <li>a score, computed as
 * {@code reportTypeWeights[Tek.reportType] * infectiousnessWeights[infectiousness] * weightedDuration}
 * where infectiousness and reportType are set based on the ExposureWindow's diagnosis key and the DiagnosisKeysDataMapping</li>
 * </ul>
 * <p>
 * The {@link ExposureWindow}s are then filtered, removing those with score lower than {@link #getMinimumWindowScore()}.
 * <p>
 * Scores and weighted durations of the {@link ExposureWindow}s that pass the {@link #getMinimumWindowScore()} are then aggregated over a day to compute the maximum and cumulative scores and duration:
 * <ul>
 * <li>sumScore = sum(score of ExposureWindows)</li>
 * <li>maxScore = max(score of ExposureWindows)</li>
 * <li>weightedDurationSum = sum(weighted duration of ExposureWindow)</li>
 * </ul>
 * Note that when the weights are typically around 100% (1.0), both the scores and the weightedDurationSum can be considered as being expressed in seconds. For example, 15 minutes of exposure with all weights equal to 1.0 would be 60 * 15 = 900 (seconds).
 */
@PublicApi
public class DailySummariesConfig extends AutoSafeParcelable {
    @Field(value = 1, useDirectList = true)
    private List<Double> reportTypeWeights;
    @Field(value = 2, useDirectList = true)
    private List<Double> infectiousnessWeights;
    @Field(value = 3, useDirectList = true)
    private List<Integer> attenuationBucketThresholdDb;
    @Field(value = 4, useDirectList = true)
    private List<Double> attenuationBucketWeights;
    @Field(5)
    private int daysSinceExposureThreshold;
    @Field(6)
    private double minimumWindowScore;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailySummariesConfig)) return false;

        DailySummariesConfig that = (DailySummariesConfig) o;

        if (daysSinceExposureThreshold != that.daysSinceExposureThreshold) return false;
        if (Double.compare(that.minimumWindowScore, minimumWindowScore) != 0) return false;
        if (reportTypeWeights != null ? !reportTypeWeights.equals(that.reportTypeWeights) : that.reportTypeWeights != null)
            return false;
        if (infectiousnessWeights != null ? !infectiousnessWeights.equals(that.infectiousnessWeights) : that.infectiousnessWeights != null)
            return false;
        if (attenuationBucketThresholdDb != null ? !attenuationBucketThresholdDb.equals(that.attenuationBucketThresholdDb) : that.attenuationBucketThresholdDb != null)
            return false;
        return attenuationBucketWeights != null ? attenuationBucketWeights.equals(that.attenuationBucketWeights) : that.attenuationBucketWeights == null;
    }

    /**
     * Thresholds defining the BLE attenuation buckets edges.
     * <p>
     * This list must have 3 elements: the immediate, near, and medium thresholds. See attenuationBucketWeights for more information.
     * <p>
     * These elements must be between 0 and 255 and come in increasing order.
     */
    public List<Integer> getAttenuationBucketThresholdDb() {
        return attenuationBucketThresholdDb;
    }

    /**
     * Scoring weights to associate with ScanInstances depending on the attenuation bucket in which their typicalAttenuationDb falls.
     * <p>
     * This list must have 4 elements, corresponding to the weights for the 4 buckets.
     * <ul>
     * <li>immediate bucket: -infinity < attenuation <= immediate threshold</li>
     * <li>near bucket: immediate threshold < attenuation <= near threshold</li>
     * <li>medium bucket: near threshold < attenuation <= medium threshold</li>
     * <li>other bucket: medium threshold < attenuation < +infinity</li>
     * </ul>
     * Each element must be between 0 and 2.5.
     */
    public List<Double> getAttenuationBucketWeights() {
        return attenuationBucketWeights;
    }

    /**
     * Reserved for future use, behavior will be changed in future revisions. No value should be set, or else 0 should be used.
     */
    public int getDaysSinceExposureThreshold() {
        return daysSinceExposureThreshold;
    }

    /**
     * Scoring weights to associate with exposures with different Infectiousness.
     * <p>
     * This map can include weights for the following Infectiousness values:
     * <ul>
     * <li>STANDARD</li>
     * <li>HIGH</li>
     * </ul>
     * Each element must be between 0 and 2.5.
     */
    public Map<Integer, Double> getInfectiousnessWeights() {
        HashMap<Integer, Double> map = new HashMap<>();
        for (int i = 0; i < infectiousnessWeights.size(); i++) {
            map.put(i, infectiousnessWeights.get(i));
        }
        return map;
    }

    /**
     * Minimum score that {@link ExposureWindow}s must reach in order to be included in the {@link DailySummary.ExposureSummaryData}.
     * <p>
     * Use 0 to consider all {@link ExposureWindow}s (recommended).
     */
    public double getMinimumWindowScore() {
        return minimumWindowScore;
    }

    /**
     * Scoring weights to associate with exposures with different ReportTypes.
     * <p>
     * This map can include weights for the following ReportTypes:
     * <ul>
     * <li>CONFIRMED_TEST</li>
     * <li>CONFIRMED_CLINICAL_DIAGNOSIS</li>
     * <li>SELF_REPORT</li>
     * <li>RECURSIVE (reserved for future use)</li>
     * </ul>
     * Each element must be between 0 and 2.5.
     */
    public Map<Integer, Double> getReportTypeWeights() {
        HashMap<Integer, Double> map = new HashMap<>();
        for (int i = 0; i < reportTypeWeights.size(); i++) {
            map.put(i, reportTypeWeights.get(i));
        }
        return map;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = reportTypeWeights != null ? reportTypeWeights.hashCode() : 0;
        result = 31 * result + (infectiousnessWeights != null ? infectiousnessWeights.hashCode() : 0);
        result = 31 * result + (attenuationBucketThresholdDb != null ? attenuationBucketThresholdDb.hashCode() : 0);
        result = 31 * result + (attenuationBucketWeights != null ? attenuationBucketWeights.hashCode() : 0);
        result = 31 * result + daysSinceExposureThreshold;
        temp = Double.doubleToLongBits(minimumWindowScore);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * A builder for {@link DailySummariesConfig}.
     */
    public static class DailySummariesConfigBuilder {
        private Double[] reportTypeWeights = new Double[ReportType.VALUES];
        private Double[] infectiousnessWeights = new Double[Infectiousness.VALUES];
        private List<Integer> attenuationBucketThresholdDb;
        private List<Double> attenuationBucketWeights;
        private int daysSinceExposureThreshold;
        private double minimumWindowScore;

        public DailySummariesConfigBuilder() {
            Arrays.fill(reportTypeWeights, 0.0);
            Arrays.fill(infectiousnessWeights, 0.0);
        }

        public DailySummariesConfig build() {
            if (attenuationBucketThresholdDb == null)
                throw new IllegalStateException("Must set attenuationBucketThresholdDb");
            if (attenuationBucketWeights == null)
                throw new IllegalStateException("Must set attenuationBucketWeights");
            DailySummariesConfig config = new DailySummariesConfig();
            config.reportTypeWeights = Arrays.asList(reportTypeWeights);
            config.infectiousnessWeights = Arrays.asList(infectiousnessWeights);
            config.attenuationBucketThresholdDb = attenuationBucketThresholdDb;
            config.attenuationBucketWeights = attenuationBucketWeights;
            config.daysSinceExposureThreshold = daysSinceExposureThreshold;
            config.minimumWindowScore = minimumWindowScore;
            return config;
        }

        /**
         * See {@link #getAttenuationBucketThresholdDb()} and {@link #getAttenuationBucketWeights()}
         */
        public DailySummariesConfigBuilder setAttenuationBuckets(List<Integer> thresholds, List<Double> weights) {
            attenuationBucketThresholdDb = new ArrayList<>(thresholds);
            attenuationBucketWeights = new ArrayList<>(weights);
            return this;
        }

        /**
         * See {@link #getDaysSinceExposureThreshold()}
         */
        public DailySummariesConfigBuilder setDaysSinceExposureThreshold(int daysSinceExposureThreshold) {
            this.daysSinceExposureThreshold = daysSinceExposureThreshold;
            return this;
        }

        /**
         * See {@link #getInfectiousnessWeights()}
         */
        public DailySummariesConfigBuilder setInfectiousnessWeight(@Infectiousness int infectiousness, double weight) {
            infectiousnessWeights[infectiousness] = weight;
            return this;
        }

        /**
         * See {@link #getMinimumWindowScore()}
         */
        public DailySummariesConfigBuilder setMinimumWindowScore(double minimumWindowScore) {
            this.minimumWindowScore = minimumWindowScore;
            return this;
        }

        /**
         * See {@link #getReportTypeWeights()}
         */
        public DailySummariesConfigBuilder setReportTypeWeight(@ReportType int reportType, double weight) {
            reportTypeWeights[reportType] = weight;
            return this;
        }
    }

    public static final Creator<DailySummariesConfig> CREATOR = new AutoCreator<>(DailySummariesConfig.class);
}
