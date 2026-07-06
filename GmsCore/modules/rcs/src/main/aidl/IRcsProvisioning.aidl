package org.microg.gms.rcs;

interface IRcsProvisioning {
    void provision(String imsi, String msisdn);
    int getProvisioningStatus();
}
