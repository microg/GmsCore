package org.microg.vending.enterprise

import com.google.android.finsky.AppInstallPolicy

class EnterpriseApp(
    packageName: String,
    displayName: String,
    state: State,
    iconUrl: String?,
    val policy: AppInstallPolicy,
) : App(packageName, displayName, state, iconUrl)