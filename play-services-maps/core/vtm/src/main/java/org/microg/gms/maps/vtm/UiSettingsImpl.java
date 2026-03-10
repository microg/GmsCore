/*
 * Copyright (C) 2019 microG Project Team
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

package org.microg.gms.maps.vtm;

import android.os.RemoteException;
import com.google.android.gms.maps.internal.IUiSettingsDelegate;

public class UiSettingsImpl extends IUiSettingsDelegate.Stub {
    private boolean zoomControlsEnabled;
    private boolean compassEnabled;
    private boolean myLocationButtonEnabled;
    private boolean scrollGesturesEnabled = true;
    private boolean zoomGesturesEnabled = true;
    private boolean tiltGesturesEnabled = true;
    private boolean rotateGesturesEnabled = true;
    private boolean allGesturesEnabled = true;
    private boolean indoorLevelPickerEnabled = false;
    private boolean mapToolbarEnabled = false;
    private boolean scrollGesturesEnabledDuringRotateOrZoom = true;
    
    private UiSettingsListener listener;

    public UiSettingsImpl() {
    }

    public UiSettingsImpl(UiSettingsListener listener) {
        this.listener = listener;
    }

    public void setListener(UiSettingsListener listener) {
        this.listener = listener;
    }

    @Override
    public void setZoomControlsEnabled(boolean zoomControlsEnabled) throws RemoteException {
        this.zoomControlsEnabled = zoomControlsEnabled;
        listener.onUiSettingsChanged(this);
    }

    @Override
    public void setCompassEnabled(boolean compassEnabled) throws RemoteException {
        this.compassEnabled = compassEnabled;
        listener.onUiSettingsChanged(this);
    }

    @Override
    public void setMyLocationButtonEnabled(boolean myLocationButtonEnabled) throws RemoteException {
        this.myLocationButtonEnabled = myLocationButtonEnabled;
        listener.onUiSettingsChanged(this);
    }

    @Override
    public void setScrollGesturesEnabled(boolean scrollGesturesEnabled) throws RemoteException {
        this.scrollGesturesEnabled = scrollGesturesEnabled;
        listener.onUiSettingsChanged(this);
    }

    @Override
    public void setZoomGesturesEnabled(boolean zoomGestures) throws RemoteException {
        this.zoomGesturesEnabled = zoomGestures;
        listener.onUiSettingsChanged(this);
    }

    @Override
    public void setTiltGesturesEnabled(boolean tiltGesturesEnabled) throws RemoteException {
        this.tiltGesturesEnabled = tiltGesturesEnabled;
        listener.onUiSettingsChanged(this);
    }

    @Override
    public void setRotateGesturesEnabled(boolean rotateGesturesEnabled) throws RemoteException {
        this.rotateGesturesEnabled = rotateGesturesEnabled;
        listener.onUiSettingsChanged(this);
    }

    @Override
    public void setAllGesturesEnabled(boolean gestures) throws RemoteException {
        scrollGesturesEnabled = gestures;
        zoomGesturesEnabled = gestures;
        tiltGesturesEnabled = gestures;
        rotateGesturesEnabled = gestures;
        listener.onUiSettingsChanged(this);
    }

    @Override
    public boolean isZoomControlsEnabled() throws RemoteException {
        return zoomControlsEnabled;
    }

    @Override
    public boolean isCompassEnabled() throws RemoteException {
        return compassEnabled;
    }

    @Override
    public boolean isMyLocationButtonEnabled() throws RemoteException {
        return myLocationButtonEnabled;
    }

    @Override
    public boolean isScrollGesturesEnabled() throws RemoteException {
        return scrollGesturesEnabled;
    }

    @Override
    public boolean isZoomGesturesEnabled() throws RemoteException {
        return zoomGesturesEnabled;
    }

    @Override
    public boolean isTiltGesturesEnabled() throws RemoteException {
        return tiltGesturesEnabled;
    }

    @Override
    public boolean isRotateGesturesEnabled() throws RemoteException {
        return rotateGesturesEnabled;
    }

    @Override
    public void setIndoorLevelPickerEnabled(boolean indoorLevelPicker) throws RemoteException {
        this.indoorLevelPickerEnabled = indoorLevelPicker;
        listener.onUiSettingsChanged(this);
    }

    @Override
    public boolean isIndoorLevelPickerEnabled() throws RemoteException {
        return indoorLevelPickerEnabled;
    }

    @Override
    public void setMapToolbarEnabled(boolean mapToolbar) throws RemoteException {
        this.mapToolbarEnabled = mapToolbar;
        listener.onUiSettingsChanged(this);
    }

    @Override
    public boolean isMapToolbarEnabled() throws RemoteException {
        return mapToolbarEnabled;
    }

    @Override
    public void setScrollGesturesEnabledDuringRotateOrZoom(boolean scrollDuringZoom) throws RemoteException {
        this.scrollGesturesEnabledDuringRotateOrZoom = scrollDuringZoom;
        listener.onUiSettingsChanged(this);
    }

    @Override
    public boolean isScrollGesturesEnabledDuringRotateOrZoom() throws RemoteException {
        return scrollGesturesEnabledDuringRotateOrZoom;
    }

    public static interface UiSettingsListener {
        void onUiSettingsChanged(UiSettingsImpl settings) throws RemoteException;
    }
}
