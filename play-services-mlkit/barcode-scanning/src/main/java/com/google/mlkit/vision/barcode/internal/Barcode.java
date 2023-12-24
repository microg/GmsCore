/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.barcode.internal;

import android.graphics.Point;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class Barcode extends AbstractSafeParcelable {
    @Field(1)
    public int format;
    @Field(2)
    public String displayValue;
    @Field(3)
    public String rawValue;
    @Field(4)
    public byte[] rawBytes;
    @Field(5)
    public Point[] cornerPoints;
    @Field(6)
    public int valueType;
    @Field(7)
    public Email email;
    @Field(8)
    public Phone phone;
    @Field(9)
    public Sms sms;
    @Field(10)
    public WiFi wifi;
    @Field(11)
    public UrlBookmark urlBookmark;
    @Field(12)
    public GeoPoint geoPoint;
    @Field(13)
    public CalendarEvent calendarEvent;
    @Field(14)
    public ContactInfo contactInfo;
    @Field(15)
    public DriverLicense driverLicense;

    // TODO: Copied from com.google.mlkit.vision.barcode.common.Barcode
    public static final int UNKNOWN_FORMAT = -1;
    public static final int ALL_FORMATS = 0;
    public static final int CODE_128 = 1;
    public static final int CODE_39 = 2;
    public static final int CODE_93 = 4;
    public static final int CODABAR = 8;
    public static final int DATA_MATRIX = 16;
    public static final int EAN_13 = 32;
    public static final int EAN_8 = 64;
    public static final int ITF = 128;
    public static final int QR_CODE = 256;
    public static final int UPC_A = 512;
    public static final int UPC_E = 1024;
    public static final int PDF417 = 2048;
    public static final int AZTEC = 4096;

    public static final int UNKNOWN_TYPE = 0;
    public static final int CONTACT_INFO = 1;
    public static final int EMAIL = 2;
    public static final int ISBN = 3;
    public static final int PHONE = 4;
    public static final int PRODUCT = 5;
    public static final int SMS = 6;
    public static final int TEXT = 7;
    public static final int URL = 8;
    public static final int WIFI = 9;
    public static final int GEO = 10;
    public static final int CALENDAR_EVENT = 11;
    public static final int DRIVER_LICENSE = 12;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Barcode> CREATOR = findCreator(Barcode.class);
}
