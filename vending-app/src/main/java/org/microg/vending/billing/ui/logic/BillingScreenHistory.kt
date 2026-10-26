/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing.ui.logic

internal class BillingScreenHistory {
    private companion object {
        const val MAX_SIZE = 32
    }

    private val screenIds = mutableListOf<String>()

    fun recordTransition(fromScreenId: String?, toScreenId: String?): Boolean {
        if (fromScreenId == null || toScreenId == null || fromScreenId == toScreenId) return false

        val targetIndex = screenIds.lastIndexOf(toScreenId)
        if (targetIndex >= 0) {
            screenIds.subList(targetIndex, screenIds.size).clear()
            return true
        }

        if (screenIds.lastOrNull() == fromScreenId) return false
        screenIds.add(fromScreenId)
        if (screenIds.size > MAX_SIZE) screenIds.removeAt(0)
        return true
    }

    fun popPrevious(currentScreenId: String?, isAvailable: (String) -> Boolean): String? {
        while (screenIds.isNotEmpty()) {
            val screenId = screenIds.removeAt(screenIds.lastIndex)
            if (screenId != currentScreenId && isAvailable(screenId)) return screenId
        }
        return null
    }
}
