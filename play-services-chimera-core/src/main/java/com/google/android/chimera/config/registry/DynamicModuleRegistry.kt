/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config.registry

object DynamicModuleRegistry {

    const val MODULE_ID_MLKIT_DOCSCAN_CROP = "com.google.android.gms.mlkit_docscan_crop"
    const val MODULE_ID_MLKIT_DOCSCAN_DETECT = "com.google.android.gms.mlkit_docscan_detect"
    const val MODULE_ID_MLKIT_DOCSCAN_ENHANCE = "com.google.android.gms.mlkit_docscan_enhance"
    const val MODULE_ID_MLKIT_DOCSCAN_UI = "com.google.android.gms.mlkit_docscan_ui"

    val MLKIT_DOCUMENT_SCANNER_MODULE_IDS: Set<String> = linkedSetOf(
        MODULE_ID_MLKIT_DOCSCAN_CROP,
        MODULE_ID_MLKIT_DOCSCAN_DETECT,
        MODULE_ID_MLKIT_DOCSCAN_ENHANCE,
        MODULE_ID_MLKIT_DOCSCAN_UI,
    )

    data class DynamicModule(
        val moduleName: String,
        val moduleIds: List<String> = emptyList(),
    ) {
        val primaryModuleId: String get() = moduleIds.firstOrNull().orEmpty()
    }

    val modules: List<DynamicModule> = listOf(
        DynamicModule(
            moduleName = "ROOT",
            moduleIds = listOf("", "com.google.android.gms.tflite"),
        ),
        DynamicModule(
            moduleName = "MlkitDocscan.optional",
            moduleIds = listOf(
                MODULE_ID_MLKIT_DOCSCAN_CROP,
                MODULE_ID_MLKIT_DOCSCAN_DETECT,
                MODULE_ID_MLKIT_DOCSCAN_ENHANCE,
            ),
        ),
        DynamicModule(
            moduleName = "MlkitDocscanUi.optional",
            moduleIds = listOf(MODULE_ID_MLKIT_DOCSCAN_UI),
        ),
        DynamicModule(
            moduleName = "TfliteDynamiteDynamite.integ",
            moduleIds = listOf("com.google.android.gms.tflite_dynamite"),
        ),
    )

    private const val GMS_MODULE_PREFIX = "com.google.android.gms."

    private val byModuleId: Map<String, DynamicModule> =
        modules.flatMap { m -> m.moduleIds.map { it to m } }.toMap()

    fun getByModuleId(moduleId: String): DynamicModule? {
        byModuleId[moduleId]?.let { return it }
        for (alias in moduleIdAliases(moduleId)) {
            byModuleId[alias]?.let { return it }
        }
        return null
    }

    /**
     * Stable, trusted storage/config name for a signed module ID. Unknown future modules use their signed
     * moduleId directly; an unsigned .mods path or mapping name is never used as an ownership key.
     */
    fun canonicalModuleName(moduleId: String): String = getByModuleId(moduleId)?.moduleName ?: moduleId

    private fun moduleIdAliases(moduleId: String): List<String> {
        if (!moduleId.startsWith(GMS_MODULE_PREFIX)) return emptyList()
        val suffix = moduleId.removePrefix(GMS_MODULE_PREFIX)
        val aliases = linkedSetOf<String>()
        aliases += GMS_MODULE_PREFIX + suffix.replace('_', '.')
        aliases += GMS_MODULE_PREFIX + suffix.replace('.', '_')
        aliases.remove(moduleId)
        return aliases.toList()
    }
}
