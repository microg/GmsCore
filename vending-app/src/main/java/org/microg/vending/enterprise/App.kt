package org.microg.vending.enterprise

open class App(
    val packageName: String,
    val versionCode: Int?,
    val displayName: String,
    val iconUrl: String?,
    val dependencies: List<App>,
    val deliveryToken: String?
)