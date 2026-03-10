/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

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