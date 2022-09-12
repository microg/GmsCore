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

package org.microg.gms.games;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static org.microg.gms.common.Constants.GMS_PACKAGE_NAME;
import static org.microg.gms.games.UpgradeActivity.ACTION_PLAY_GAMES_UPGRADE;
import static org.microg.gms.games.UpgradeActivity.EXTRA_GAME_PACACKE_NAME;

public class GamesStubService extends BaseService {

    public static final String PARAM_GAME_PACKAGE_NAME = "com.google.android.gms.games.key.gamePackageName";

    public GamesStubService() {
        super("GmsGamesSvc", GmsService.GAMES);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        String packageName = null;
        if (request.extras != null) {
            packageName = request.extras.getString(PARAM_GAME_PACKAGE_NAME);
        }
        if (packageName == null) packageName = GMS_PACKAGE_NAME;
        Intent intent = new Intent(ACTION_PLAY_GAMES_UPGRADE);
        intent.setPackage(GMS_PACKAGE_NAME);
        intent.putExtra(EXTRA_GAME_PACACKE_NAME, packageName);
        Bundle bundle = new Bundle();
        bundle.putParcelable("pendingIntent", PendingIntent.getActivity(this, packageName.hashCode(), intent, FLAG_UPDATE_CURRENT));
        callback.onPostInitComplete(CommonStatusCodes.RESOLUTION_REQUIRED, null, bundle);
    }
}
