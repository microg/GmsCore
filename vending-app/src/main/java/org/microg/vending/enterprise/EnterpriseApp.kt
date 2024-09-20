package org.microg.vending.enterprise

import com.google.android.finsky.AppInstallPolicy

class EnterpriseApp(
    packageName: String,
    versionCode: Int?,
    displayName: String,
    state: State,
    iconUrl: String?,
    deliveryToken: String?,
    val policy: AppInstallPolicy
) : App(packageName, versionCode, displayName, state, iconUrl, deliveryToken)