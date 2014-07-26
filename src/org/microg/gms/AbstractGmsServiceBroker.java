/*
 * Copyright (c) 2014 μg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.common.internal.IGmsServiceBroker;

public abstract class AbstractGmsServiceBroker extends IGmsServiceBroker.Stub {
	@Override
	public void getPlusService(IGmsCallbacks callback, int versionCode, String packageName, String str2, String[] paramArrayOfString, String str3, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Plus service not supported");
	}

	@Override
	public void getPanoramaService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Panorama service not supported");
	}

	@Override
	public void getAppDataSearchService(IGmsCallbacks callback, int versionCode, String packageName) throws RemoteException {
		throw new IllegalArgumentException("App Data Search service not supported");
	}

	@Override
	public void getWalletService(IGmsCallbacks callback, int versionCode) throws RemoteException {
		throw new IllegalArgumentException("Wallet service not supported");
	}

	@Override
	public void getPeopleService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("People service not supported");
	}

	@Override
	public void getReportingService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Reporting service not supported");
	}

	@Override
	public void getLocationService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Location service not supported");
	}

	@Override
	public void getGoogleLocationManagerService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Google Location Manager service not supported");
	}

	@Override
	public void getGamesService(IGmsCallbacks callback, int versionCode, String packageName, String str2, String[] args, String str3, IBinder binder, String str4, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Games service not supported");
	}

	@Override
	public void getAppStateService(IGmsCallbacks callback, int versionCode, String packageName, String str2, String[] args) throws RemoteException {
		throw new IllegalArgumentException("App State service not supported");
	}

	@Override
	public void getPlayLogService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Play Log service not supported");
	}

	@Override
	public void getAdMobService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("AdMob service not supported");
	}

	@Override
	public void getDroidGuardService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("DroidGuard service not supported");
	}

	@Override
	public void getLockboxService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Lockbox service not supported");
	}

	@Override
	public void getCastMirroringService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Cast Mirroring service not supported");
	}

	@Override
	public void getNetworkQualityService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Network Quality service not supported");
	}

	@Override
	public void getGoogleIdentityService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Google Identity service not supported");
	}

	@Override
	public void getGoogleFeedbackService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Google Feedback service not supported");
	}

	@Override
	public void getCastService(IGmsCallbacks callback, int versionCode, String packageName, IBinder binder, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Cast service not supported");
	}

	@Override
	public void getDriveService(IGmsCallbacks callback, int versionCode, String packageName, String[] args, String str2, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Drive service not supported");
	}

	@Override
	public void getLightweightAppDataSearchService(IGmsCallbacks callback, int versionCode, String packageName) throws RemoteException {
		throw new IllegalArgumentException("Lightweight App Data Search service not supported");
	}

	@Override
	public void getSearchAdministrationService(IGmsCallbacks callback, int versionCode, String packageName) throws RemoteException {
		throw new IllegalArgumentException("Search Administration service not supported");
	}

	@Override
	public void getAutoBackupService(IGmsCallbacks callback, int versionCode, String packageName, Bundle params) throws RemoteException {
		throw new IllegalArgumentException("Auto Backup service not supported");
	}

	@Override
	public void getAddressService(IGmsCallbacks callback, int versionCode, String packageName) throws RemoteException {
		throw new IllegalArgumentException("Address service not supported");
	}
}
