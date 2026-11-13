/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

/** Keep a locally resolved slot authoritative, then retain the response's SIM association. */
internal fun resolveVerificationSimSlot(
    imsi: String?,
    imsiToSlotMap: Map<String, Int>,
    associatedSlotIndex: Int?
): Int = imsi?.let { imsiToSlotMap[it] }?.takeIf { it >= 0 }
    ?: associatedSlotIndex?.takeIf { it >= 0 }
    ?: -1
