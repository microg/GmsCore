/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

import com.google.android.gms.common.Feature;

/**
 * Features advertised by Constellation (svc 155) and Asterism (svc 199).
 * Versions must match or exceed what Messages requests.
 */
public final class RcsFeatures {
    public static final Feature[] SUPPORTED = new Feature[] {
        new Feature("asterism_consent", 3),
        new Feature("one_time_verification", 1),
        new Feature("carrier_auth", 1),
        new Feature("verify_phone_number", 2),
        new Feature("get_iid_token", 1),
        new Feature("get_pnv_capabilities", 1),
        new Feature("ts43", 1),
        new Feature("verify_phone_number_local_read", 1)
    };

    private RcsFeatures() {}
}
