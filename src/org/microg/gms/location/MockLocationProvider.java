package org.microg.gms.location;

import android.location.Location;
import android.os.Bundle;

import static org.microg.gms.maps.Constants.KEY_MOCK_LOCATION;

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
    }
}
