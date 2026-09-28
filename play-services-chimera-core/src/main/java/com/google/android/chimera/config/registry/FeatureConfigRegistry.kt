/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config.registry

data class FeatureConfig(
    val featureName: String,
    val featureVersion: Int
)

object FeatureConfigRegistry {
    private val features = listOf(
        FeatureConfig("chimera_debug", 1),
        FeatureConfig("dynamiteloader", 2),
        FeatureConfig("loader_mp_result_code", 1),
        FeatureConfig("mlkit.barcode.ui", 1),
        FeatureConfig("module_flag_control", 1),
        FeatureConfig("moduleinstall", 7),
        FeatureConfig("vision.barcode", 1),
    )
    val featureMap: Map<String, FeatureConfig> = features.associateBy { it.featureName }
}
