package org.microg.vending.enterprise

import org.microg.vending.enterprise.proto.AppInstallPolicy

class EnterpriseApp(
    packageName: String,
    versionCode: Int?,
    displayName: String,
    iconUrl: String?,
    deliveryToken: String?,
    val policy: AppInstallPolicy
) : App(packageName, versionCode, displayName, iconUrl, deliveryToken)