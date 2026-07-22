/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

open class App(
    val packageName: String,
    val versionCode: Int?,
    val displayName: String,
    val iconUrl: String?,
    val dependencies: List<App>,
    val deliveryToken: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is App) return false

        if (packageName != other.packageName) return false

        return true
    }

    override fun hashCode(): Int = packageName.hashCode()
}