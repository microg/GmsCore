/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.microg.gms

import android.accounts.Account
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.common.internal.IGmsServiceBroker
import com.google.android.gms.common.internal.ValidateAccountRequest
import org.microg.gms.common.GmsService
import org.microg.gms.common.GmsService.Companion.byServiceId
import java.util.*

abstract class AbstractGmsServiceBroker(
        private val supportedServices: EnumSet<GmsService>
) : IGmsServiceBroker.Stub() {
    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getPlusService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                authPackage: String?, scopes: Array<String>?, accountName: String?, params: Bundle?) {
        val extras = params ?: Bundle()
        extras.putString("auth_package", authPackage)
        callGetService(GmsService.PLUS, callback, versionCode, packageName, extras, accountName, scopes)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getPanoramaService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                    params: Bundle?) {
        callGetService(GmsService.PANORAMA, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getAppDataSearchService(callback: IGmsCallbacks, versionCode: Int, packageName: String?) {
        callGetService(GmsService.INDEX, callback, versionCode, packageName)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getWalletService(callback: IGmsCallbacks, versionCode: Int) {
        getWalletServiceWithPackageName(callback, versionCode, null)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getPeopleService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                  params: Bundle?) {
        callGetService(GmsService.PEOPLE, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getReportingService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                     params: Bundle?) {
        callGetService(GmsService.LOCATION_REPORTING, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getLocationService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                    params: Bundle?) {
        callGetService(GmsService.LOCATION, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getGoogleLocationManagerService(callback: IGmsCallbacks, versionCode: Int,
                                                 packageName: String?, params: Bundle?) {
        callGetService(GmsService.LOCATION_MANAGER, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getGamesService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                 accountName: String?, scopes: Array<String>?, gamePackageName: String?,
                                 popupWindowToken: IBinder?, desiredLocale: String?, params: Bundle?) {
        val extras = params ?: Bundle()
        extras.putString("com.google.android.gms.games.key.gamePackageName", gamePackageName)
        extras.putString("com.google.android.gms.games.key.desiredLocale", desiredLocale)
        //extras.putParcelable("com.google.android.gms.games.key.popupWindowToken", popupWindowToken);
        callGetService(GmsService.GAMES, callback, versionCode, packageName, extras, accountName, scopes)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getAppStateService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                    accountName: String?, scopes: Array<String>?) {
        callGetService(GmsService.APPSTATE, callback, versionCode, packageName, null, accountName, scopes)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getPlayLogService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                   params: Bundle?) {
        callGetService(GmsService.PLAY_LOG, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getAdMobService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                 params: Bundle?) {
        callGetService(GmsService.ADREQUEST, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getDroidGuardService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                      params: Bundle?) {
        callGetService(GmsService.DROIDGUARD, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getLockboxService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                   params: Bundle?) {
        callGetService(GmsService.LOCKBOX, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getCastMirroringService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                         params: Bundle?) {
        callGetService(GmsService.CAST_MIRRORING, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getNetworkQualityService(callback: IGmsCallbacks, versionCode: Int,
                                          packageName: String?, params: Bundle?) {
        callGetService(GmsService.NETWORK_QUALITY, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getGoogleIdentityService(callback: IGmsCallbacks, versionCode: Int,
                                          packageName: String?, params: Bundle?) {
        callGetService(GmsService.ACCOUNT, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getGoogleFeedbackService(callback: IGmsCallbacks, versionCode: Int,
                                          packageName: String?, params: Bundle?) {
        callGetService(GmsService.FEEDBACK, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getCastService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                binder: IBinder?, params: Bundle?) {
        callGetService(GmsService.CAST, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getDriveService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                 scopes: Array<String>?, accountName: String?, params: Bundle?) {
        callGetService(GmsService.DRIVE, callback, versionCode, packageName, params, accountName, scopes)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getLightweightAppDataSearchService(callback: IGmsCallbacks, versionCode: Int,
                                                    packageName: String?) {
        callGetService(GmsService.LIGHTWEIGHT_INDEX, callback, versionCode, packageName)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getSearchAdministrationService(callback: IGmsCallbacks, versionCode: Int,
                                                packageName: String?) {
        callGetService(GmsService.SEARCH_ADMINISTRATION, callback, versionCode, packageName)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getAutoBackupService(callback: IGmsCallbacks, versionCode: Int, packageName: String?,
                                      params: Bundle?) {
        callGetService(GmsService.PHOTO_AUTO_BACKUP, callback, versionCode, packageName, params)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getAddressService(callback: IGmsCallbacks, versionCode: Int, packageName: String?) {
        callGetService(GmsService.ADDRESS, callback, versionCode, packageName)
    }

    @Deprecated("")
    @Throws(RemoteException::class)
    override fun getWalletServiceWithPackageName(callback: IGmsCallbacks, versionCode: Int, packageName: String?) {
        callGetService(GmsService.WALLET, callback, versionCode, packageName)
    }

    @Throws(RemoteException::class)
    private fun callGetService(service: GmsService, callback: IGmsCallbacks, gmsVersion: Int,
                               packageName: String?) {
        callGetService(service, callback, gmsVersion, packageName, null)
    }

    @Throws(RemoteException::class)
    private fun callGetService(service: GmsService, callback: IGmsCallbacks, gmsVersion: Int,
                               packageName: String?, extras: Bundle?) {
        callGetService(service, callback, gmsVersion, packageName, extras, null, null)
    }

    @Throws(RemoteException::class)
    private fun callGetService(service: GmsService, callback: IGmsCallbacks, gmsVersion: Int, packageName: String?, extras: Bundle?, accountName: String?, scopes: Array<String>?) {
        val request = GetServiceRequest(service.SERVICE_ID)
        request.gmsVersion = gmsVersion
        request.packageName = packageName
        request.extras = extras
        request.account = if (accountName == null) null else Account(accountName, "com.google")
        request.scopes = scopes?.let { scopesFromStringArray(it) }
        getService(callback, request)
    }

    private fun scopesFromStringArray(arr: Array<String>): Array<Scope?> {
        val scopes = arrayOfNulls<Scope>(arr.size)
        for (i in arr.indices) {
            scopes[i] = Scope(arr[i])
        }
        return scopes
    }

    @Throws(RemoteException::class)
    override fun getService(callback: IGmsCallbacks?, request: GetServiceRequest) {
        val gmsService = byServiceId(request.serviceId)
        if (supportedServices.contains(gmsService) || supportedServices.contains(GmsService.ANY)) {
            handleServiceRequest(callback, request, gmsService)
        } else {
            Log.d(TAG, "Service not supported: $request")
            throw IllegalArgumentException("Service not supported: " + request.serviceId)
        }
    }

    @Throws(RemoteException::class)
    abstract fun handleServiceRequest(callback: IGmsCallbacks?, request: GetServiceRequest?, service: GmsService?)

    @Throws(RemoteException::class)
    override fun validateAccount(callback: IGmsCallbacks?, request: ValidateAccountRequest?) {
        throw IllegalArgumentException("ValidateAccountRequest not supported")
    }

    @Throws(RemoteException::class)
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (super.onTransact(code, data, reply, flags)) return true
        Log.d(TAG, "onTransact [unknown]: $code, $data, $flags")
        return false
    }

    companion object {
        private const val TAG = "GmsServiceBroker"
    }
}