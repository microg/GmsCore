/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import java.util.Locale

/** Matches stock GMS: context.getDir("dg_cache", MODE_PRIVATE) → app_dg_cache/ */
const val DG_CACHE_FOLDER_NAME = "dg_cache"

/**
 * Stock GMS uses uppercase hex for VM cache subdirectory names.
 * okio ByteString.hex() returns lowercase, which mismatches /proc/self/maps paths.
 */
fun formatVmCacheKey(vmChecksumHex: String): String = vmChecksumHex.uppercase(Locale.US)
