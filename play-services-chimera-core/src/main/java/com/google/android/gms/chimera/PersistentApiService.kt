/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.chimera

import androidx.annotation.Keep

/**
 * Chimera bound service proxy for persistent (non-instant-app) API services.
 *
 * Used as a routing target for chimera bound services like phenotype.
 *
 * When a dynamically loaded module calls:
 *   BoundService.getStartIntent(context, "com.google.android.gms.phenotype.service.START")
 * The Intent is routed to this service class via chimera-action scheme,
 * which then delegates to the real PhenotypeService implementation.
 */
@Keep
class PersistentApiService : GmsApiService()
