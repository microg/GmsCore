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

package org.microg.gms.location;

import android.location.Location;
import android.os.Bundle;

import static com.google.android.gms.location.FusedLocationProviderClient.KEY_MOCK_LOCATION;

public class MockLocationProvider {
    private boolean mockEnabled = false;
    private Location mockLocation = null;
    private final LocationChangeListener changeListener;

    public MockLocationProvider(LocationChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }
    
    public Location getLocation() {
        return mockEnabled ? mockLocation : null;
    }

    public void setLocation(Location mockLocation) {
        if (mockLocation.getExtras() == null) {
            mockLocation.setExtras(new Bundle());
        }
        mockLocation.getExtras().putBoolean(KEY_MOCK_LOCATION, false);
        this.mockLocation = mockLocation;
        this.changeListener.onLocationChanged();
    }
}
