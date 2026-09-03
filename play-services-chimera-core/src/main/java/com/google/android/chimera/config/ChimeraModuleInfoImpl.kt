/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config

import android.os.Bundle
import com.google.android.chimera.config.registry.DynamicModuleRegistry

class ChimeraModuleInfoImpl(
    dynamicModule: DynamicModuleRegistry.DynamicModule,
    moduleApkInfo: ModuleManager.ModuleApkInfo?,
    submoduleId: String?,
    moduleVersion: Int,
): ModuleManager.ModuleInfo(
    dynamicModule.primaryModuleId,
    moduleVersion,
    submoduleId,
    null,
    moduleApkInfo) {

    // microG has no module metadata source, so this is always empty (official callers only check key presence).
    override fun getMetadata(): Bundle = Bundle.EMPTY
}
