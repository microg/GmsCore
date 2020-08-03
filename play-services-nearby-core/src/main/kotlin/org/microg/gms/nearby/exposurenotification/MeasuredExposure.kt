/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.RiskLevel
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import java.util.concurrent.TimeUnit

data class MeasuredExposure(val rpi: ByteArray, val aem: ByteArray, val timestamp: Long, val duration: Long, val rssi: Int, val notCorrectedAttenuation: Int = 0, val key: TemporaryExposureKey? = null) {
    @RiskLevel
    val transmissionRiskLevel: Int
        get() = key?.transmissionRiskLevel ?: RiskLevel.RISK_LEVEL_INVALID
    
    val durationInMinutes
        get() = TimeUnit.MILLISECONDS.toMinutes(duration)

    val daysSinceExposure
        get() = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp)

    val attenuation
        get() = notCorrectedAttenuation - currentDeviceInfo.rssiCorrection

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
        return getAttenuationRiskScore(configuration) * getDaysSinceLastExposureRiskScore(configuration) * getDurationRiskScore(configuration) * getTransmissionRiskScore(configuration)
    }

    fun getAttenuationDurations(configuration: ExposureConfiguration): IntArray {
        return when {
            attenuation < configuration.durationAtAttenuationThresholds[0] -> intArrayOf(durationInMinutes.toInt(), 0, 0)
            attenuation < configuration.durationAtAttenuationThresholds[1] -> intArrayOf(0, durationInMinutes.toInt(), 0)
            else -> intArrayOf(0, 0, durationInMinutes.toInt())
        }
    }

    fun toExposureInformation(configuration: ExposureConfiguration): ExposureInformation =
            ExposureInformation.ExposureInformationBuilder()
                    .setDateMillisSinceEpoch(timestamp)
                    .setDurationMinutes(durationInMinutes.toInt())
                    .setAttenuationValue(attenuation)
                    .setTransmissionRiskLevel(transmissionRiskLevel)
                    .setTotalRiskScore(getRiskScore(configuration))
                    .setAttenuationDurations(getAttenuationDurations(configuration))
                    .build()
}
