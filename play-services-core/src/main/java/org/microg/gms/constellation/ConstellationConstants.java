/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

/**
 * Shared constants for Constellation (svc 155) and Asterism (svc 199) services.
 */
public final class ConstellationConstants {
    /** SharedPreferences file holding DG token cache, EC key pair, verification state. */
    public static final String PREFS_CONSTELLATION = "constellation_prefs";

    /** SharedPreferences file holding per-sender IID tokens. */
    public static final String PREFS_CONSTELLATION_IID = "constellation_iid";

    /** Broadcast action emitted by our silent-SMS PendingIntent (carries SMS PDU extras). */
    public static final String ACTION_SILENT_SMS_RECEIVED =
            "com.google.android.gms.constellation.SILENT_SMS_RECEIVED";

    /** FCM sender ID Messages passes via clqs.java:40 (getIidToken request.a). */
    public static final String SENDER_MESSAGES_IID = "466216207879";

    /** Constellation default sender (GMS Phenotype IidToken__default_project_number). */
    public static final String SENDER_CONSTELLATION = "496232013492";

    /** Read-only PhoneNumber API sender (GMS Phenotype IidToken__read_only_project_number). */
    public static final String SENDER_READ_ONLY = "745476177629";

    private ConstellationConstants() {}
}
