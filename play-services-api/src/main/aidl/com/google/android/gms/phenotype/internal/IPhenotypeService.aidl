package com.google.android.gms.phenotype.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.phenotype.internal.IPhenotypeCallbacks;
import com.google.android.gms.phenotype.Flag;
import com.google.android.gms.phenotype.RegistrationInfo;

interface IPhenotypeService {
    oneway void register(IPhenotypeCallbacks callbacks, String packageName, int version, in String[] p3, in byte[] p4) = 0; // returns via callbacks.onRegistered()
    oneway void weakRegister(IPhenotypeCallbacks callbacks, String packageName, int version, in String[] p3, in int[] p4, in byte[] p5) = 1; // returns via callbacks.onWeakRegistered()
    oneway void unregister(IPhenotypeCallbacks callbacks, String packageName) = 2; // returns via callbacks.onUnregistered()
    oneway void getConfigurationSnapshot(IPhenotypeCallbacks callbacks, String packageName, String user) = 3; // returns via callbacks.onConfiguration()
    oneway void commitToConfiguration(IPhenotypeCallbacks callbacks, String snapshotToken) = 4; // returns via callbacks.onCommitedToConfiguration()
    oneway void getExperimentTokens(IPhenotypeCallbacks callbacks, String packageName, String logSourceName) = 5; // returns via callbacks.onExperimentTokens()
    oneway void getDogfoodsToken(IPhenotypeCallbacks callbacks) = 6; // returns via callbacks.onDogfoodsToken()
    oneway void setDogfoodsToken(IPhenotypeCallbacks callbacks, in byte[] p1) = 7; // returns via callbacks.onDogfoodsTokenSet()
    oneway void getFlag(IPhenotypeCallbacks callbacks, String packageName, String name, int type) = 8; // returns via callbacks.onFlag()
    oneway void getCommitedConfiguration(IPhenotypeCallbacks callbacks, String packageName) = 9; // returns via callbacks.onCommittedConfiguration()
    oneway void getConfigurationSnapshot2(IPhenotypeCallbacks callbacks, String packageName, String user, String p3) = 10; // returns via callbacks.onConfiguration()
    oneway void syncAfterOperation(IPhenotypeCallbacks callbacks, String packageName, long version) = 11; // returns via callbacks.onSyncFinished()
    oneway void registerSync(IPhenotypeCallbacks callbacks, String packageName, int version, in String[] p3, in byte[] p4, String p5, String p6) = 12; // returns via callbacks.onConfiguration()
    oneway void setFlagOverrides(IPhenotypeCallbacks callbacks, String packageName, String user, String flagName, int flagType, int flagDataType, String flagValue) = 13; // returns via callbacks.onFlagOverridesSet()
    oneway void deleteFlagOverrides(IPhenotypeCallbacks callbacks, String packageName, String user, String flagName) = 14; // returns via callbacks.onFlagOverrides()
    oneway void listFlagOverrides(IPhenotypeCallbacks callbacks, String packageName, String user, String flagName) = 15; // returns via callbacks.onFlagOverrides()

    oneway void clearFlagOverrides(IPhenotypeCallbacks callbacks, String packageName, String user) = 17; // returns via callbacks.onFlagOverridesSet()
    oneway void bulkRegister(IPhenotypeCallbacks callbacks, in RegistrationInfo[] infos) = 18; // returns via callbacks.onRegister()
    oneway void setAppSpecificProperties(IPhenotypeCallbacks callbacks, String packageName, in byte[] p2) = 19; // returns via callbacks.onAppSpecificPropertiesSet()

    oneway void getServingVersion(IPhenotypeCallbacks callbacks) = 21; // returns via callbacks.onServingVersion()
    oneway void getExperimentTokensForLogging(IPhenotypeCallbacks callbacks, String packageName, String logSourceName, String p3, String clientPackageName) = 22; // returns via callbacks.onExperimentTokens()
    oneway void syncAllAfterOperation(IPhenotypeCallbacks callbacks, long p1) = 23; // returns via callbacks.onSyncFinished()
    oneway void setRuntimeProperties(IStatusCallback callbacks, String p1, in byte[] p2) = 24;
//    oneway void setExternalExperiments(IStatusCallback callbacks, String p1, in List<byte[]> p2) = 25;
}
