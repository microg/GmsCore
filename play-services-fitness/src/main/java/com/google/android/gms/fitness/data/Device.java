/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fitness.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Parcel;

import android.provider.Settings;
import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

import static android.os.Build.VERSION.SDK_INT;

/**
 * Representation of an integrated device (such as a phone or a wearable) that can hold sensors. Each sensor is exposed as a {@link DataSource}.
 */
@SafeParcelable.Class
public class Device extends AbstractSafeParcelable {

    /**
     * Constant indicating the device type is not known.
     */
    public static final int TYPE_UNKNOWN = 0;
    /**
     * Constant indicating the device is an Android phone.
     */
    public static final int TYPE_PHONE = 1;
    /**
     * Constant indicating the device is an Android tablet.
     */
    public static final int TYPE_TABLET = 2;
    /**
     * Constant indicating the device is a watch or other wrist-mounted band.
     */
    public static final int TYPE_WATCH = 3;
    /**
     * Constant indicating the device is a chest strap.
     */
    public static final int TYPE_CHEST_STRAP = 4;
    /**
     * Constant indicating the device is a scale or similar device the user steps on.
     */
    public static final int TYPE_SCALE = 5;
    /**
     * Constant indicating the device is a headset, pair of glasses, or other head-mounted device
     */
    public static final int TYPE_HEAD_MOUNTED = 6;

    @Field(value = 1, getterName = "getManufacturer")
    @NonNull
    private final String manufacturer;
    @Field(value = 2, getterName = "getModel")
    @NonNull
    private final String model;
    @Field(value = 4, getterName = "getUid")
    @NonNull
    private final String uid;
    @Field(value = 5, getterName = "getType")
    private final int type;
    @Field(value = 6, getterName = "getPlatformType")
    private final int platformType;

    @Constructor
    Device(@NonNull @Param(1) String manufacturer, @NonNull @Param(2) String model, @NonNull @Param(4) String uid, @Param(5) int type, @Param(6) int platformType) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.uid = uid;
        this.type = type;
        this.platformType = platformType;
    }

    /**
     * Creates a new device.
     *
     * @param manufacturer The manufacturer of the product/hardware.
     * @param model        The end-user-visible name for the end product.
     * @param uid          A serial number or other unique identifier for the particular device hardware.
     * @param type         The type of device. One of the type constants.
     */
    public Device(@NonNull String manufacturer, @NonNull String model, @NonNull String uid, int type) {
        this(manufacturer, model, uid, type, 0);
    }

    /**
     * Returns the manufacturer of the product/hardware.
     */
    @NonNull
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * Returns the end-user-visible model name for the device.
     */
    @NonNull
    public String getModel() {
        return model;
    }

    /**
     * Returns the constant representing the type of the device. This will usually be one of the values from the type constants in this class, but it's
     * not required to be. Any other value should be treated as {@link #TYPE_UNKNOWN}.
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the serial number or other unique ID for the hardware.
     * <p>
     * Device UIDs are obfuscated based on the calling application's package name. Different applications will see different UIDs for the same
     * {@link Device}. If two {@link Device} instances have the same underlying UID, they'll also have the same obfuscated UID within each app (but not across
     * apps).
     */
    @NonNull
    public String getUid() {
        return uid;
    }

    String getDeviceId() {
        return manufacturer + ":" + model + ":" + uid;
    }

    int getPlatformType() {
        return platformType;
    }

    /**
     * Returns the Device representation of the local device, which can be used when defining local data sources.
     *
     * @noinspection deprecation
     */
    public static Device getLocalDevice(Context context) {
        @SuppressLint("HardwareIds") String uid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        int type = TYPE_PHONE;
        Configuration configuration = context.getResources().getConfiguration();
        PackageManager packageManager = context.getPackageManager();
        if (SDK_INT >= 20 && packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH))
            type = TYPE_WATCH;
        else if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) || packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))
            type = TYPE_UNKNOWN; // TV
        else if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
            type = TYPE_UNKNOWN; // Car
        else if ((configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) > Configuration.SCREENLAYOUT_SIZE_LARGE && configuration.smallestScreenWidthDp >= 600)
            type = TYPE_TABLET;
        else if (android.os.Build.PRODUCT.startsWith("glass_"))
            type = TYPE_HEAD_MOUNTED;
        return new Device(android.os.Build.MANUFACTURER, android.os.Build.MODEL, uid, type, 2);
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("Device").value(getDeviceId() + ":" + type + ":" + platformType).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Device> CREATOR = findCreator(Device.class);

}
