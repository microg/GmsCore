/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import org.microg.vending.enterprise.proto.AppInstallPolicy

class EnterpriseApp(
    packageName: String,
    versionCode: Int?,
    displayName: String,
    iconUrl: String?,
    deliveryToken: String?,
    dependencies: List<App>,
    val policy: AppInstallPolicy,
    val installConstraints: List<InstallConstraints> = listOf(InstallConstraints.NONE)
) : App(packageName, versionCode, displayName, iconUrl, dependencies, deliveryToken) {

    val isRequired: Boolean
        get() = policy == AppInstallPolicy.MANDATORY || policy == AppInstallPolicy.FORCE_INSTALLED

    fun canBeInstalledUnder(available: InstallConstraints): Boolean =
        installConstraints.any { it.isSatisfiedBy(available) }
}
