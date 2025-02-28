package com.google.android.finsky.splitinstallservice

data class PackageComponent(
    val packageName: String,
    val componentName: String,
    val url: String,
    /**
     * Size in bytes
     */
    val size: Long
)