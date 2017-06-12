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

package com.google.android.gms.cast;

import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.common.images.WebImage;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

@PublicApi
public class CastDevice extends AutoSafeParcelable {
    private static final String EXTRA_CAST_DEVICE = "com.google.android.gms.cast.EXTRA_CAST_DEVICE";

    /**
     * Video-output device capability.
     */
    public static final int CAPABILITY_VIDEO_OUT = 1;

    /**
     * Video-input device capability.
     */
    public static final int CAPABILITY_VIDEO_IN = 2;

    /**
     * Audio-output device capability.
     */
    public static final int CAPABILITY_AUDIO_OUT = 4;

    /**
     * Audio-input device capability.
     */
    public static final int CAPABILITY_AUDIO_IN = 8;

    @SafeParceled(1)
    private int versionCode = 3;

    @SafeParceled(2)
    private String deviceId;

    @SafeParceled(3)
    private String addrString;
    private Inet4Address addr;

    @SafeParceled(4)
    private String friendlyName;

    @SafeParceled(5)
    private String modelName;

    @SafeParceled(6)
    private String deviceVersion;

    @SafeParceled(7)
    private int servicePort;

    @SafeParceled(value = 8, subClass = WebImage.class)
    private ArrayList<WebImage> icons;

    @SafeParceled(8)
    private int capabilities;

    @SafeParceled(9)
    private int status;

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceVersion() {
        return deviceVersion;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public static CastDevice getFromBundle(Bundle extras) {
        if (extras == null) {
            return null;
        }
        extras.setClassLoader(CastDevice.class.getClassLoader());
        return extras.getParcelable(EXTRA_CAST_DEVICE);
    }

    public WebImage getIcon(int preferredWidth, int preferredHeight) {
        return null;
    }

    public List<WebImage> getIcons() {
        return icons;
    }

    public Inet4Address getIpAddress() {
        return addr;
    }

    public String getModelName() {
        return modelName;
    }

    public int getServicePort() {
        return servicePort;
    }

    public boolean hasCapabilities(int[] capabilities) {
        return false;
    }

    public boolean hasCapability(int capability) {
        return (capability & capabilities) == capability;
    }

    public boolean hasIcons() {
        return !icons.isEmpty();
    }

    public boolean isOnLocalNetwork() {
        return false;
    }

    public boolean isSameDevice(CastDevice castDevice) {
        return TextUtils.equals(castDevice.deviceId, deviceId);
    }

    public void putInBundle(Bundle bundle) {
        bundle.putParcelable(EXTRA_CAST_DEVICE, this);
    }

    public static Creator<CastDevice> CREATOR = new AutoCreator<CastDevice>(CastDevice.class);
}
