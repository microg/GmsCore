/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

/**
 * Stock GMS names DroidGuard VM cache directories with uppercase hex of the
 * VM checksum. okio ByteString.hex() is lowercase and shows up in
 * /proc/self/maps, which the DG VM samples.
 */
object DroidGuardVmCache {
    fun vmKey(hexChecksum: String): String = hexChecksum.uppercase()
}
