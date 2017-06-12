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

package org.microg.gms.ads;

import android.os.RemoteException;

import com.google.android.gms.ads.identifier.internal.IAdvertisingIdService;

import java.util.UUID;

public class AdvertisingIdServiceImpl extends IAdvertisingIdService.Stub {
    @Override
    public String getAdvertisingId() throws RemoteException {
        return generateAdvertisingId(null);
    }

    @Override
    public boolean isAdTrackingLimited(boolean defaultHint) throws RemoteException {
        return true;
    }

    @Override
    public String generateAdvertisingId(String packageName) throws RemoteException {
        return UUID.randomUUID().toString();
    }

    @Override
    public void setAdTrackingLimited(String packageName, boolean limited) throws RemoteException {
        // Ignored, sorry :)
    }
}
