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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mappings from diagnosis keys data to concepts returned by the API.
 */
@PublicApi
public class DiagnosisKeysDataMapping extends AutoSafeParcelable {
    @Field(1)
    private List<Integer> daysSinceOnsetToInfectiousness;
    @Field(2)
    @ReportType
    private int reportTypeWhenMissing;
    @Field(3)
    @Infectiousness
    private int infectiousnessWhenDaysSinceOnsetMissing;

    /**
     * Mapping from diagnosisKey.daysSinceOnsetOfSymptoms to {@link Infectiousness}.
     * <p>
     * Infectiousness is computed from this mapping and the tek metadata as - daysSinceOnsetToInfectiousness[{@link TemporaryExposureKey#getDaysSinceOnsetOfSymptoms()}], or - {@link #getInfectiousnessWhenDaysSinceOnsetMissing()} if {@link TemporaryExposureKey#getDaysSinceOnsetOfSymptoms()} is {@link TemporaryExposureKey#DAYS_SINCE_ONSET_OF_SYMPTOMS_UNKNOWN}.
     * <p>
     * Values of DaysSinceOnsetOfSymptoms that aren't represented in this map are given {@link Infectiousness#NONE} as infectiousness. Exposures with infectiousness equal to {@link Infectiousness#NONE} are dropped.
     */
    public Map<Integer, Integer> getDaysSinceOnsetToInfectiousness() {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < daysSinceOnsetToInfectiousness.size(); i++) {
            map.put(i, daysSinceOnsetToInfectiousness.get(i));
        }
        return map;
    }

    /**
     * Infectiousness of TEKs for which onset of symptoms is not set.
     * <p>
     * See {@link #getDaysSinceOnsetToInfectiousness()} for more info.
     */
    public int getInfectiousnessWhenDaysSinceOnsetMissing() {
        return infectiousnessWhenDaysSinceOnsetMissing;
    }

    /**
     * Report type to default to when a TEK has no report type set.
     * <p>
     * This report type gets used when creating the {@link ExposureWindow}s and the {@link DailySummary}s. The system will treat TEKs with missing report types as if they had this provided report type.
     */
    public int getReportTypeWhenMissing() {
        return reportTypeWhenMissing;
    }

    /**
     * A builder for {@link DiagnosisKeysDataMapping}.
     */
    public static class DiagnosisKeysDataMappingBuilder {
        private final static int MAX_DAYS = 29;
        private List<Integer> daysSinceOnsetToInfectiousness;
        @ReportType
        private int reportTypeWhenMissing = ReportType.UNKNOWN;
        @Infectiousness
        private Integer infectiousnessWhenDaysSinceOnsetMissing;

        public DiagnosisKeysDataMapping build() {
            if (daysSinceOnsetToInfectiousness == null)
                throw new IllegalStateException("Must set daysSinceOnsetToInfectiousness");
            if (reportTypeWhenMissing == ReportType.UNKNOWN)
                throw new IllegalStateException("Must set reportTypeWhenMissing");
            if (infectiousnessWhenDaysSinceOnsetMissing == null)
                throw new IllegalStateException("Must set infectiousnessWhenDaysSinceOnsetMissing");
            DiagnosisKeysDataMapping mapping = new DiagnosisKeysDataMapping();
            mapping.daysSinceOnsetToInfectiousness = daysSinceOnsetToInfectiousness;
            mapping.reportTypeWhenMissing = reportTypeWhenMissing;
            mapping.infectiousnessWhenDaysSinceOnsetMissing = infectiousnessWhenDaysSinceOnsetMissing;
            return mapping;
        }

        public DiagnosisKeysDataMappingBuilder setDaysSinceOnsetToInfectiousness(Map<Integer, Integer> daysSinceOnsetToInfectiousness) {
            if (daysSinceOnsetToInfectiousness.size() > MAX_DAYS)
                throw new IllegalArgumentException("daysSinceOnsetToInfectiousness exceeds " + MAX_DAYS + " days");
            Integer[] values = new Integer[MAX_DAYS];
            Arrays.fill(values, 0);
            for (Map.Entry<Integer, Integer> entry : daysSinceOnsetToInfectiousness.entrySet()) {
                if (entry.getKey() > 14) throw new IllegalArgumentException("invalid day since onset");
                values[entry.getKey() + 14] = entry.getValue();
            }
            this.daysSinceOnsetToInfectiousness = Arrays.asList(values);
            return this;
        }

        public DiagnosisKeysDataMappingBuilder setInfectiousnessWhenDaysSinceOnsetMissing(@Infectiousness int infectiousnessWhenDaysSinceOnsetMissing) {
            this.infectiousnessWhenDaysSinceOnsetMissing = infectiousnessWhenDaysSinceOnsetMissing;
            return this;
        }

        public DiagnosisKeysDataMappingBuilder setReportTypeWhenMissing(@ReportType int reportTypeWhenMissing) {
            this.reportTypeWhenMissing = reportTypeWhenMissing;
            return this;
        }
    }

    public static final Creator<DiagnosisKeysDataMapping> CREATOR = new AutoCreator<>(DiagnosisKeysDataMapping.class);
}
