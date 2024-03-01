/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.phenotype.*
import com.google.android.gms.phenotype.internal.IPhenotypeCallbacks
import com.google.android.gms.phenotype.internal.IPhenotypeService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "PhenotypeService"

class PhenotypeService : BaseService(TAG, GmsService.PHENOTYPE) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest?, service: GmsService?) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request?.packageName)
        callback.onPostInitComplete(0, PhenotypeServiceImpl(packageName).asBinder(), null)
    }
}

class PhenotypeServiceImpl(val packageName: String?) : IPhenotypeService.Stub() {
    override fun register(callbacks: IPhenotypeCallbacks, packageName: String?, version: Int, p3: Array<out String>?, p4: ByteArray?) {
        Log.d(TAG, "register($packageName, $version, $p3, $p4)")
        callbacks.onRegistered(if (version != 0) Status.SUCCESS else Status.CANCELED)
    }

    override fun weakRegister(callbacks: IPhenotypeCallbacks, packageName: String?, version: Int, p3: Array<out String>?, p4: IntArray?, p5: ByteArray?) {
        Log.d(TAG, "weakRegister($packageName, $version, $p3, $p4, $p5)")
        callbacks.onWeakRegistered(Status.SUCCESS)
    }

    override fun unregister(callbacks: IPhenotypeCallbacks, packageName: String?) {
        Log.d(TAG, "unregister($packageName)")
        callbacks.onUnregistered(Status.SUCCESS)
    }

    override fun getConfigurationSnapshot(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?) {
        getConfigurationSnapshot2(callbacks, packageName, user, null)
    }

    override fun commitToConfiguration(callbacks: IPhenotypeCallbacks, snapshotToken: String?) {
        Log.d(TAG, "commitToConfiguration($snapshotToken)")
        callbacks.onCommitedToConfiguration(Status.SUCCESS)
    }

    override fun getExperimentTokens(callbacks: IPhenotypeCallbacks, packageName: String?, logSourceName: String?) {
        getExperimentTokensForLogging(callbacks, packageName, logSourceName, null, this.packageName)
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

    override fun getCommitedConfiguration(callbacks: IPhenotypeCallbacks, packageName: String?) {
        Log.d(TAG, "getCommitedConfiguration($packageName)")
        callbacks.onCommittedConfiguration(Status.SUCCESS, Configurations().apply {
            field4 = emptyArray()
        })
    }

    override fun getConfigurationSnapshot2(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, p3: String?) {
        Log.d(TAG, "getConfigurationSnapshot2($packageName, $user, $p3)")
        callbacks.onConfiguration(Status.SUCCESS, Configurations().apply {
            field4 = emptyArray()
        })
    }

    override fun syncAfterOperation(callbacks: IPhenotypeCallbacks, packageName: String?, version: Long) {
        Log.d(TAG, "syncAfterOperation($packageName, $version)")
        callbacks.onSyncFinished(Status.SUCCESS, version)
    }

    override fun registerSync(callbacks: IPhenotypeCallbacks, packageName: String?, version: Int, p3: Array<out String>?, p4: ByteArray?, p5: String?, p6: String?) {
        Log.d(TAG, "registerSync($packageName, $version, $p3, $p4, $p5, $p6)")
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

    override fun setAppSpecificProperties(callbacks: IPhenotypeCallbacks, packageName: String?, p2: ByteArray?) {
        Log.d(TAG, "setAppSpecificProperties($packageName, $p2)")
        callbacks.onAppSpecificPropertiesSet(Status.SUCCESS)
    }

    override fun getServingVersion(callbacks: IPhenotypeCallbacks) {
        Log.d(TAG, "getServingVersion()")
        callbacks.onServingVersion(Status.SUCCESS, 1)
    }

    override fun getExperimentTokensForLogging(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, p3: String?, clientPackageName: String?) {
        Log.d(TAG, "getExperimentTokens($packageName, $user, $p3, $clientPackageName)")
        callbacks.onExperimentTokens(Status.SUCCESS, ExperimentTokens().apply {
            field2 = ""
        })
    }

    override fun syncAllAfterOperation(callbacks: IPhenotypeCallbacks?, p1: Long) {
        Log.d(TAG, "Not yet implemented: syncAllAfterOperation")
    }

    override fun setRuntimeProperties(callbacks: IStatusCallback?, p1: String?, p2: ByteArray?) {
        Log.d(TAG, "Not yet implemented: setRuntimeProperties")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
