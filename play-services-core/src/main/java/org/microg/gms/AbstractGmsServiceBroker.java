/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.gms;

import android.accounts.Account;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.common.internal.IGmsServiceBroker;
import com.google.android.gms.common.internal.ValidateAccountRequest;

import org.microg.gms.common.Services;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractGmsServiceBroker extends IGmsServiceBroker.Stub {
    public static final int ID_ACCEPT_ALL = -1;

    private static final String TAG = "GmsServiceBroker";
    private final Set<Integer> supportedServiceIds;

    public AbstractGmsServiceBroker(Integer supportedServiceId, Integer... supportedServiceIds) {
        this(combine(supportedServiceId, supportedServiceIds));
    }

    private static Set<Integer> combine(Integer i, Integer... is) {
        Set<Integer> integers = new HashSet<Integer>(Arrays.asList(is));
        integers.add(i);
        return integers;
    }

    public AbstractGmsServiceBroker(Set<Integer> supportedServiceIds) {
        this.supportedServiceIds = supportedServiceIds;
    }

    @Deprecated
    @Override
    public void getPlusService(IGmsCallbacks callback, int versionCode, String packageName,
                               String authPackage, String[] scopes, String accountName, Bundle params)
            throws RemoteException {
        Bundle extras = params == null ? new Bundle() : params;
        extras.putString("auth_package", authPackage);
        callGetService(Services.PLUS.SERVICE_ID, callback, versionCode, packageName, extras, accountName, scopes);
    }

    @Deprecated
    @Override
    public void getPanoramaService(IGmsCallbacks callback, int versionCode, String packageName,
                                   Bundle params) throws RemoteException {
        callGetService(Services.PANORAMA.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getAppDataSearchService(IGmsCallbacks callback, int versionCode, String packageName)
            throws RemoteException {
        callGetService(Services.INDEX.SERVICE_ID, callback, versionCode, packageName);
    }

    @Deprecated
    @Override
    public void getWalletService(IGmsCallbacks callback, int versionCode) throws RemoteException {
        getWalletServiceWithPackageName(callback, versionCode, null);
    }

    @Deprecated
    @Override
    public void getPeopleService(IGmsCallbacks callback, int versionCode, String packageName,
                                 Bundle params) throws RemoteException {
        callGetService(Services.PEOPLE.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getReportingService(IGmsCallbacks callback, int versionCode, String packageName,
                                    Bundle params) throws RemoteException {
        callGetService(Services.LOCATION_REPORTING.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getLocationService(IGmsCallbacks callback, int versionCode, String packageName,
                                   Bundle params) throws RemoteException {
        callGetService(Services.LOCATION.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getGoogleLocationManagerService(IGmsCallbacks callback, int versionCode,
                                                String packageName, Bundle params) throws RemoteException {
        callGetService(Services.LOCATION_MANAGER.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getGamesService(IGmsCallbacks callback, int versionCode, String packageName,
                                String accountName, String[] scopes, String gamePackageName,
                                IBinder popupWindowToken, String desiredLocale, Bundle params)
            throws RemoteException {
        Bundle extras = params == null ? new Bundle() : params;
        extras.putString("com.google.android.gms.games.key.gamePackageName", gamePackageName);
        extras.putString("com.google.android.gms.games.key.desiredLocale", desiredLocale);
        //extras.putParcelable("com.google.android.gms.games.key.popupWindowToken", popupWindowToken);
        callGetService(Services.GAMES.SERVICE_ID, callback, versionCode, packageName, extras, accountName, scopes);
    }

    @Deprecated
    @Override
    public void getAppStateService(IGmsCallbacks callback, int versionCode, String packageName,
                                   String accountName, String[] scopes) throws RemoteException {
        callGetService(Services.APPSTATE.SERVICE_ID, callback, versionCode, packageName, null, accountName, scopes);
    }

    @Deprecated
    @Override
    public void getPlayLogService(IGmsCallbacks callback, int versionCode, String packageName,
                                  Bundle params) throws RemoteException {
        callGetService(Services.PLAY_LOG.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getAdMobService(IGmsCallbacks callback, int versionCode, String packageName,
                                Bundle params) throws RemoteException {
        callGetService(Services.ADREQUEST.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getDroidGuardService(IGmsCallbacks callback, int versionCode, String packageName,
                                     Bundle params) throws RemoteException {
        callGetService(Services.DROIDGUARD.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getLockboxService(IGmsCallbacks callback, int versionCode, String packageName,
                                  Bundle params) throws RemoteException {
        callGetService(Services.LOCKBOX.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getCastMirroringService(IGmsCallbacks callback, int versionCode, String packageName,
                                        Bundle params) throws RemoteException {
        callGetService(Services.CAST_MIRRORING.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getNetworkQualityService(IGmsCallbacks callback, int versionCode,
                                         String packageName, Bundle params) throws RemoteException {
        callGetService(Services.NETWORK_QUALITY.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getGoogleIdentityService(IGmsCallbacks callback, int versionCode,
                                         String packageName, Bundle params) throws RemoteException {
        callGetService(Services.ACCOUNT.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getGoogleFeedbackService(IGmsCallbacks callback, int versionCode,
                                         String packageName, Bundle params) throws RemoteException {
        callGetService(Services.FEEDBACK.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getCastService(IGmsCallbacks callback, int versionCode, String packageName,
                               IBinder binder, Bundle params) throws RemoteException {
        throw new IllegalArgumentException("Cast service not supported");
    }

    @Deprecated
    @Override
    public void getDriveService(IGmsCallbacks callback, int versionCode, String packageName,
                                String[] scopes, String accountName, Bundle params) throws RemoteException {
        callGetService(Services.DRIVE.SERVICE_ID, callback, versionCode, packageName, params, accountName, scopes);
    }

    @Deprecated
    @Override
    public void getLightweightAppDataSearchService(IGmsCallbacks callback, int versionCode,
                                                   String packageName) throws RemoteException {
        callGetService(Services.LIGHTWEIGHT_INDEX.SERVICE_ID, callback, versionCode, packageName);
    }

    @Deprecated
    @Override
    public void getSearchAdministrationService(IGmsCallbacks callback, int versionCode,
                                               String packageName) throws RemoteException {
        callGetService(Services.SEARCH_ADMINISTRATION.SERVICE_ID, callback, versionCode, packageName);
    }

    @Deprecated
    @Override
    public void getAutoBackupService(IGmsCallbacks callback, int versionCode, String packageName,
                                     Bundle params) throws RemoteException {
        callGetService(Services.PHOTO_AUTO_BACKUP.SERVICE_ID, callback, versionCode, packageName, params);
    }

    @Deprecated
    @Override
    public void getAddressService(IGmsCallbacks callback, int versionCode, String packageName)
            throws RemoteException {
        callGetService(Services.ADDRESS.SERVICE_ID, callback, versionCode, packageName);
    }

    @Deprecated
    @Override
    public void getWalletServiceWithPackageName(IGmsCallbacks callback, int versionCode, String packageName) throws RemoteException {
        callGetService(Services.WALLET.SERVICE_ID, callback, versionCode, packageName);
    }

    private void callGetService(int serviceId, IGmsCallbacks callback, int gmsVersion,
                                String packageName) throws RemoteException {
        callGetService(serviceId, callback, gmsVersion, packageName, null);
    }

    private void callGetService(int serviceId, IGmsCallbacks callback, int gmsVersion,
                                String packageName, Bundle extras) throws RemoteException {
        callGetService(serviceId, callback, gmsVersion, packageName, extras, null, null);
    }

    private void callGetService(int serviceId, IGmsCallbacks callback, int gmsVersion, String packageName, Bundle extras, String accountName, String[] scopes) throws RemoteException {
        GetServiceRequest request = new GetServiceRequest(serviceId);
        request.gmsVersion = gmsVersion;
        request.packageName = packageName;
        request.extras = extras;
        request.account = accountName == null ? null : new Account(accountName, "com.google");
        request.scopes = scopes == null ? null : scopesFromStringArray(scopes);
        getService(callback, request);
    }

    private Scope[] scopesFromStringArray(String[] arr) {
        Scope[] scopes = new Scope[arr.length];
        for (int i = 0; i < arr.length; i++) {
            scopes[i] = new Scope(arr[i]);
        }
        return scopes;
    }

    @Override
    public void getService(IGmsCallbacks callback, GetServiceRequest request) throws RemoteException {
        if (supportedServiceIds.contains(request.serviceId) || supportedServiceIds.contains(ID_ACCEPT_ALL)) {
            handleServiceRequest(callback, request);
        } else {
            Log.d(TAG, "Service not supported: " + request);
            throw new IllegalArgumentException("Service not supported: " + request.serviceId);
        }
    }

    public abstract void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request) throws RemoteException;

    @Override
    public void validateAccount(IGmsCallbacks callback, ValidateAccountRequest request) throws RemoteException {
        throw new IllegalArgumentException("ValidateAccountRequest not supported");
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
