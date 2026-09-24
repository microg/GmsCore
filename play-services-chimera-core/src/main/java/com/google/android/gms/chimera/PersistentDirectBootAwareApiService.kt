/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.chimera

import androidx.annotation.Keep

/**
 * Chimera bound service proxy for persistent direct-boot-aware API services.
 *
 * Used as a routing target for chimera bound services like clearcut.
 *
 * Same as PersistentApiService but intended for services that need to run
 * before the user unlocks the device (direct boot mode).
 */
@Keep
class PersistentDirectBootAwareApiService : GmsApiService()
