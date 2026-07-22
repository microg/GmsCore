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

package org.microg.gms.maps.vtm.markup;

import android.os.RemoteException;
import com.google.android.gms.maps.model.internal.ITileOverlayDelegate;

public class TileOverlayImpl extends ITileOverlayDelegate.Stub {
    @Override
    public void remove() throws RemoteException {

    }

    @Override
    public void clearTileCache() throws RemoteException {

    }

    @Override
    public String getId() throws RemoteException {
        return null;
    }

    @Override
    public void setZIndex(float zIndex) throws RemoteException {

    }

    @Override
    public float getZIndex() throws RemoteException {
        return 0;
    }

    @Override
    public void setVisible(boolean visible) throws RemoteException {

    }

    @Override
    public boolean isVisible() throws RemoteException {
        return false;
    }

    @Override
    public boolean equalsRemote(ITileOverlayDelegate other) throws RemoteException {
        return false;
    }

    @Override
    public int hashCodeRemote() throws RemoteException {
        return 0;
    }

    @Override
    public void setFadeIn(boolean fadeIn) throws RemoteException {

    }

    @Override
    public boolean getFadeIn() throws RemoteException {
        return false;
    }

    @Override
    public void setTransparency(float transparency) throws RemoteException {

    }

    @Override
    public float getTransparency() throws RemoteException {
        return 0;
    }
}
