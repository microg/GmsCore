/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.phenotype.*
import com.google.android.gms.phenotype.internal.IPhenotypeCallbacks
import com.google.android.gms.phenotype.internal.IPhenotypeService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "PhenotypeService"

class PhenotypeService : BaseService(TAG, GmsService.PHENOTYPE) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest?, service: GmsService?) {
        callback.onPostInitComplete(0, PhenotypeServiceImpl().asBinder(), null)
    }
}

class PhenotypeServiceImpl : IPhenotypeService.Stub() {
    override fun register(callbacks: IPhenotypeCallbacks, p1: String?, p2: Int, p3: Array<out String>?, p4: ByteArray?) {
        Log.d(TAG, "register($p1, $p2, $p3, $p4)")
        callbacks.onRegistered(Status.SUCCESS)
    }

    override fun weakRegister(callbacks: IPhenotypeCallbacks, p1: String?, p2: Int, p3: Array<out String>?, p4: IntArray?, p5: ByteArray?) {
        Log.d(TAG, "weakRegister($p1, $p2, $p3, $p4, $p5)")
        callbacks.onWeakRegistered(Status.SUCCESS)
    }

    override fun unregister(callbacks: IPhenotypeCallbacks, p1: String?) {
        Log.d(TAG, "unregister($p1)")
        callbacks.onUnregistered(Status.SUCCESS)
    }

    override fun getConfigurationSnapshot(callbacks: IPhenotypeCallbacks, p1: String?, p2: String?) {
        Log.d(TAG, "getConfigurationSnapshot($p1, $p2)")
        callbacks.onConfiguration(Status.SUCCESS, Configurations().apply {
            field4 = emptyArray()
        })
    }

    override fun commitToConfiguration(callbacks: IPhenotypeCallbacks, p1: String?) {
        Log.d(TAG, "commitToConfiguration($p1)")
        callbacks.onCommitedToConfiguration(Status.SUCCESS)
    }

    override fun getExperimentTokens(callbacks: IPhenotypeCallbacks, p1: String?, logSourceName: String?) {
        Log.d(TAG, "getExperimentTokens($p1, $logSourceName)")
        callbacks.onExperimentTokens(Status.SUCCESS, ExperimentTokens())
    }

    override fun getDogfoodsToken(callbacks: IPhenotypeCallbacks) {
        Log.d(TAG, "getDogfoodsToken()")
        callbacks.onDogfoodsToken(Status.SUCCESS, DogfoodsToken())
    }

    override fun setDogfoodsToken(callbacks: IPhenotypeCallbacks, p1: ByteArray?) {
        Log.d(TAG, "setDogfoodsToken($p1)")
        callbacks.onDogfoodsTokenSet(Status.SUCCESS)
    }

    override fun getFlag(callbacks: IPhenotypeCallbacks, packageName: String?, name: String?, type: Int) {
        Log.d(TAG, "setDogfoodsToken($packageName, $name, $type)")
        callbacks.onFlag(Status.SUCCESS, null)
    }

    override fun getCommitedConfiguration(callbacks: IPhenotypeCallbacks, p1: String?) {
        Log.d(TAG, "getCommitedConfiguration($p1)")
        callbacks.onCommittedConfiguration(Status.SUCCESS, Configurations().apply {
            field4 = emptyArray()
        })
    }

    override fun getConfigurationSnapshot2(callbacks: IPhenotypeCallbacks, p1: String?, p2: String?, p3: String?) {
        Log.d(TAG, "getConfigurationSnapshot2($p1, $p2, $p3)")
        callbacks.onConfiguration(Status.SUCCESS, Configurations().apply {
            field4 = emptyArray()
        })
    }

    override fun syncAfterOperation(callbacks: IPhenotypeCallbacks, p1: String?, p2: Long) {
        Log.d(TAG, "syncAfterOperation($p1, $p2)")
        callbacks.onSyncFinished(Status.SUCCESS, p2)
    }

    override fun registerSync(callbacks: IPhenotypeCallbacks, p1: String?, p2: Int, p3: Array<out String>?, p4: ByteArray?, p5: String?, p6: String?) {
        Log.d(TAG, "registerSync($p1, $p2, $p3, $p4, $p5, $p6)")
        callbacks.onConfiguration(Status.SUCCESS, Configurations().apply {
            field4 = emptyArray()
        })
    }

    override fun setFlagOverrides(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, flagName: String?, flagType: Int, flagDataType: Int, flagValue: String?) {
        Log.d(TAG, "setFlagOverrides($packageName, $user, $flagName, $flagDataType, $flagType, $flagDataType, $flagValue)")
        callbacks.onFlagOverridesSet(Status.SUCCESS)
    }

    override fun deleteFlagOverrides(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, flagName: String?) {
        Log.d(TAG, "deleteFlagOverrides($packageName, $user, $flagName)")
        callbacks.onFlagOverrides(Status.SUCCESS, FlagOverrides())
    }

    override fun listFlagOverrides(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, flagName: String?) {
        Log.d(TAG, "listFlagOverrides($packageName, $user, $flagName)")
        callbacks.onFlagOverrides(Status.SUCCESS, FlagOverrides())
    }

    override fun clearFlagOverrides(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?) {
        Log.d(TAG, "clearFlagOverrides($packageName, $user)")
        callbacks.onFlagOverridesSet(Status.SUCCESS)
    }

    override fun bulkRegister(callbacks: IPhenotypeCallbacks, infos: Array<out RegistrationInfo>?) {
        Log.d(TAG, "bulkRegister($infos)")
        callbacks.onRegistered(Status.SUCCESS)
    }

    override fun setAppSpecificProperties(callbacks: IPhenotypeCallbacks, p1: String?, p2: ByteArray?) {
        Log.d(TAG, "setAppSpecificProperties($p1, $p2)")
        callbacks.onAppSpecificPropertiesSet(Status.SUCCESS)
    }

    override fun getServingVersion(callbacks: IPhenotypeCallbacks) {
        Log.d(TAG, "getServingVersion()")
        callbacks.onServingVersion(Status.SUCCESS, 1)
    }

    override fun getExperimentTokens2(callbacks: IPhenotypeCallbacks, p1: String?, p2: String?, p3: String?, p4: String?) {
        Log.d(TAG, "getExperimentTokens2($p1, $p2, $p3, $p4)")
        callbacks.onExperimentTokens(Status.SUCCESS, ExperimentTokens())
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags) { super.onTransact(code, data, reply, flags) }
}
