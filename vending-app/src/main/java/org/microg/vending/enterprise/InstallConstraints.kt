/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

data class InstallConstraints(
    val requiresUnmeteredNetwork: Boolean,
    val requiresCharging: Boolean,
    val requiresDeviceIdle: Boolean
) {

    fun isSatisfiedBy(available: InstallConstraints): Boolean =
        (!requiresUnmeteredNetwork || available.requiresUnmeteredNetwork) &&
            (!requiresCharging || available.requiresCharging) &&
            (!requiresDeviceIdle || available.requiresDeviceIdle)

    fun toMask(): Int {
        var mask = 0

        if (requiresUnmeteredNetwork) {
            mask = mask or UNMETERED_NETWORK
        }

        if (requiresCharging) {
            mask = mask or CHARGING
        }

        if (requiresDeviceIdle) {
            mask = mask or DEVICE_IDLE
        }

        return mask
    }

    companion object {
        private const val UNMETERED_NETWORK = 1
        private const val CHARGING = 2
        private const val DEVICE_IDLE = 4

        const val MASK_COUNT = 8

        val NONE = InstallConstraints(
            requiresUnmeteredNetwork = false,
            requiresCharging = false,
            requiresDeviceIdle = false
        )

        fun fromMask(mask: Int) = InstallConstraints(
            requiresUnmeteredNetwork = mask and UNMETERED_NETWORK != 0,
            requiresCharging = mask and CHARGING != 0,
            requiresDeviceIdle = mask and DEVICE_IDLE != 0
        )
    }
}
