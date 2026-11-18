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

package org.microg.gms.cast;

import android.util.Log;

import androidx.mediarouter.media.MediaRouteProvider;

/**
 * Route controller handed to the MediaRouter of client apps.
 * <p>
 * Sessions are not handled here: the Cast framework in the client connects to the device through
 * {@link CastDeviceControllerImpl}. The only thing the MediaRouter needs from us is volume control, which is what the
 * volume keys end up using while a Cast route is selected. Volume requests are forwarded to the device connection of
 * the running session instead of opening a second one.
 */
public class CastMediaRouteController extends MediaRouteProvider.RouteController {
    private static final String TAG = "GmsCastRouteController";

    private final CastDeviceRegistry registry = CastDeviceRegistry.getInstance();
    private final String routeId;

    public CastMediaRouteController(String routeId) {
        this.routeId = routeId;
    }

    @Override
    public void onSetVolume(int volume) {
        requestVolume((double) volume / CastMediaRouteProvider.ROUTE_VOLUME_MAX);
    }

    @Override
    public void onUpdateVolume(int delta) {
        double current = this.registry.getVolume(this.routeId);
        if (current < 0) {
            Log.d(TAG, "Volume of " + this.routeId + " is not known, ignoring volume change");
            return;
        }
        requestVolume(current + (double) delta / CastMediaRouteProvider.ROUTE_VOLUME_MAX);
    }

    private void requestVolume(double volume) {
        CastDeviceControllerImpl controller = this.registry.getController(this.routeId);
        if (controller == null) {
            Log.d(TAG, "No session on " + this.routeId + ", ignoring volume change");
            return;
        }
        controller.setVolumeFromRoute(volume);
    }
}
