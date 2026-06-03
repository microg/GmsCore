package org.microg.gms.wearable;

/**
 * Canonical message and data paths for all WearOS communication.
 *
 * RULES:
 * 1. This file is the ONLY place where these paths are defined.
 * 2. All subsystems MUST import from here. Never hardcode paths.
 * 3. Convention: lowercase, singular nouns, slash-separated.
 *
 * WHY THIS EXISTS:
 * The original implementation had path constants scattered across
 * multiple files with inconsistent singular/plural naming. Messages
 * sent to "/microg/notification/dismiss" were never received by a
 * listener registered on "/microg/notifications/dismiss". This class
 * eliminates that class of bug entirely.
 */
public final class WearableDataPaths {

    private WearableDataPaths() {}

    // --- Capability Advertisement ---
    public static final String CAPABILITIES          = "/microg/capabilities";

    // --- Cloud Sync ---
    public static final String CLOUD_SYNC             = "/microg/cloudsync";
    public static final String CLOUD_SYNC_ACCOUNT     = "/microg/cloudsync/account";

    // --- Notifications (Phone → Watch) ---
    public static final String NOTIF_FORWARD          = "/microg/notification";
    public static final String NOTIF_DISMISS          = "/microg/notification/dismiss";
    public static final String NOTIF_REPLY            = "/microg/notification/reply";
    public static final String NOTIF_BATCH            = "/microg/notification/batch";

    // --- Media (Phone ↔ Watch) ---
    public static final String MEDIA_METADATA         = "/microg/media/metadata";
    public static final String MEDIA_STATE            = "/microg/media/state";
    public static final String MEDIA_COMMAND           = "/microg/media/command";
    public static final String MEDIA_DISCONNECT       = "/microg/media/disconnect";

    // --- Heartbeat (Phone ↔ Watch) ---
    public static final String HEARTBEAT              = "/microg/heartbeat";
    public static final String HEARTBEAT_ACK          = "/microg/heartbeat/ack";

    // --- App Install (Phone → Watch) ---
    public static final String APP_INSTALL            = "/microg/apps/install";
}
