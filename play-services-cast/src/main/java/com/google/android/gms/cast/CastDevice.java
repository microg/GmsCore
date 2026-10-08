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
import android.net.Uri;
import android.text.TextUtils;

import com.google.android.gms.common.images.WebImage;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@PublicApi
public class CastDevice extends AutoSafeParcelable {
    private static final String EXTRA_CAST_DEVICE = "com.google.android.gms.cast.EXTRA_CAST_DEVICE";

    public CastDevice () {
    }

    public CastDevice (
            String id, String name, InetAddress host, int port, String
            deviceVersion, String friendlyName, String modelName, String
            iconPath, int status, int capabilities) {
        this.deviceId = id;
        this.inetAddress = host;
        this.address = host.getHostAddress();
        this.servicePort = port;
        this.deviceVersion = deviceVersion;
        this.friendlyName = friendlyName;
        this.icons = new ArrayList<WebImage>();
        if (iconPath != null) {
            String iconHost = host instanceof Inet6Address
                    ? "[" + Uri.encode(this.address, ":") + "]" : this.address;
            Uri iconOrigin = new Uri.Builder()
                    .scheme("http")
                    .encodedAuthority(iconHost + ":8008")
                    .build();
            this.icons.add(new WebImage(Uri.parse(iconOrigin.toString() + iconPath)));
        }
        this.modelName = modelName;
        this.capabilities = capabilities;
        this.status = status;
    }

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

    /**
     * Device capability flag that indicates the device represents a multi-zone group.
     */
    public static final int CAPABILITY_MULTIZONE_GROUP = 32;

    /** Device IDs of Cast Nearby / cloud receivers, which are not on the local network. */
    private static final String CAST_NEARBY_DEVICE_ID_PREFIX = "__cast_nearby__";

    @SafeParceled(1)
    private int versionCode = 3;

    @SafeParceled(2)
    private String deviceId;

    @SafeParceled(3)
    private String address;

    // Rebuilt from {@link #address} after parceling. Hardware LAN hosts and loopback/software
    // receivers are distinguished through {@link #getInetAddress()}.
    private transient InetAddress inetAddress;

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

    @SafeParceled(9)
    private int capabilities;

    @SafeParceled(10)
    private int status;

    @SafeParceled(11)
    private String unknown; // TODO: Need to figure this one out

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
        if (icons == null || icons.isEmpty()) return null;

        WebImage fallback = null;
        if (preferredWidth <= 0 || preferredHeight <= 0) {
            for (WebImage icon : icons) {
                if (icon != null) return icon;
            }
            return null;
        }

        WebImage bestFit = null;
        long bestFitDistance = Long.MAX_VALUE;
        WebImage closest = null;
        long closestDistance = Long.MAX_VALUE;
        for (WebImage icon : icons) {
            if (icon == null) continue;
            if (fallback == null) fallback = icon;
            int width = icon.getWidth();
            int height = icon.getHeight();
            if (width <= 0 || height <= 0) continue;
            long distance = Math.abs((long) width - preferredWidth)
                    + Math.abs((long) height - preferredHeight);
            if (distance < closestDistance) {
                closest = icon;
                closestDistance = distance;
            }
            if (width >= preferredWidth && height >= preferredHeight && distance < bestFitDistance) {
                bestFit = icon;
                bestFitDistance = distance;
            }
        }
        return bestFit != null ? bestFit : (closest != null ? closest : fallback);
    }

    public List<WebImage> getIcons() {
        return icons;
    }

    public String getAddress() {
        return address;
    }

    /**
     * Gets the {@link InetAddress} of the device. Loopback or unspecified addresses belong to
     * software/test receivers; a unicast LAN address belongs to a hardware receiver.
     */
    public InetAddress getInetAddress() {
        if (inetAddress != null) return inetAddress;
        inetAddress = parseAddress(address);
        return inetAddress;
    }

    /**
     * @deprecated Use {@link #getInetAddress()} instead.
     */
    @Deprecated
    public Inet4Address getIpAddress() {
        InetAddress resolved = getInetAddress();
        return resolved instanceof Inet4Address ? (Inet4Address) resolved : null;
    }

    public String getModelName() {
        return modelName;
    }

    public int getServicePort() {
        return servicePort;
    }

    public boolean hasCapabilities(int[] capabilities) {
        for (int capability : capabilities) {
            if (!this.hasCapability(capability)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasCapability(int capability) {
        return (capability & capabilities) == capability;
    }

    public boolean hasIcons() {
        return !icons.isEmpty();
    }

    /**
     * Returns {@code true} for a hardware receiver discovered on the local network.
     * Cast Nearby / cloud IDs, loopback, and unspecified addresses return {@code false} so a
     * real Chromecast is not confused with a software receiver bound to localhost.
     */
    public boolean isOnLocalNetwork() {
        if (deviceId != null && deviceId.startsWith(CAST_NEARBY_DEVICE_ID_PREFIX)) {
            return false;
        }
        InetAddress resolved = getInetAddress();
        return resolved != null && !resolved.isLoopbackAddress() && !resolved.isAnyLocalAddress();
    }

    private static InetAddress parseAddress(String host) {
        if (host == null || host.isEmpty()) return null;
        String toParse = host;
        if (toParse.startsWith("[") && toParse.endsWith("]") && toParse.length() > 2) {
            toParse = toParse.substring(1, toParse.length() - 1);
        }
        try {
            return InetAddress.getByName(toParse);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public boolean isSameDevice(CastDevice castDevice) {
        return TextUtils.equals(castDevice.deviceId, deviceId);
    }

    public void putInBundle(Bundle bundle) {
        bundle.putParcelable(EXTRA_CAST_DEVICE, this);
    }

    @Override
    public String toString() {
        return "CastDevice{" +
                "deviceId=" + this.deviceId +
                ", address=" + address +
                ", friendlyName=" + friendlyName +
                ", modelName=" + modelName +
                ", deviceVersion=" + deviceVersion +
                ", servicePort=" + servicePort +
                (icons == null ? "" : (", icons=" + icons.toString())) +
                ", capabilities=" + capabilities +
                ", status=" + status +
                "}";
    }

    public static Creator<CastDevice> CREATOR = new AutoCreator<CastDevice>(CastDevice.class);
}
