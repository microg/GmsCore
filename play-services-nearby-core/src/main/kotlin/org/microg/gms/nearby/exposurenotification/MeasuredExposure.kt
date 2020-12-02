/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.util.Log
import com.google.android.gms.nearby.exposurenotification.*
import java.util.concurrent.TimeUnit

data class PlainExposure(val rpi: ByteArray, val aem: ByteArray, val timestamp: Long, val duration: Long, val rssi: Int)

data class MeasuredExposure(val timestamp: Long, val duration: Long, val rssi: Int, val txPower: Int, @CalibrationConfidence val confidence: Int, val key: TemporaryExposureKey) {
    val attenuation
        get() = txPower - (rssi + currentDeviceInfo.rssiCorrection)
}

fun List<MeasuredExposure>.merge(): List<MergedExposure> {
    val keys = map { it.key }.distinct()
    val result = arrayListOf<MergedExposure>()
    for (key in keys) {
        var merged: MergedExposure? = null
        for (exposure in filter { it.key == key }.distinctBy { it.timestamp }.sortedBy { it.timestamp }) {
            if (merged != null && merged.timestamp + MergedExposure.MAXIMUM_DURATION > exposure.timestamp) {
                merged += exposure
            } else {
                if (merged != null) {
                    result.add(merged)
                }
                merged = MergedExposure(key, exposure.timestamp, exposure.txPower, exposure.confidence, listOf(MergedSubExposure(exposure.attenuation, exposure.duration)))
            }
            if (merged.durationInMinutes >= 30) {
                result.add(merged)
                merged = null
            }
        }
        if (merged != null) {
            result.add(merged)
        }
    }
    return result
}

internal data class MergedSubExposure(val attenuation: Int, val duration: Long)

data class MergedExposure internal constructor(val key: TemporaryExposureKey, val timestamp: Long, val txPower: Int, @CalibrationConfidence val confidence: Int, internal val subs: List<MergedSubExposure>) {
    @RiskLevel
    val transmissionRiskLevel: Int
        get() = key.transmissionRiskLevel

    val durationInMinutes
        get() = TimeUnit.MILLISECONDS.toMinutes(subs.map { it.duration }.sum())

    val daysSinceExposure
        get() = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp)

    val attenuation
        get() = (subs.map { it.attenuation * it.duration }.sum().toDouble() / subs.map { it.duration }.sum().toDouble()).toInt()

    fun getAttenuationRiskScore(configuration: ExposureConfiguration): Int {
        return when {
            attenuation > 73 -> configuration.attenuationScores[0]
            attenuation > 63 -> configuration.attenuationScores[1]
            attenuation > 51 -> configuration.attenuationScores[2]
            attenuation > 33 -> configuration.attenuationScores[3]
            attenuation > 27 -> configuration.attenuationScores[4]
            attenuation > 15 -> configuration.attenuationScores[5]
            attenuation > 10 -> configuration.attenuationScores[6]
            else -> configuration.attenuationScores[7]
        }
    }

    fun getDaysSinceLastExposureRiskScore(configuration: ExposureConfiguration): Int {
        return when {
            daysSinceExposure >= 14 -> configuration.daysSinceLastExposureScores[0]
            daysSinceExposure >= 12 -> configuration.daysSinceLastExposureScores[1]
            daysSinceExposure >= 10 -> configuration.daysSinceLastExposureScores[2]
            daysSinceExposure >= 8 -> configuration.daysSinceLastExposureScores[3]
            daysSinceExposure >= 6 -> configuration.daysSinceLastExposureScores[4]
            daysSinceExposure >= 4 -> configuration.daysSinceLastExposureScores[5]
            daysSinceExposure >= 2 -> configuration.daysSinceLastExposureScores[6]
            else -> configuration.daysSinceLastExposureScores[7]
        }
    }

    fun getDurationRiskScore(configuration: ExposureConfiguration): Int {
        return when {
            durationInMinutes == 0L -> configuration.durationScores[0]
            durationInMinutes <= 5 -> configuration.durationScores[1]
            durationInMinutes <= 10 -> configuration.durationScores[2]
            durationInMinutes <= 15 -> configuration.durationScores[3]
            durationInMinutes <= 20 -> configuration.durationScores[4]
            durationInMinutes <= 25 -> configuration.durationScores[5]
            durationInMinutes <= 30 -> configuration.durationScores[6]
            else -> configuration.durationScores[7]
        }
    }

    fun getTransmissionRiskScore(configuration: ExposureConfiguration): Int {
        return when (transmissionRiskLevel) {
            RiskLevel.RISK_LEVEL_LOWEST -> configuration.transmissionRiskScores[0]
            RiskLevel.RISK_LEVEL_LOW -> configuration.transmissionRiskScores[1]
            RiskLevel.RISK_LEVEL_LOW_MEDIUM -> configuration.transmissionRiskScores[2]
            RiskLevel.RISK_LEVEL_MEDIUM -> configuration.transmissionRiskScores[3]
            RiskLevel.RISK_LEVEL_MEDIUM_HIGH -> configuration.transmissionRiskScores[4]
            RiskLevel.RISK_LEVEL_HIGH -> configuration.transmissionRiskScores[5]
            RiskLevel.RISK_LEVEL_VERY_HIGH -> configuration.transmissionRiskScores[6]
            RiskLevel.RISK_LEVEL_HIGHEST -> configuration.transmissionRiskScores[7]
            else -> 1
        }
    }

    fun getRiskScore(configuration: ExposureConfiguration): Int {
        val risk = getAttenuationRiskScore(configuration) * getDaysSinceLastExposureRiskScore(configuration) * getDurationRiskScore(configuration) * getTransmissionRiskScore(configuration)
        Log.d(TAG, "Risk score calculation: ${getAttenuationRiskScore(configuration)} * ${getDaysSinceLastExposureRiskScore(configuration)} * ${getDurationRiskScore(configuration)} * ${getTransmissionRiskScore(configuration)} = $risk")
        if (risk < configuration.minimumRiskScore) return 0
        return risk
    }

    fun getAttenuationDurations(configuration: ExposureConfiguration): IntArray {
        return intArrayOf(
                TimeUnit.MILLISECONDS.toMinutes(subs.filter { it.attenuation < configuration.durationAtAttenuationThresholds[0] }.map { it.duration }.sum()).toInt(),
                TimeUnit.MILLISECONDS.toMinutes(subs.filter { it.attenuation >= configuration.durationAtAttenuationThresholds[0] && it.attenuation < configuration.durationAtAttenuationThresholds[1] }.map { it.duration }.sum()).toInt(),
                TimeUnit.MILLISECONDS.toMinutes(subs.filter { it.attenuation >= configuration.durationAtAttenuationThresholds[1] }.map { it.duration }.sum()).toInt()
        )
    }

    fun toExposureInformation(configuration: ExposureConfiguration): ExposureInformation =
            ExposureInformation.ExposureInformationBuilder()
                    .setDateMillisSinceEpoch(key.rollingStartIntervalNumber.toLong() * ROLLING_WINDOW_LENGTH_MS)
                    .setDurationMinutes(durationInMinutes.toInt())
                    .setAttenuationValue(attenuation)
                    .setTransmissionRiskLevel(transmissionRiskLevel)
                    .setTotalRiskScore(getRiskScore(configuration))
                    .setAttenuationDurations(getAttenuationDurations(configuration))
                    .build()

    operator fun plus(exposure: MeasuredExposure): MergedExposure = copy(subs = subs + MergedSubExposure(exposure.attenuation, exposure.duration))

    companion object {
        const val MAXIMUM_DURATION = 30 * 60 * 1000
    }
}
