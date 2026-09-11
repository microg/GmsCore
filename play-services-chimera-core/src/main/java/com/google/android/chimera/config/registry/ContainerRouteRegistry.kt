/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config.registry

import com.google.android.chimera.config.ComponentRoute

object ContainerRouteRegistry {

    val serviceRoutes: List<ComponentRoute> = listOf(
        ComponentRoute.build {
            containerName = ".chimera.container.moduleinstall.ModuleInstallService.START"
            moduleChimeraName = ".chimera.GmsApiService"
        },
        ComponentRoute.build {
            containerName = ".phenotype.service.START"
            moduleChimeraName = ".chimera.PersistentApiService"
        },
        ComponentRoute.build {
            containerName = ".clearcut.service.START"
            moduleChimeraName = ".chimera.PersistentDirectBootAwareApiService"
        },
    )
}
