/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.context

import com.google.android.chimera.loader.ChimeraModuleApk

data class ModuleContextRef(
    val moduleName: String?,
    val moduleApiClassname: String,
    val chimeraModuleApk: ChimeraModuleApk,
    val moduleContext: ModuleContext,
    val classLoader: ClassLoader,
    val apkSha256: String? = null
)
