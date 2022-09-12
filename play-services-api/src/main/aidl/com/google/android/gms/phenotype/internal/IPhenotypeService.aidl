package com.google.android.gms.phenotype.internal;

import com.google.android.gms.phenotype.internal.IPhenotypeCallbacks;
import com.google.android.gms.phenotype.Flag;
import com.google.android.gms.phenotype.RegistrationInfo;

interface IPhenotypeService {
    oneway void register(IPhenotypeCallbacks callbacks, String p1, int p2, in String[] p3, in byte[] p4) = 0; // returns via callbacks.onRegistered()
    oneway void weakRegister(IPhenotypeCallbacks callbacks, String p1, int p2, in String[] p3, in int[] p4, in byte[] p5) = 1; // returns via callbacks.onWeakRegistered()
    oneway void unregister(IPhenotypeCallbacks callbacks, String p1) = 2; // returns via callbacks.onUnregistered()
    oneway void getConfigurationSnapshot(IPhenotypeCallbacks callbacks, String p1, String p2) = 3; // returns via callbacks.onConfiguration()
    oneway void commitToConfiguration(IPhenotypeCallbacks callbacks, String p1) = 4; // returns via callbacks.onCommitedToConfiguration()
    oneway void getExperimentTokens(IPhenotypeCallbacks callbacks, String p1, String logSourceName) = 5; // returns via callbacks.onExperimentTokens()
    oneway void getDogfoodsToken(IPhenotypeCallbacks callbacks) = 6; // returns via callbacks.onDogfoodsToken()
    oneway void setDogfoodsToken(IPhenotypeCallbacks callbacks, in byte[] p1) = 7; // returns via callbacks.onDogfoodsTokenSet()
    oneway void getFlag(IPhenotypeCallbacks callbacks, String packageName, String name, int type) = 8; // returns via callbacks.onFlag()
    oneway void getCommitedConfiguration(IPhenotypeCallbacks callbacks, String p1) = 9; // returns via callbacks.onCommittedConfiguration()
    oneway void getConfigurationSnapshot2(IPhenotypeCallbacks callbacks, String p1, String p2, String p3) = 10; // returns via callbacks.onConfiguration()
    oneway void syncAfterOperation(IPhenotypeCallbacks callbacks, String p1, long p2) = 11; // returns via callbacks.onSyncFinished()
    oneway void registerSync(IPhenotypeCallbacks callbacks, String p1, int p2, in String[] p3, in byte[] p4, String p5, String p6) = 12; // returns via callbacks.onConfiguration()
    oneway void setFlagOverrides(IPhenotypeCallbacks callbacks, String packageName, String user, String flagName, int flagType, int flagDataType, String flagValue) = 13; // returns via callbacks.onFlagOverridesSet()
    oneway void deleteFlagOverrides(IPhenotypeCallbacks callbacks, String packageName, String user, String flagName) = 14; // returns via callbacks.onFlagOverrides()
    oneway void listFlagOverrides(IPhenotypeCallbacks callbacks, String packageName, String user, String flagName) = 15; // returns via callbacks.onFlagOverrides()

    oneway void clearFlagOverrides(IPhenotypeCallbacks callbacks, String packageName, String user) = 17; // returns via callbacks.onFlagOverridesSet()
    oneway void bulkRegister(IPhenotypeCallbacks callbacks, in RegistrationInfo[] infos) = 18; // returns via callbacks.onRegister()
    oneway void setAppSpecificProperties(IPhenotypeCallbacks callbacks, String p1, in byte[] p2) = 19; // returns via callbacks.onAppSpecificPropertiesSet()

    oneway void getServingVersion(IPhenotypeCallbacks callbacks) = 21; // returns via callbacks.onServingVersion()
    oneway void getExperimentTokens2(IPhenotypeCallbacks callbacks, String p1, String p2, String p3, String p4) = 22; // returns via callbacks.onExperimentTokens()
}
