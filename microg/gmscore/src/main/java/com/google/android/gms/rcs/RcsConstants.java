package com.google.android.gms.rcs;

public class RcsConstants {
    public static final String ACTION_RCS_SERVICE = "com.google.android.gms.rcs.START";
    public static final String PERMISSION_RCS = "com.google.android.gms.permission.RCS";

    // RCS provisioning states
    public static final int PROVISIONING_STATE_NOT_READY = 0;
    public static final int PROVISIONING_STATE_CAPABLE = 1;
    public static final int PROVISIONING_STATE_PROVISIONED = 2;

    // Default carrier configuration values
    public static final String DEFAULT_IMS_URI = "sip:ims.mnc000.mcc000.3gppnetwork.org";
    public static final String DEFAULT_RCS_URI = "sip:rcs@ims.mnc000.mcc000.3gppnetwork.org";

    // Intent extras
    public static final String EXTRA_PHONE_NUMBER = "phone_number";
    public static final String EXTRA_CONFIG = "rcs_config";
    public static final String EXTRA_PROVISIONING_STATE = "provisioning_state";
}
