package com.google.android.gms.phenotype.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.phenotype.Configurations;
import com.google.android.gms.phenotype.DogfoodsToken;
import com.google.android.gms.phenotype.ExperimentTokens;
import com.google.android.gms.phenotype.Flag;
import com.google.android.gms.phenotype.FlagOverrides;

interface IPhenotypeCallbacks {
    oneway void onRegistered(in Status status) = 0;
    oneway void onWeakRegistered(in Status status) = 1;
    oneway void onUnregistered(in Status status) = 2;
    oneway void onConfiguration(in Status status, in Configurations configurations) = 3;
    oneway void onCommitedToConfiguration(in Status status) = 4;
    oneway void onExperimentTokens(in Status status, in ExperimentTokens experimentTokens) = 5;
    oneway void onDogfoodsToken(in Status status, in DogfoodsToken dogfoodsToken) = 6;
    oneway void onDogfoodsTokenSet(in Status status) = 7;
    oneway void onFlag(in Status status, in Flag flag) = 8;
    oneway void onCommittedConfiguration(in Status status, in Configurations configuration) = 9;
    oneway void onSyncFinished(in Status status, long p1) = 10;
    oneway void onFlagOverridesSet(in Status status) = 11;
    oneway void onFlagOverrides(in Status status, in FlagOverrides overrides) = 12;
    oneway void onAppSpecificPropertiesSet(in Status status) = 13;

    oneway void onServingVersion(in Status status, long version) = 15;
}
