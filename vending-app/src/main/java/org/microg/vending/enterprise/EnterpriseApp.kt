package org.microg.vending.enterprise

import org.microg.vending.enterprise.proto.AppInstallPolicy

class EnterpriseApp(
    packageName: String,
    versionCode: Int?,
    displayName: String,
    iconUrl: String?,
    deliveryToken: String?,
    dependencies: List<App>,
    val policy: AppInstallPolicy
) : App(packageName, versionCode, displayName, iconUrl, dependencies, deliveryToken)